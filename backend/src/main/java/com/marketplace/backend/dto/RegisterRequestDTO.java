// RegisterRequestDTO.java
package com.marketplace.backend.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequestDTO {
    @NotBlank private String name;
    @NotBlank @Email private String email;
    @NotBlank private String phone;

    @NotBlank
    @Size(min = 8, message = "A senha precisa ter pelo menos 8 caracteres")
    private String password;

    // Consentimento com Termos + Política. null é aceito (o app do cliente ainda
    // não envia); um false explícito é rejeitado.
    @AssertTrue(message = "É preciso aceitar os Termos de Uso e a Política de Privacidade")
    private Boolean acceptedTerms;

    // Token do Cloudflare Turnstile. Só é validado quando turnstile.enabled=true.
    private String captchaToken;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Boolean getAcceptedTerms() { return acceptedTerms; }
    public void setAcceptedTerms(Boolean acceptedTerms) { this.acceptedTerms = acceptedTerms; }
    public String getCaptchaToken() { return captchaToken; }
    public void setCaptchaToken(String captchaToken) { this.captchaToken = captchaToken; }
}
