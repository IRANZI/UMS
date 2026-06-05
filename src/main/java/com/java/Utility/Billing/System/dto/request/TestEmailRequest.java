package com.java.Utility.Billing.System.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Send a test email to verify Gmail SMTP configuration")
public class TestEmailRequest {

    @NotBlank(message = "Recipient email is required")
    @Email(message = "Invalid email format")
    @Schema(example = "mrshhh39@gmail.com")
    private String to;
}
