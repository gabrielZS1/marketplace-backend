package com.marketplace.backend.controller;

import com.marketplace.backend.dto.BusinessMeResponseDTO;
import com.marketplace.backend.dto.PushTokenUpdateRequestDTO;
import com.marketplace.backend.entity.Business;
import com.marketplace.backend.entity.Employee;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.repository.BusinessRepository;
import com.marketplace.backend.repository.EmployeeRepository;
import com.marketplace.backend.repository.UserRepository;
import com.marketplace.backend.service.SubscriptionStatusService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/businesses/me")
public class BusinessAccountController {

    private final BusinessRepository businessRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final SubscriptionStatusService subscriptionStatusService;

    public BusinessAccountController(BusinessRepository businessRepository,
                                    EmployeeRepository employeeRepository,
                                    UserRepository userRepository,
                                    SubscriptionStatusService subscriptionStatusService) {
        this.businessRepository = businessRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.subscriptionStatusService = subscriptionStatusService;
    }

    @GetMapping
    public ResponseEntity<BusinessMeResponseDTO> me() {
        UUID userId = loggedUserId();

        Business owned = businessRepository
                .findFirstByOwnerIdOrderByOnboardingCompletedDescCreatedAtAsc(userId)
                .orElse(null);
        if (owned != null) {
            return ResponseEntity.ok(subscriptionStatusService.toMeResponse(owned, "OWNER", null));
        }

        Employee employee = employeeRepository.findByIdAndActiveTrue(userId).orElse(null);
        if (employee != null) {
            return ResponseEntity.ok(
                    subscriptionStatusService.toMeResponse(
                            employee.getBusiness(), "EMPLOYEE", employee.getId()
                    )
            );
        }

        return ResponseEntity.notFound().build();
    }

    @PatchMapping("/push-token")
    public ResponseEntity<Void> updatePushToken(@Valid @RequestBody PushTokenUpdateRequestDTO request) {
        UUID userId = loggedUserId();

        Business owned = businessRepository
                .findFirstByOwnerIdOrderByOnboardingCompletedDescCreatedAtAsc(userId)
                .orElse(null);
        if (owned != null) {
            owned.setExpoPushToken(request.getExpoPushToken());
            businessRepository.save(owned);
            return ResponseEntity.noContent().build();
        }

        Employee employee = employeeRepository.findByIdAndActiveTrue(userId).orElse(null);
        if (employee != null) {
            User user = employee.getUser();
            user.setExpoPushToken(request.getExpoPushToken());
            userRepository.save(user);
            return ResponseEntity.noContent().build();
        }

        throw new RuntimeException("Negócio não encontrado");
    }

    private UUID loggedUserId() {
        return UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
    }
}
