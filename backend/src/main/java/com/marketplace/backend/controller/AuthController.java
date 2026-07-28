package com.marketplace.backend.controller;

import com.marketplace.backend.dto.AuthResponseDTO;
import com.marketplace.backend.dto.LoginRequestDTO;
import com.marketplace.backend.dto.RegisterRequestDTO;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.enums.UserRole;
import com.marketplace.backend.repository.UserRepository;
import com.marketplace.backend.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.CLIENT);

        User saved = userRepository.save(user);
        String token = jwtService.generateToken(saved.getId().toString(), saved.getRole().name());


        return ResponseEntity.ok(new AuthResponseDTO(token, saved.getName(), saved.getRole().name(), saved.getAddress()));


    }

    @PostMapping("/register-business-owner")
    public ResponseEntity<AuthResponseDTO> registerBusinessOwner(@Valid @RequestBody RegisterRequestDTO request) {
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.BUSINESS_OWNER);

        User saved = userRepository.save(user);
        String token = jwtService.generateToken(saved.getId().toString(), saved.getRole().name());

        return ResponseEntity.ok(new AuthResponseDTO(token, saved.getName(), saved.getRole().name(), saved.getAddress()));

    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("E-mail ou senha inválidos"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("E-mail ou senha inválidos");
        }

        String token = jwtService.generateToken(user.getId().toString(), user.getRole().name());
        return ResponseEntity.ok(new AuthResponseDTO(token, user.getName(), user.getRole().name(), user.getAddress()));    }
}