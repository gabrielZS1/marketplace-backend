package com.marketplace.backend.service;

import com.marketplace.backend.entity.OneTimeCode;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.enums.OneTimeCodePurpose;
import com.marketplace.backend.repository.OneTimeCodeRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;

/**
 * Emite e valida códigos de 6 dígitos enviados por e-mail.
 * Só o hash do código fica no banco; expira em 15 min e aceita no máximo 5 tentativas.
 */
@Service
public class OneTimeCodeService {

    private static final int CODE_TTL_MINUTES = 15;
    private static final int MAX_ATTEMPTS = 5;

    private final OneTimeCodeRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public OneTimeCodeService(OneTimeCodeRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Invalida códigos anteriores do mesmo tipo e cria um novo.
     * @return o código em texto puro, para ser enviado por e-mail (nunca persistido).
     */
    @Transactional
    public String issue(User user, OneTimeCodePurpose purpose) {
        repository.invalidateAll(user, purpose, OffsetDateTime.now());

        String code = String.format("%06d", random.nextInt(1_000_000));

        OneTimeCode entity = new OneTimeCode();
        entity.setUser(user);
        entity.setPurpose(purpose);
        entity.setCodeHash(passwordEncoder.encode(code));
        entity.setExpiresAt(OffsetDateTime.now().plusMinutes(CODE_TTL_MINUTES));
        repository.save(entity);

        return code;
    }

    /**
     * Consome o código: valida e marca como usado. Lança RuntimeException (pt-BR) se inválido.
     *
     * Sem @Transactional de propósito: cada save() confirma na hora, então o incremento
     * de tentativas / a marcação de usado sobrevivem ao throw que sinaliza a falha.
     */
    public void consume(User user, OneTimeCodePurpose purpose, String code) {
        OneTimeCode entity = repository
                .findTopByUserAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(user, purpose)
                .orElseThrow(() -> new RuntimeException("Código inválido ou expirado."));

        if (entity.getExpiresAt().isBefore(OffsetDateTime.now())) {
            entity.setUsedAt(OffsetDateTime.now());
            repository.save(entity);
            throw new RuntimeException("Código inválido ou expirado.");
        }

        if (entity.getAttempts() >= MAX_ATTEMPTS) {
            entity.setUsedAt(OffsetDateTime.now());
            repository.save(entity);
            throw new RuntimeException("Muitas tentativas. Peça um novo código.");
        }

        if (code == null || !passwordEncoder.matches(code, entity.getCodeHash())) {
            entity.setAttempts((short) (entity.getAttempts() + 1));
            repository.save(entity);
            throw new RuntimeException("Código inválido ou expirado.");
        }

        entity.setUsedAt(OffsetDateTime.now());
        repository.save(entity);
    }
}
