package com.marketplace.backend.controller;

import com.marketplace.backend.dto.GeneratePromoCodesRequestDTO;
import com.marketplace.backend.dto.PromoCodeResponseDTO;
import com.marketplace.backend.entity.PromoCode;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.repository.PromoCodeRepository;
import com.marketplace.backend.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/promo-codes")
public class PromoCodeController {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom random = new SecureRandom();

    private final PromoCodeRepository promoCodeRepository;
    private final UserRepository userRepository;

    @Value("${app.admin-email}")
    private String adminEmail;

    public PromoCodeController(PromoCodeRepository promoCodeRepository, UserRepository userRepository) {
        this.promoCodeRepository = promoCodeRepository;
        this.userRepository = userRepository;
    }

    /**
     * Gera um lote de códigos promocionais.
     * Cada código concede {freeDays} dias de teste ao ser resgatado no onboarding.
     */
    @PostMapping("/generate")
    public ResponseEntity<List<PromoCodeResponseDTO>> generate(@Valid @RequestBody GeneratePromoCodesRequestDTO request) {
        checkIsAdmin();

        String label = (request.getLabel() != null && !request.getLabel().isBlank())
                ? request.getLabel().trim()
                : null;

        List<PromoCodeResponseDTO> generated = new ArrayList<>();

        for (int i = 0; i < request.getQuantity(); i++) {
            String code = generateUniqueCode();

            PromoCode promoCode = new PromoCode();
            promoCode.setCode(code);
            promoCode.setFreeDays(request.getFreeDays());
            promoCode.setLabel(label);
            promoCodeRepository.save(promoCode);

            generated.add(new PromoCodeResponseDTO(code, request.getFreeDays(), label));
        }

        return ResponseEntity.ok(generated);
    }

    /**
     * Exporta os códigos ainda não resgatados em CSV, pronto pra subir no Kiwify
     * como "chave de licença" / conteúdo entregável (1 código por comprador).
     * Filtra por lote via ?label=LANCAMENTO-15
     */
    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<String> export(@RequestParam(required = false) String label) {
        checkIsAdmin();

        List<PromoCode> codes = (label != null && !label.isBlank())
                ? promoCodeRepository.findByRedeemedFalseAndLabelOrderByCreatedAtAsc(label.trim())
                : promoCodeRepository.findByRedeemedFalseOrderByCreatedAtAsc();

        StringBuilder csv = new StringBuilder("code,free_days,label\n");
        for (PromoCode c : codes) {
            csv.append(c.getCode()).append(',')
               .append(c.getFreeDays()).append(',')
               .append(c.getLabel() == null ? "" : c.getLabel())
               .append('\n');
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"promo-codes.csv\"")
                .contentType(MediaType.valueOf("text/csv"))
                .body(csv.toString());
    }

    private String generateUniqueCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(8);
            for (int i = 0; i < 8; i++) {
                sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
            }
            code = sb.toString();
        } while (promoCodeRepository.findByCode(code).isPresent());
        return code;
    }

    private void checkIsAdmin() {
        UUID userId = UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (!adminEmail.equalsIgnoreCase(user.getEmail())) {
            throw new RuntimeException("Acesso negado");
        }
    }
}
