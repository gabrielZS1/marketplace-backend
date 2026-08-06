package com.marketplace.backend.dto;

public class SubscriptionResponseDTO {
    private String preapprovalId;
    private String paymentUrl;
    private String status;

    public SubscriptionResponseDTO(String preapprovalId, String paymentUrl, String status) {
        this.preapprovalId = preapprovalId;
        this.paymentUrl = paymentUrl;
        this.status = status;
    }

    public String getPreapprovalId() { return preapprovalId; }
    public String getPaymentUrl() { return paymentUrl; }
    public String getStatus() { return status; }
}