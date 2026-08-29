package com.marketplace.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final long MAX_BYTES = 5L * 1024 * 1024; // 5 MB

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${app.base-url}")
    private String baseUrl;

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

        return baseUrl + "/uploads/" + safeSubfolder + "/" + fileName;
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
