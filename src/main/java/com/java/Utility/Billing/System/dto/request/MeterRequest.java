package com.java.Utility.Billing.System.dto.request;

import com.java.Utility.Billing.System.enums.MeterStatus;
import com.java.Utility.Billing.System.enums.MeterType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MeterRequest {

    @NotBlank(message = "Meter number is required")
    private String meterNumber;

    @NotNull(message = "Meter type is required")
    private MeterType meterType;

    @NotNull(message = "Installation date is required")
    private LocalDate installationDate;

    private MeterStatus status;

    @NotNull(message = "Customer ID is required")
    private Long customerId;
}
