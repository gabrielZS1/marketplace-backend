package com.marketplace.backend.controller;

import com.marketplace.backend.dto.EmployeeRequestDTO;
import com.marketplace.backend.dto.EmployeeResponseDTO;
import com.marketplace.backend.entity.Business;
import com.marketplace.backend.entity.Employee;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.enums.UserRole;
import com.marketplace.backend.repository.BusinessRepository;
import com.marketplace.backend.repository.EmployeeRepository;
import com.marketplace.backend.repository.ReviewRepository;
import com.marketplace.backend.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/businesses/{businessId}/employees")
public class EmployeeController {

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
    public ResponseEntity<EmployeeResponseDTO> create(@PathVariable UUID businessId, @Valid @RequestBody EmployeeRequestDTO request) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));

        checkIsOwner(business);

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.EMPLOYEE);
        User savedUser = userRepository.save(user);

        Employee employee = new Employee();
        employee.setUser(savedUser);
        employee.setBusiness(business);
        employee.setBio(request.getBio());
        employee.setSpecialty(request.getSpecialty());

        Employee saved = employeeRepository.save(employee);
        return ResponseEntity.ok(toResponseDTO(saved));
    }

    private void checkIsOwner(Business business) {
        UUID loggedUserId = UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
        if (!business.getOwner().getId().equals(loggedUserId)) {
            throw new RuntimeException("Você não tem permissão para gerenciar esta empresa");
        }
    }

    private EmployeeResponseDTO toResponseDTO(Employee e) {
        Double rating = reviewRepository.findAverageRatingByEmployeeId(e.getId());
        return new EmployeeResponseDTO(e.getId(), e.getUser().getName(), e.getBio(), e.getSpecialty(), e.getActive(), rating);
    }


}