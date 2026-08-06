package com.marketplace.backend.dto;

import com.marketplace.backend.enums.GiftCardCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public class GiftCardRequestDTO {
    @NotNull
    private GiftCardCategory category;

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private BigDecimal price;

    private List<String> benefits;

    private String validityLabel;

    public GiftCardCategory getCategory() { return category; }
    public void setCategory(GiftCardCategory category) { this.category = category; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public List<String> getBenefits() { return benefits; }
    public void setBenefits(List<String> benefits) { this.benefits = benefits; }
    public String getValidityLabel() { return validityLabel; }
    public void setValidityLabel(String validityLabel) { this.validityLabel = validityLabel; }
}