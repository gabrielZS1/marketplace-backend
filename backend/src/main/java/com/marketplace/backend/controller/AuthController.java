package com.marketplace.backend.controller;

import com.marketplace.backend.dto.AuthResponseDTO;
import com.marketplace.backend.dto.LoginRequestDTO;
import com.marketplace.backend.dto.RefreshRequestDTO;
import com.marketplace.backend.dto.RegisterRequestDTO;
import com.marketplace.backend.dto.UserResponseDTO;
import com.marketplace.backend.entity.RefreshToken;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.enums.UserRole;
import com.marketplace.backend.repository.UserRepository;
import com.marketplace.backend.security.JwtService;
import com.marketplace.backend.service.RefreshTokenService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
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
        String accessToken = jwtService.generateToken(saved.getId().toString(), saved.getRole().name());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(saved);

        return ResponseEntity.ok(new AuthResponseDTO(
                accessToken, refreshToken.getToken(), saved.getName(), saved.getRole().name(), saved.getAddress()
        ));
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
        String accessToken = jwtService.generateToken(saved.getId().toString(), saved.getRole().name());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(saved);

        return ResponseEntity.ok(new AuthResponseDTO(
                accessToken, refreshToken.getToken(), saved.getName(), saved.getRole().name(), saved.getAddress()
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> me() {
        java.util.UUID userId = java.util.UUID.fromString(
                SecurityContextHolder.getContext().getAuthentication().getName()
        );

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        return ResponseEntity.ok(new UserResponseDTO(
                user.getId(), user.getName(), user.getEmail(), user.getPhone(),
                user.getRole().name(), user.getAddress(), user.getPhotoUrl()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("E-mail ou senha inválidos"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("E-mail ou senha inválidos");
        }

        String accessToken = jwtService.generateToken(user.getId().toString(), user.getRole().name());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return ResponseEntity.ok(new AuthResponseDTO(
                accessToken, refreshToken.getToken(), user.getName(), user.getRole().name(), user.getAddress()
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refresh(@Valid @RequestBody RefreshRequestDTO request) {
        RefreshToken storedToken = refreshTokenService.validateAndGet(request.getRefreshToken());
        User user = storedToken.getUser();

        String newAccessToken = jwtService.generateToken(user.getId().toString(), user.getRole().name());

        // rotation: revoga o antigo e cria um novo refresh token
        refreshTokenService.revoke(storedToken.getToken());
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);

        return ResponseEntity.ok(new AuthResponseDTO(
                newAccessToken, newRefreshToken.getToken(), user.getName(), user.getRole().name(), user.getAddress()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequestDTO request) {
        refreshTokenService.revoke(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }
}