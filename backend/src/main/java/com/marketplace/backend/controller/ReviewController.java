package com.marketplace.backend.controller;

import com.marketplace.backend.dto.ReviewRequestDTO;
import com.marketplace.backend.dto.ReviewResponseDTO;
import com.marketplace.backend.entity.Appointment;
import com.marketplace.backend.entity.Review;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.enums.AppointmentStatus;
import com.marketplace.backend.repository.AppointmentRepository;
import com.marketplace.backend.repository.ReviewRepository;
import com.marketplace.backend.repository.UserRepository;
import jakarta.validation.Valid;
import com.marketplace.backend.repository.UserBlockRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.marketplace.backend.service.FileStorageService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@RestController
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final UserBlockRepository userBlockRepository;


    public ReviewController(
            ReviewRepository reviewRepository,
            AppointmentRepository appointmentRepository,
            UserRepository userRepository,
            FileStorageService fileStorageService,
            UserBlockRepository userBlockRepository
    ) {
        this.userBlockRepository = userBlockRepository;
        this.reviewRepository = reviewRepository;
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping(value = "/api/appointments/{appointmentId}/review", consumes = "multipart/form-data")
    public ResponseEntity<ReviewResponseDTO> create(
            @PathVariable UUID appointmentId,
            @RequestParam Integer rating,
            @RequestParam(required = false) String comment,
            @RequestParam(required = false) MultipartFile photo
    ) {
        UUID clientId = UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Agendamento não encontrado"));

        if (!appointment.getClient().getId().equals(clientId)) {
            throw new RuntimeException("Você não tem permissão para avaliar este agendamento");
        }



        // Só dá pra avaliar um atendimento que já aconteceu. Considera concluído também o que
        // foi confirmado e já terminou mas o job automático ainda não fechou.
        AppointmentStatus st = appointment.getStatus();
        boolean finished = st == AppointmentStatus.COMPLETED
                || ((st == AppointmentStatus.CONFIRMED || st == AppointmentStatus.IN_PROGRESS)
                && appointment.getEndsAt().isBefore(OffsetDateTime.now()));
        if (!finished) {
            throw new RuntimeException("Você só pode avaliar depois que o atendimento for concluído.");
        }

        if (rating == null || rating < 1 || rating > 5) {
            throw new RuntimeException("A nota deve ser de 1 a 5.");
        }

        if (comment != null && comment.length() > 1000) {
            throw new RuntimeException("O comentário pode ter no máximo 1000 caracteres.");
        }

        if (reviewRepository.findByAppointmentId(appointmentId).isPresent()) {
            throw new RuntimeException("Este agendamento já foi avaliado");
        }

        User client = userRepository.findById(clientId).orElseThrow();

        Review review = new Review();
        review.setAppointment(appointment);
        review.setBusiness(appointment.getBusiness());
        review.setEmployee(appointment.getEmployee());
        review.setClient(client);
        review.setRating(rating);
        review.setComment(comment);

        if (photo != null && !photo.isEmpty()) {
            String photoUrl = fileStorageService.store(photo, "reviews/" + appointmentId);
            review.setPhotoUrl(photoUrl);
        }

        Review saved = reviewRepository.save(review);
        return ResponseEntity.ok(toResponseDTO(saved));
    }

    @GetMapping("/api/businesses/{businessId}/reviews")
    public List<ReviewResponseDTO> listByBusiness(@PathVariable UUID businessId) {
        // Rota pública: se quem pede está logado, esconde as avaliações de quem ele bloqueou.
        Set<UUID> blocked = currentUserId()
                .map(id -> Set.copyOf(userBlockRepository.findBlockedIdsByBlockerId(id)))
                .orElse(Set.of());

        return reviewRepository.findByBusinessIdOrderByCreatedAtDesc(businessId).stream()
                .filter(r -> !blocked.contains(r.getClient().getId()))
                .map(this::toResponseDTO)
                .toList();
    }

    private Optional<UUID> currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(auth.getName()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private ReviewResponseDTO toResponseDTO(Review r) {
        return new ReviewResponseDTO(
                r.getId(),
                r.getClient().getName(),
                r.getRating(),
                r.getComment(),
                r.getCreatedAt(),
                r.getEmployee() != null && r.getEmployee().getUser() != null
                        ? r.getEmployee().getUser().getName() : null,
                r.getAppointment().getService() != null
                        ? r.getAppointment().getService().getName() : null,
                r.getPhotoUrl(),
                r.getClient().getId()
        );
    }
}