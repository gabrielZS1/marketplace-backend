package com.marketplace.backend.controller;

import com.marketplace.backend.entity.User;
import com.marketplace.backend.entity.UserBlock;
import com.marketplace.backend.repository.UserBlockRepository;
import com.marketplace.backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Bloqueio de usuários: as avaliações de quem foi bloqueado deixam de aparecer para quem bloqueou. */
@RestController
@RequestMapping("/api/users/me/blocks")
public class BlockController {

    private final UserBlockRepository userBlockRepository;
    private final UserRepository userRepository;

    public BlockController(UserBlockRepository userBlockRepository, UserRepository userRepository) {
        this.userBlockRepository = userBlockRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<Map<String, Object>> list() {
        return userBlockRepository.findWithBlockedByBlockerId(me()).stream()
                .map(b -> Map.<String, Object>of(
                        "userId", b.getBlocked().getId(),
                        "name", b.getBlocked().getName()))
                .toList();
    }

    @PostMapping("/{userId}")
    @Transactional
    public ResponseEntity<Void> block(@PathVariable UUID userId) {
        UUID me = me();
        if (me.equals(userId)) {
            throw new RuntimeException("Você não pode bloquear a si mesmo.");
        }
        User blocked = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        if (!userBlockRepository.existsByBlockerIdAndBlockedId(me, userId)) {
            UserBlock block = new UserBlock();
            block.setBlocker(userRepository.getReferenceById(me));
            block.setBlocked(blocked);
            userBlockRepository.save(block);
        }
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}")
    @Transactional
    public ResponseEntity<Void> unblock(@PathVariable UUID userId) {
        userBlockRepository.deleteByBlockerIdAndBlockedId(me(), userId);
        return ResponseEntity.noContent().build();
    }

    private UUID me() {
        return UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
    }
}
