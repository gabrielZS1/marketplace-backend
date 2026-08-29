package com.marketplace.backend.controller;

import com.marketplace.backend.dto.AuthResponseDTO;
import com.marketplace.backend.dto.ChangePasswordRequestDTO;
import com.marketplace.backend.dto.DeleteAccountRequestDTO;
import com.marketplace.backend.dto.ForgotPasswordRequestDTO;
import com.marketplace.backend.dto.GoogleLoginRequestDTO;
import com.marketplace.backend.dto.LoginRequestDTO;
import com.marketplace.backend.dto.RefreshRequestDTO;
import com.marketplace.backend.dto.RegisterRequestDTO;
import com.marketplace.backend.dto.ResetPasswordRequestDTO;
import com.marketplace.backend.dto.UserResponseDTO;
import com.marketplace.backend.dto.VerifyEmailRequestDTO;
import com.marketplace.backend.entity.RefreshToken;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.enums.OneTimeCodePurpose;
import com.marketplace.backend.enums.UserRole;
import com.marketplace.backend.repository.UserRepository;
import com.marketplace.backend.security.JwtService;
import com.marketplace.backend.service.AccountDeletionService;
import com.marketplace.backend.service.EmailService;
import com.marketplace.backend.service.GoogleAuthService;
import com.marketplace.backend.service.OneTimeCodeService;
import com.marketplace.backend.service.RefreshTokenService;
import com.marketplace.backend.service.TurnstileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final OneTimeCodeService oneTimeCodeService;
    private final EmailService emailService;
    private final GoogleAuthService googleAuthService;
    private final TurnstileService turnstileService;
    private final AccountDeletionService accountDeletionService;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            OneTimeCodeService oneTimeCodeService,
            EmailService emailService,
            GoogleAuthService googleAuthService,
            TurnstileService turnstileService,
            AccountDeletionService accountDeletionService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.oneTimeCodeService = oneTimeCodeService;
        this.emailService = emailService;
        this.googleAuthService = googleAuthService;
        this.turnstileService = turnstileService;
        this.accountDeletionService = accountDeletionService;
    }

    // ============================================================
    // CADASTRO
    // ============================================================

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(
            @Valid @RequestBody RegisterRequestDTO request, HttpServletRequest http) {
        return ResponseEntity.ok(createUser(request, UserRole.CLIENT, http));
    }

    @PostMapping("/register-business-owner")
    public ResponseEntity<AuthResponseDTO> registerBusinessOwner(
            @Valid @RequestBody RegisterRequestDTO request, HttpServletRequest http) {
        return ResponseEntity.ok(createUser(request, UserRole.BUSINESS_OWNER, http));
    }

    private AuthResponseDTO createUser(RegisterRequestDTO request, UserRole role, HttpServletRequest http) {
        turnstileService.verify(request.getCaptchaToken(), clientIp(http));

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail().toLowerCase());
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setEmailVerified(false);

        User saved = userRepository.save(user);
        sendVerificationCode(saved);
        return buildAuthResponse(saved);
    }

    // ============================================================
    // LOGIN E-MAIL/SENHA
    // ============================================================

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new RuntimeException("E-mail ou senha inválidos"));

        if (user.getLockoutUntil() != null && user.getLockoutUntil().isAfter(OffsetDateTime.now())) {
            throw new RuntimeException(
                    "Muitas tentativas. Tente novamente em alguns minutos.");
        }

        // Google é só uma opção de entrada — não revelamos que a conta não tem senha.
        // Conta sem senha nunca casa: mensagem genérica de senha inválida.
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            registerFailedLogin(user);
            throw new RuntimeException("E-mail ou senha inválidos");
        }

        if (user.getFailedLoginAttempts() > 0 || user.getLockoutUntil() != null) {
            user.setFailedLoginAttempts((short) 0);
            user.setLockoutUntil(null);
            userRepository.save(user);
        }

        return ResponseEntity.ok(buildAuthResponse(user));
    }

    private void registerFailedLogin(User user) {
        short attempts = (short) (user.getFailedLoginAttempts() + 1);
        if (attempts >= MAX_LOGIN_ATTEMPTS) {
            user.setFailedLoginAttempts((short) 0);
            user.setLockoutUntil(OffsetDateTime.now().plusMinutes(LOCKOUT_MINUTES));
        } else {
            user.setFailedLoginAttempts(attempts);
        }
        userRepository.save(user);
    }

    // ============================================================
    // LOGIN COM GOOGLE
    // ============================================================

    @PostMapping("/google")
    public ResponseEntity<AuthResponseDTO> google(@Valid @RequestBody GoogleLoginRequestDTO request) {
        GoogleAuthService.GoogleUser g = googleAuthService.verify(request.getIdToken());

        User user = userRepository.findByGoogleId(g.googleId())
                .or(() -> userRepository.findByEmail(g.email()))
                .orElse(null);

        if (user == null) {
            user = new User();
            user.setName(g.name() != null ? g.name() : g.email());
            user.setEmail(g.email());
            user.setGoogleId(g.googleId());
            user.setPhotoUrl(g.picture());
            user.setEmailVerified(g.emailVerified());
            user.setRole("BUSINESS_OWNER".equalsIgnoreCase(request.getRole())
                    ? UserRole.BUSINESS_OWNER : UserRole.CLIENT);
        } else {
            // Conta já existe (por e-mail): vincula o Google na primeira vez.
            if (user.getGoogleId() == null) {
                user.setGoogleId(g.googleId());
            }
            if (Boolean.FALSE.equals(user.getEmailVerified()) && g.emailVerified()) {
                user.setEmailVerified(true);
            }
            if (user.getPhotoUrl() == null && g.picture() != null) {
                user.setPhotoUrl(g.picture());
            }
        }

        User saved = userRepository.save(user);
        return ResponseEntity.ok(buildAuthResponse(saved));
    }

    // ============================================================
    // REFRESH / LOGOUT
    // ============================================================

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refresh(@Valid @RequestBody RefreshRequestDTO request) {
        RefreshToken storedToken = refreshTokenService.validateAndGet(request.getRefreshToken());
        User user = storedToken.getUser();

        String newAccessToken = jwtService.generateToken(user.getId().toString(), user.getRole().name());
        refreshTokenService.revoke(request.getRefreshToken());
        String newRefreshToken = refreshTokenService.createRefreshToken(user);

        return ResponseEntity.ok(new AuthResponseDTO(
                newAccessToken, newRefreshToken, user.getName(), user.getEmail(),
                user.getRole().name(), user.getAddress(), Boolean.TRUE.equals(user.getEmailVerified())
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequestDTO request) {
        refreshTokenService.revoke(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    // ============================================================
    // PERFIL
    // ============================================================

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> me() {
        User user = currentUser();
        return ResponseEntity.ok(new UserResponseDTO(
                user.getId(), user.getName(), user.getEmail(), user.getPhone(),
                user.getRole().name(), user.getAddress(), user.getPhotoUrl(),
                Boolean.TRUE.equals(user.getEmailVerified())
        ));
    }

    @PostMapping("/delete-account")
    public ResponseEntity<Void> deleteAccount(@RequestBody(required = false) DeleteAccountRequestDTO request) {
        User user = currentUser();

        if (user.getPasswordHash() != null) {
            String password = request == null ? null : request.getPassword();
            if (password == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
                throw new RuntimeException("Senha incorreta");
            }
        }

        accountDeletionService.delete(user);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequestDTO request) {
        User user = currentUser();

        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Senha atual incorreta");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        return ResponseEntity.noContent().build();
    }

    // ============================================================
    // ESQUECI A SENHA (código por e-mail)
    // ============================================================

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO request) {
        // Resposta sempre 204, exista o e-mail ou não (não revela quem tem conta).
        userRepository.findByEmail(request.getEmail().toLowerCase()).ifPresent(user -> {
            String code = oneTimeCodeService.issue(user, OneTimeCodePurpose.PASSWORD_RESET);
            emailService.sendPasswordResetCode(user.getEmail(), code);
        });
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new RuntimeException("Código inválido ou expirado."));

        oneTimeCodeService.consume(user, OneTimeCodePurpose.PASSWORD_RESET, request.getCode());

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Troca de senha desloga todos os dispositivos.
        refreshTokenService.revokeAllForUser(user);
        return ResponseEntity.noContent().build();
    }

    // ============================================================
    // VERIFICAÇÃO DE E-MAIL
    // ============================================================

    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequestDTO request) {
        User user = currentUser();

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            return ResponseEntity.noContent().build();
        }

        oneTimeCodeService.consume(user, OneTimeCodePurpose.EMAIL_VERIFICATION, request.getCode());
        user.setEmailVerified(true);
        userRepository.save(user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification() {
        User user = currentUser();
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            sendVerificationCode(user);
        }
        return ResponseEntity.noContent().build();
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private void sendVerificationCode(User user) {
        String code = oneTimeCodeService.issue(user, OneTimeCodePurpose.EMAIL_VERIFICATION);
        emailService.sendEmailVerificationCode(user.getEmail(), code);
    }

    private AuthResponseDTO buildAuthResponse(User user) {
        String accessToken = jwtService.generateToken(user.getId().toString(), user.getRole().name());
        String refreshToken = refreshTokenService.createRefreshToken(user);
        return new AuthResponseDTO(
                accessToken, refreshToken, user.getName(), user.getEmail(),
                user.getRole().name(), user.getAddress(), Boolean.TRUE.equals(user.getEmailVerified())
        );
    }

    private User currentUser() {
        UUID userId = UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }

    private static String clientIp(HttpServletRequest http) {
        if (http == null) return null;
        String cf = http.getHeader("CF-Connecting-IP");
        if (cf != null && !cf.isBlank()) return cf;
        String xff = http.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return http.getRemoteAddr();
    }
}
