package com.java.Utility.Billing.System.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BillGenerationRequest {

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotNull(message = "Billing month is required")
    @Min(value = 1, message = "Month must be between 1 and 12")
    @Max(value = 12, message = "Month must be between 1 and 12")
    private Integer billingMonth;

    @NotNull(message = "Billing year is required")
    @Min(value = 2020, message = "Invalid billing year")
    private Integer billingYear;
}
