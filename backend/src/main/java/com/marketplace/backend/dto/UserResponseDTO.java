package com.marketplace.backend.dto;

import java.util.UUID;

public class UserResponseDTO {
    private UUID id;
    private String name;
    private String email;
    private String phone;
    private String role;
    private String address;
    private String photoUrl;

    public UserResponseDTO(UUID id, String name, String email, String phone, String role, String address, String photoUrl) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.address = address;
        this.photoUrl = photoUrl;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getRole() { return role; }
    public String getAddress() { return address; }
    public String getPhotoUrl() { return photoUrl; }
}