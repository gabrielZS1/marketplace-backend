package com.marketplace.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class VerifyEmailRequestDTO {

    @NotBlank
    private String code;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}
