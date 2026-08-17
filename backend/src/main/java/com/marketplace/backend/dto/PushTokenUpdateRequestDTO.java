package com.marketplace.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class PushTokenUpdateRequestDTO {

    @NotBlank
    private String expoPushToken;

    public String getExpoPushToken() { return expoPushToken; }
    public void setExpoPushToken(String expoPushToken) { this.expoPushToken = expoPushToken; }
}