package com.java.Utility.Billing.System.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class MeterReadingRequest {

    @NotNull(message = "Meter ID is required")
    private Long meterId;

    @NotNull(message = "Previous reading is required")
    @DecimalMin(value = "0.0", message = "Previous reading must be >= 0")
    private BigDecimal previousReading;

    @NotNull(message = "Current reading is required")
    @DecimalMin(value = "0.0", message = "Current reading must be >= 0")
    private BigDecimal currentReading;

    @NotNull(message = "Reading date is required")
    private LocalDate readingDate;
}
