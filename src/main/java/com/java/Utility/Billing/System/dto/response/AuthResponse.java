package com.java.Utility.Billing.System.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
    private boolean mustChangePassword;
    private UserResponse user;
}
