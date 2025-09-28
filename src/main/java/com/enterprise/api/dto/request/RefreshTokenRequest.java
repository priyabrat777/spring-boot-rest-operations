package com.enterprise.api.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for token refresh operations.
 * Contains the refresh token for generating new access tokens.
 * 
 * Requirements addressed:
 * - 4.2: Token refresh functionality
 */
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;

    // Constructors
    public RefreshTokenRequest() {}

    public RefreshTokenRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    // Getters and Setters
    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    @Override
    public String toString() {
        return "RefreshTokenRequest{" +
                "refreshToken='[PROTECTED]'" +
                '}';
    }
}