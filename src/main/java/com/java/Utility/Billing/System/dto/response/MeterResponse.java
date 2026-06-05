package com.java.Utility.Billing.System.dto.response;

import com.java.Utility.Billing.System.enums.MeterStatus;
import com.java.Utility.Billing.System.enums.MeterType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class MeterResponse {
    private Long id;
    private String meterNumber;
    private MeterType meterType;
    private LocalDate installationDate;
    private MeterStatus status;
    private Long customerId;
    private String customerName;
    private LocalDateTime createdAt;
}
