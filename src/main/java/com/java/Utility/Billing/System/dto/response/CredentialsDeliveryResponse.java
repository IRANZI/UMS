package com.java.Utility.Billing.System.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CredentialsDeliveryResponse {
    private UserResponse user;
    private boolean emailSent;
    private String temporaryPassword;
    private String deliveryNote;
}
