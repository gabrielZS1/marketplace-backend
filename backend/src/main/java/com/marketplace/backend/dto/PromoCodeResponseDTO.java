package com.marketplace.backend.dto;

public class PromoCodeResponseDTO {
    private String code;
    private Integer freeDays;
    private String label;

    public PromoCodeResponseDTO(String code, Integer freeDays, String label) {
        this.code = code;
        this.freeDays = freeDays;
        this.label = label;
    }

    public String getCode() { return code; }
    public Integer getFreeDays() { return freeDays; }
    public String getLabel() { return label; }
}
