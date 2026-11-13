package com.marketplace.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private static final long MAX_BYTES = 5L * 1024 * 1024; // 5 MB

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${app.base-url}")
    private String baseUrl;

    // Supabase Storage (opcional). Vazio = grava no disco.
    @Value("${app.storage.supabase-url:}")
    private String supabaseUrl;

    @Value("${app.storage.supabase-secret-key:}")
    private String supabaseSecretKey;

    @Value("${app.storage.supabase-bucket:uploads}")
    private String supabaseBucket;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public String store(MultipartFile file, String subfolder) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Arquivo vazio.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new RuntimeException("Imagem muito grande (máximo 5 MB).");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("Não foi possível ler o arquivo enviado.");
        }

        // 1) Assinatura real do arquivo — ignora o tipo/nome que o cliente informou.
        String extension = detectImageExtension(bytes);
        if (extension == null) {
            throw new RuntimeException("Envie uma imagem JPG ou PNG.");
        }

        // 2) Confirma que decodifica como imagem de verdade (barra .html/.svg disfarçado).
        try {
            BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(bytes));
            if (decoded == null || decoded.getWidth() <= 0 || decoded.getHeight() <= 0) {
                throw new RuntimeException("Arquivo de imagem inválido.");
            }
        } catch (IOException e) {
            throw new RuntimeException("Arquivo de imagem inválido.");
        }

        String safeSubfolder = sanitizeSubfolder(subfolder);
        String fileName = UUID.randomUUID() + extension;

        // Com SUPABASE_URL + SUPABASE_SECRET_KEY configurados, a foto vai para o Supabase
        // Storage (não some em deploy/reinício). Sem eles, grava no disco, como antes.
        if (supabaseEnabled()) {
            return storeInSupabase(bytes, safeSubfolder, fileName, extension);
        }

        try {
            Path folderPath = Paths.get(uploadDir).resolve(safeSubfolder).normalize();
            Path base = Paths.get(uploadDir).toAbsolutePath().normalize();
            if (!folderPath.toAbsolutePath().normalize().startsWith(base)) {
                throw new RuntimeException("Caminho de destino inválido.");
            }
            Files.createDirectories(folderPath);
            Files.write(folderPath.resolve(fileName), bytes);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao salvar a imagem.");
        }

        return publicBaseUrl() + "/uploads/" + safeSubfolder + "/" + fileName;
    }

    // ------------------------------------------------------------------
    // Supabase Storage
    // ------------------------------------------------------------------

    private boolean supabaseEnabled() {
        return supabaseUrl != null && !supabaseUrl.isBlank()
                && supabaseSecretKey != null && !supabaseSecretKey.isBlank();
    }

    private String storeInSupabase(byte[] bytes, String safeSubfolder, String fileName, String extension) {
        String base = supabaseUrl.trim().replaceAll("/+$", "");
        String objectPath = safeSubfolder + "/" + fileName;
        String contentType = ".png".equals(extension) ? "image/png" : "image/jpeg";

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(base + "/storage/v1/object/" + supabaseBucket + "/" + objectPath))
                .timeout(Duration.ofSeconds(30))
                .header("apikey", supabaseSecretKey)
                .header("Content-Type", contentType)
                .header("x-upsert", "false")
                .POST(HttpRequest.BodyPublishers.ofByteArray(bytes));

        // Chave legada (service_role) é um JWT e também vai em Authorization. A chave nova
        // (sb_secret_...) NÃO é JWT e só pode ir no header apikey.
        if (supabaseSecretKey.startsWith("eyJ")) {
            request.header("Authorization", "Bearer " + supabaseSecretKey);
        }

        try {
            HttpResponse<String> response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                log.error("Supabase Storage recusou o upload de {}: HTTP {} {}",
                        objectPath, response.statusCode(), response.body());
                throw new RuntimeException("Não foi possível salvar a imagem agora. Tente novamente em instantes.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Não foi possível salvar a imagem agora. Tente novamente em instantes.");
        } catch (IOException e) {
            log.error("Falha de rede ao enviar {} para o Supabase Storage", objectPath, e);
            throw new RuntimeException("Não foi possível salvar a imagem agora. Tente novamente em instantes.");
        }

        return base + "/storage/v1/object/public/" + supabaseBucket + "/" + objectPath;
    }

    /**
     * Origem pública da API: a da requisição em andamento (atrás do proxy do Render vem
     * pelos headers X-Forwarded-*), e só se não houver requisição usa o app.base-url.
     * Antes valia sempre o APP_BASE_URL — se ele ficasse com o host errado, toda foto
     * nova era gravada com uma URL que não abre.
     */
    private String publicBaseUrl() {
        try {
            String fromRequest = org.springframework.web.servlet.support.ServletUriComponentsBuilder
                    .fromCurrentContextPath().build().toUriString();
            if (fromRequest != null && !fromRequest.isBlank()) {
                return fromRequest.replaceAll("/+$", "");
            }
        } catch (IllegalStateException ignored) {
            // fora de uma requisição HTTP
        }
        return baseUrl.replaceAll("/+$", "");
    }

    /** Extensão a partir dos primeiros bytes (magic number). null se não for JPG/PNG. */
    private static String detectImageExtension(byte[] b) {
        if (b.length < 8) return null;
        // JPEG: FF D8 FF
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return ".jpg";
        }
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if ((b[0] & 0xFF) == 0x89 && b[1] == 0x50 && b[2] == 0x4E && b[3] == 0x47
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return ".png";
        }
        return null;
    }

    /** Só letras, números, '/', '-', '_'. Sem segmentos vazios, '.' ou '..'. */
    private static String sanitizeSubfolder(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new RuntimeException("Pasta de destino inválida.");
        }
        String cleaned = raw.replaceAll("[^a-zA-Z0-9/_-]", "");
        for (String segment : cleaned.split("/")) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                throw new RuntimeException("Pasta de destino inválida.");
            }
        }
        return cleaned;
    }
}
