package com.marketplace.backend.dto;

public class AuthResponseDTO {
    private String token;
    private String refreshToken;
    private String name;
    private String role;
    private String address;

    public AuthResponseDTO(String token, String refreshToken, String name, String role, String address) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.name = name;
        this.role = role;
        this.address = address;
    }

    public String getToken() { return token; }
    public String getRefreshToken() { return refreshToken; }
    public String getName() { return name; }
    public String getRole() { return role; }
    public String getAddress() { return address; }
}