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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;

    public ReviewController(ReviewRepository reviewRepository, AppointmentRepository appointmentRepository, UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/api/appointments/{appointmentId}/review")
    public ResponseEntity<ReviewResponseDTO> create(@PathVariable UUID appointmentId, @Valid @RequestBody ReviewRequestDTO request) {
        UUID clientId = UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Agendamento não encontrado"));

        if (!appointment.getClient().getId().equals(clientId)) {
            throw new RuntimeException("Você não tem permissão para avaliar este agendamento");
        }

        if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw new RuntimeException("Só é possível avaliar agendamentos concluídos");
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
        review.setRating(request.getRating());
        review.setComment(request.getComment());

        Review saved = reviewRepository.save(review);
        return ResponseEntity.ok(toResponseDTO(saved));
    }

    @GetMapping("/api/businesses/{businessId}/reviews")
    public List<ReviewResponseDTO> listByBusiness(@PathVariable UUID businessId) {
        return reviewRepository.findByBusinessIdOrderByCreatedAtDesc(businessId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    private ReviewResponseDTO toResponseDTO(Review r) {
        return new ReviewResponseDTO(r.getId(), r.getClient().getName(), r.getRating(), r.getComment(), r.getCreatedAt());
    }
}