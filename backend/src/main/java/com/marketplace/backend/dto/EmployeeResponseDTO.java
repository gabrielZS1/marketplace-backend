package com.marketplace.backend.dto;

import java.util.UUID;

public class EmployeeResponseDTO {
    private UUID id;
    private String name;
    private String bio;
    private String specialty;
    private Boolean active;
    private Double rating;

    public EmployeeResponseDTO(UUID id, String name, String bio, String specialty, Boolean active, Double rating) {
        this.id = id; this.name = name; this.bio = bio; this.specialty = specialty;
        this.active = active; this.rating = this.rating;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getBio() { return bio; }
    public String getSpecialty() { return specialty; }
    public Boolean getActive() { return active; }
    public Double getRating() { return rating; }
}