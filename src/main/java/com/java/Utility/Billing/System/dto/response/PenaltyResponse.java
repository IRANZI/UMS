package com.java.Utility.Billing.System.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class PenaltyResponse {
    private Long id;
    private String name;
    private BigDecimal percentage;
    private int gracePeriodDays;
    private int version;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private boolean active;
    private LocalDateTime createdAt;
}
