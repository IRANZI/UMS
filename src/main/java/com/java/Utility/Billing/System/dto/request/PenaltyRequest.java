package com.java.Utility.Billing.System.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PenaltyRequest {

    @NotBlank(message = "Penalty name is required")
    private String name;

    @NotNull(message = "Percentage is required")
    @DecimalMin(value = "0.0", message = "Percentage must be >= 0")
    private BigDecimal percentage;

    @NotNull(message = "Grace period days is required")
    @Min(value = 0, message = "Grace period must be >= 0")
    private Integer gracePeriodDays;

    @NotNull(message = "Effective from date is required")
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;
}
