package com.marketplace.backend.dto;

import com.marketplace.backend.enums.SubscriptionStatus;

public class UpdateBusinessStatusDTO {
    private Boolean active;
    private SubscriptionStatus subscriptionStatus;
    private Boolean featured;

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public SubscriptionStatus getSubscriptionStatus() { return subscriptionStatus; }
    public void setSubscriptionStatus(SubscriptionStatus subscriptionStatus) { this.subscriptionStatus = subscriptionStatus; }
    public Boolean getFeatured() { return featured; }
    public void setFeatured(Boolean featured) { this.featured = featured; }
}