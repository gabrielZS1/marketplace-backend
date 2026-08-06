package com.marketplace.backend.dto;

import java.util.UUID;

public class EmployeeCreatedResponseDTO {
    private UUID id;
    private String name;
    private String email;
    private String temporaryPassword;

    public EmployeeCreatedResponseDTO(UUID id, String name, String email, String temporaryPassword) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.temporaryPassword = temporaryPassword;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getTemporaryPassword() { return temporaryPassword; }
}