package com.marketplace.backend.dto;

public class DeleteAccountRequestDTO {

    /** Obrigatória para contas com senha; ignorada para contas só-Google. */
    private String password;

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
