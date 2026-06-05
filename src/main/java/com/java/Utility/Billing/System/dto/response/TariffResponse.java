package com.java.Utility.Billing.System.dto.response;

import com.java.Utility.Billing.System.enums.MeterType;
import com.java.Utility.Billing.System.enums.TariffType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TariffResponse {
    private Long id;
    private String name;
    private MeterType meterType;
    private TariffType tariffType;
    private int version;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private boolean active;
    private BigDecimal fixedServiceCharge;
    private BigDecimal unitRate;
    private List<TariffTierResponse> tiers;
    private LocalDateTime createdAt;
}
