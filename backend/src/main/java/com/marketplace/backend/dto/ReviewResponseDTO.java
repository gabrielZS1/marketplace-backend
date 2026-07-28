// ReviewResponseDTO.java
package com.marketplace.backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ReviewResponseDTO {
    private UUID id;
    private String clientName;
    private Integer rating;
    private String comment;
    private OffsetDateTime createdAt;

    public ReviewResponseDTO(UUID id, String clientName, Integer rating, String comment, OffsetDateTime createdAt) {
        this.id = id; this.clientName = clientName; this.rating = rating;
        this.comment = comment; this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getClientName() { return clientName; }
    public Integer getRating() { return rating; }
    public String getComment() { return comment; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}