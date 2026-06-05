package com.java.Utility.Billing.System.dto.response;

import com.java.Utility.Billing.System.enums.PaymentMethod;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {
    private Long id;
    private Long billId;
    private String billReference;
    private BigDecimal amountPaid;
    private PaymentMethod paymentMethod;
    private LocalDate paymentDate;
    private String recordedBy;
    private LocalDateTime createdAt;
}
