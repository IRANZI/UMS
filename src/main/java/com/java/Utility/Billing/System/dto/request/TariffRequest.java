package com.java.Utility.Billing.System.dto.request;

import com.java.Utility.Billing.System.enums.MeterType;
import com.java.Utility.Billing.System.enums.TariffType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class TariffRequest {

    @NotBlank(message = "Tariff name is required")
    private String name;

    @NotNull(message = "Meter type is required")
    private MeterType meterType;

    @NotNull(message = "Tariff type is required")
    private TariffType tariffType;

    @NotNull(message = "Effective from date is required")
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    @NotNull(message = "Fixed service charge is required")
    private BigDecimal fixedServiceCharge;

    private BigDecimal unitRate;

    @Valid
    private List<TariffTierRequest> tiers;
}
