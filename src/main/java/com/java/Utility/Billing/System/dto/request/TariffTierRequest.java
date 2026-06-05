package com.java.Utility.Billing.System.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TariffTierRequest {

    @NotNull(message = "Minimum units is required")
    @DecimalMin(value = "0.0", message = "Minimum units must be >= 0")
    private BigDecimal minUnits;

    private BigDecimal maxUnits;

    @NotNull(message = "Rate per unit is required")
    @DecimalMin(value = "0.0", message = "Rate must be >= 0")
    private BigDecimal ratePerUnit;
}
