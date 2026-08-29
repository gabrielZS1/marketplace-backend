package com.marketplace.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class GoogleLoginRequestDTO {

    /** ID token devolvido pelo SDK do Google no app. */
    @NotBlank
    private String idToken;

    /**
     * Papel desejado quando a conta ainda não existe: "BUSINESS_OWNER" no app do dono,
     * qualquer outra coisa (ou nulo) cria como CLIENT. Ignorado se a conta já existe.
     */
    private String role;

    public String getIdToken() { return idToken; }
    public void setIdToken(String idToken) { this.idToken = idToken; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
