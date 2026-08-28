package com.marketplace.backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ReviewResponseDTO {

    private UUID id;
    private String clientName;
    private Integer rating;
    private String comment;
    private OffsetDateTime createdAt;
    private String employeeName;
    private String serviceName;
    private String photoUrl;

    public ReviewResponseDTO(UUID id, String clientName, Integer rating,
                             String comment, OffsetDateTime createdAt,
                             String employeeName, String serviceName,
                             String photoUrl) {
        this.id = id;
        this.clientName = clientName;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
        this.employeeName = employeeName;
        this.serviceName = serviceName;
        this.photoUrl = photoUrl;
    }

    public UUID getId()             { return id; }
    public String getClientName()   { return clientName; }
    public Integer getRating()      { return rating; }
    public String getComment()      { return comment; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public String getEmployeeName() { return employeeName; }
    public String getServiceName()  { return serviceName; }
    public String getPhotoUrl()     { return photoUrl; }
}