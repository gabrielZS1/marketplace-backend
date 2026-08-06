package com.marketplace.backend.dto;

import com.marketplace.backend.enums.GiftCardCategory;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class GiftCardResponseDTO {
    private UUID id;
    private GiftCardCategory category;
    private String title;
    private String description;
    private BigDecimal price;
    private List<String> benefits;
    private String validityLabel;

    public GiftCardResponseDTO(UUID id, GiftCardCategory category, String title, String description,
                               BigDecimal price, List<String> benefits, String validityLabel) {
        this.id = id;
        this.category = category;
        this.title = title;
        this.description = description;
        this.price = price;
        this.benefits = benefits;
        this.validityLabel = validityLabel;
    }

    public UUID getId() { return id; }
    public GiftCardCategory getCategory() { return category; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public List<String> getBenefits() { return benefits; }
    public String getValidityLabel() { return validityLabel; }
}