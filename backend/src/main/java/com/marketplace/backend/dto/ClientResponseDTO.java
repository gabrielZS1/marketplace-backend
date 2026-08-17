package com.marketplace.backend.dto;

import java.util.UUID;

public class ClientResponseDTO {
    private UUID id;
    private String name;
    private String phone;
    private String photoUrl;

    public ClientResponseDTO(UUID id, String name, String phone, String photoUrl) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.photoUrl = photoUrl;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getPhotoUrl() { return photoUrl; }
}