package com.marketplace.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class GeneratePromoCodesRequestDTO {
    @NotNull @Min(1) @Max(5000)
    private Integer quantity;

    /** Dias de teste que cada código vai conceder (ex: 15 ou 30). */
    @NotNull @Min(1) @Max(365)
    private Integer freeDays;

    /** Opcional: rótulo do lote (ex: "LANCAMENTO-15"). */
    private String label;

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Integer getFreeDays() { return freeDays; }
    public void setFreeDays(Integer freeDays) { this.freeDays = freeDays; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
}
