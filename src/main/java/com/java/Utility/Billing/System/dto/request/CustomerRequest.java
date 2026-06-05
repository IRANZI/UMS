package com.java.Utility.Billing.System.dto.request;

import com.java.Utility.Billing.System.enums.CustomerStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "Customer details: fullNames, nationalId (unique), email, phoneNumber, address, status")
public class CustomerRequest {

    @NotBlank(message = "Full names are required")
    @Schema(example = "Jean Uwimana")
    private String fullNames;

    @NotBlank(message = "National ID is required")
    @Pattern(regexp = "^[0-9]{16}$", message = "National ID must be exactly 16 digits")
    @Schema(example = "1199887766554433", description = "Unique 16-digit National ID")
    private String nationalId;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(example = "jean.uwimana@email.com")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number")
    @Schema(example = "+250788123456")
    private String phoneNumber;

    @NotBlank(message = "Address is required")
    @Schema(example = "Kigali, Gasabo District, Rwanda")
    private String address;

    @Schema(example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE"},
            description = "ACTIVE or INACTIVE. Defaults to ACTIVE if omitted.")
    private CustomerStatus status;

    @Schema(description = "Optional link to system user account", example = "null")
    private Long userId;
}
