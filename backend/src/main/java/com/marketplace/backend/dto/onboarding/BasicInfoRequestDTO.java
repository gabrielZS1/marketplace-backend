package com.marketplace.backend.dto.onboarding;

import jakarta.validation.constraints.NotBlank;

public class BasicInfoRequestDTO {
    @NotBlank
    private String name;

    private String description;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}