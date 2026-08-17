package com.marketplace.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class GeneratePromoCodesRequestDTO {
    @NotNull @Min(1)
    private Integer quantity;

    @NotNull @Min(1)
    private Integer freeMonths;

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Integer getFreeMonths() { return freeMonths; }
    public void setFreeMonths(Integer freeMonths) { this.freeMonths = freeMonths; }
}