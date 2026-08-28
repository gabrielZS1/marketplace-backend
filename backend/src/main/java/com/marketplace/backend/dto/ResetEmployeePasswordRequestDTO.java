package com.marketplace.backend.dto;

import jakarta.validation.constraints.Size;

/**
 * Dono redefine a senha de um funcionário.
 * Se {@code password} vier vazio, o backend gera uma senha temporária.
 */
public class ResetEmployeePasswordRequestDTO {

    @Size(min = 8, message = "A senha precisa ter pelo menos 8 caracteres")
    private String password;

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
