package com.marketplace.backend.dto;

public class PromoCodeResponseDTO {
    private String code;
    private Integer freeMonths;

    public PromoCodeResponseDTO(String code, Integer freeMonths) {
        this.code = code;
        this.freeMonths = freeMonths;
    }

    public String getCode() { return code; }
    public Integer getFreeMonths() { return freeMonths; }
}