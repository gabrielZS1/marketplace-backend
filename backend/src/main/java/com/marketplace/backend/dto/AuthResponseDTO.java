package com.marketplace.backend.dto;

public class AuthResponseDTO {
    private String token;
    private String refreshToken;
    private String name;
    private String email;
    private String role;
    private String address;
    private boolean emailVerified;

    public AuthResponseDTO(String token, String refreshToken, String name, String email, String role,
                           String address, boolean emailVerified) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.name = name;
        this.email = email;
        this.role = role;
        this.address = address;
        this.emailVerified = emailVerified;
    }

    public String getToken() { return token; }
    public String getRefreshToken() { return refreshToken; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getAddress() { return address; }
    public boolean isEmailVerified() { return emailVerified; }
}
