package com.java.Utility.Billing.System.dto.response;

import com.java.Utility.Billing.System.enums.MeterType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BillLineItemResponse {
    private Long id;
    private Long meterId;
    private String meterNumber;
    private MeterType meterType;
    private BigDecimal consumption;
    private BigDecimal amount;
    private String description;
}
