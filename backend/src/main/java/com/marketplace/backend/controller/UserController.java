package com.marketplace.backend.controller;

import com.marketplace.backend.entity.User;
import com.marketplace.backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PatchMapping("/me/address")
    public ResponseEntity<Void> updateAddress(@RequestBody Map<String, String> body) {
        UUID userId = UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        user.setAddress(body.get("address"));
        userRepository.save(user);

        return ResponseEntity.noContent().build();
    }
}