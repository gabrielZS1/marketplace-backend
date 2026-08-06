package com.marketplace.backend.controller;

import com.marketplace.backend.dto.EmployeeCreatedResponseDTO;
import com.marketplace.backend.dto.EmployeeRequestDTO;
import com.marketplace.backend.dto.EmployeeResponseDTO;
import com.marketplace.backend.dto.EmployeeUpdateRequestDTO;
import com.marketplace.backend.entity.Business;
import com.marketplace.backend.entity.Employee;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.enums.UserRole;
import com.marketplace.backend.exception.PlanLimitExceededException;
import com.marketplace.backend.repository.BusinessRepository;
import com.marketplace.backend.repository.EmployeeRepository;
import com.marketplace.backend.repository.ReviewRepository;
import com.marketplace.backend.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/businesses/{businessId}/employees")
public class EmployeeController {

    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private final SecureRandom random = new SecureRandom();

    private final EmployeeRepository employeeRepository;
    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ReviewRepository reviewRepository;

    public EmployeeController(EmployeeRepository employeeRepository, BusinessRepository businessRepository,
                              UserRepository userRepository, PasswordEncoder passwordEncoder, ReviewRepository reviewRepository) {
        this.employeeRepository = employeeRepository;
        this.businessRepository = businessRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.reviewRepository = reviewRepository;
    }

    @GetMapping
    public List<EmployeeResponseDTO> listByBusiness(@PathVariable UUID businessId) {
        return employeeRepository.findAll().stream()
                .filter(e -> e.getBusiness().getId().equals(businessId) && e.getActive())
                .map(this::toResponseDTO)
                .toList();
    }

    @PostMapping
    public ResponseEntity<EmployeeCreatedResponseDTO> create(@PathVariable UUID businessId, @Valid @RequestBody EmployeeRequestDTO request) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));

        checkIsOwner(business);
        checkPlanLimit(business); // NOVO

        String temporaryPassword = generateTemporaryPassword();

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setRole(UserRole.EMPLOYEE);
        User savedUser = userRepository.save(user);

        Employee employee = new Employee();
        employee.setUser(savedUser);
        employee.setBusiness(business);
        employee.setBio(request.getBio());
        employee.setSpecialty(request.getSpecialty());

        Employee saved = employeeRepository.save(employee);

        return ResponseEntity.ok(new EmployeeCreatedResponseDTO(
                saved.getId(), savedUser.getName(), savedUser.getEmail(), temporaryPassword
        ));
    }

    @PatchMapping("/{employeeId}")
    public ResponseEntity<EmployeeResponseDTO> update(@PathVariable UUID businessId, @PathVariable UUID employeeId,
                                                      @RequestBody EmployeeUpdateRequestDTO request) {
        Employee employee = findOwnedEmployee(businessId, employeeId);

        if (request.getName() != null) employee.getUser().setName(request.getName());
        if (request.getPhone() != null) employee.getUser().setPhone(request.getPhone());
        if (request.getBio() != null) employee.setBio(request.getBio());
        if (request.getSpecialty() != null) employee.setSpecialty(request.getSpecialty());

        userRepository.save(employee.getUser());
        Employee saved = employeeRepository.save(employee);

        return ResponseEntity.ok(toResponseDTO(saved));
    }

    @DeleteMapping("/{employeeId}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID businessId, @PathVariable UUID employeeId) {
        Employee employee = findOwnedEmployee(businessId, employeeId);
        employee.setActive(false);
        employeeRepository.save(employee);
        return ResponseEntity.noContent().build();
    }

    private Employee findOwnedEmployee(UUID businessId, UUID employeeId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));
        checkIsOwner(business);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Funcionário não encontrado"));

        if (!employee.getBusiness().getId().equals(businessId)) {
            throw new RuntimeException("Funcionário não pertence a esta empresa");
        }
        return employee;
    }

    private void checkPlanLimit(Business business) {
        long activeCount = employeeRepository.findAll().stream()
                .filter(e -> e.getBusiness().getId().equals(business.getId()) && e.getActive())
                .count();

        if (activeCount >= business.getTeamSize()) {
            throw new PlanLimitExceededException(
                    "Seu plano atual permite até " + business.getTeamSize() +
                            " profissional(is). Faça upgrade do plano para cadastrar mais funcionários."
            );
        }
    }

    private void checkIsOwner(Business business) {
        UUID loggedUserId = UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
        if (!business.getOwner().getId().equals(loggedUserId)) {
            throw new RuntimeException("Você não tem permissão para gerenciar esta empresa");
        }
    }

    private String generateTemporaryPassword() {
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(PASSWORD_CHARS.charAt(random.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }

    private EmployeeResponseDTO toResponseDTO(Employee e) {
        Double rating = reviewRepository.findAverageRatingByEmployeeId(e.getId());
        return new EmployeeResponseDTO(e.getId(), e.getUser().getName(), e.getBio(), e.getSpecialty(), e.getActive(), rating);
    }
}