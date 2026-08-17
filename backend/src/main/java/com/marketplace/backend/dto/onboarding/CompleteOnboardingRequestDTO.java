package com.marketplace.backend.dto.onboarding;

import jakarta.validation.constraints.Min;


    public class CompleteOnboardingRequestDTO {
        @Min(0)
        private Integer trialMonths = 1; // mantido só por compatibilidade, não é mais usado pra decidir cobrança

        private String promoCode;

        public Integer getTrialMonths() { return trialMonths; }
        public void setTrialMonths(Integer trialMonths) { this.trialMonths = trialMonths; }
        public String getPromoCode() { return promoCode; }
        public void setPromoCode(String promoCode) { this.promoCode = promoCode; }
    }
