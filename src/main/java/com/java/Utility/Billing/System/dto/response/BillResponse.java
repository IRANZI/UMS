package com.java.Utility.Billing.System.dto.response;

import com.java.Utility.Billing.System.enums.BillStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BillResponse {
    private Long id;
    private String billReference;
    private Long customerId;
    private String customerName;
    private int billingMonth;
    private int billingYear;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal penaltyAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal outstandingBalance;
    private BillStatus status;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private List<BillLineItemResponse> lineItems;
    private LocalDateTime createdAt;
}
