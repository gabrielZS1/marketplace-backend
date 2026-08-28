package com.marketplace.backend.dto;

import com.marketplace.backend.entity.TimeBlock;

import java.time.OffsetDateTime;
import java.util.UUID;

public class TimeBlockResponseDTO {
    private UUID id;
    private OffsetDateTime startsAt;
    private OffsetDateTime endsAt;
    private String reason;

    public TimeBlockResponseDTO(TimeBlock b) {
        this.id = b.getId();
        this.startsAt = b.getStartsAt();
        this.endsAt = b.getEndsAt();
        this.reason = b.getReason();
    }

    public UUID getId() { return id; }
    public OffsetDateTime getStartsAt() { return startsAt; }
    public OffsetDateTime getEndsAt() { return endsAt; }
    public String getReason() { return reason; }
}
