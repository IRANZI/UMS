package com.java.Utility.Billing.System.mapper;

import com.java.Utility.Billing.System.dto.response.*;
import com.java.Utility.Billing.System.entity.*;
import com.java.Utility.Billing.System.enums.RoleName;

import java.math.BigDecimal;
import java.util.stream.Collectors;

public final class EntityMapper {

    private EntityMapper() {}

    public static UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullNames(user.getFullNames())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .status(user.getStatus())
                .emailVerified(user.isEmailVerified())
                .mustChangePassword(user.isMustChangePassword())
                .profilePicturePath(user.getProfilePicturePath())
                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public static CustomerResponse toCustomerResponse(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .fullNames(customer.getFullNames())
                .nationalId(customer.getNationalId())
                .email(customer.getEmail())
                .phoneNumber(customer.getPhoneNumber())
                .address(customer.getAddress())
                .status(customer.getStatus())
                .userId(customer.getUser() != null ? customer.getUser().getId() : null)
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }

    public static MeterResponse toMeterResponse(Meter meter) {
        return MeterResponse.builder()
                .id(meter.getId())
                .meterNumber(meter.getMeterNumber())
                .meterType(meter.getMeterType())
                .installationDate(meter.getInstallationDate())
                .status(meter.getStatus())
                .customerId(meter.getCustomer().getId())
                .customerName(meter.getCustomer().getFullNames())
                .createdAt(meter.getCreatedAt())
                .build();
    }

    public static MeterReadingResponse toMeterReadingResponse(MeterReading reading) {
        BigDecimal consumption = reading.getCurrentReading().subtract(reading.getPreviousReading());
        return MeterReadingResponse.builder()
                .id(reading.getId())
                .meterId(reading.getMeter().getId())
                .meterNumber(reading.getMeter().getMeterNumber())
                .previousReading(reading.getPreviousReading())
                .currentReading(reading.getCurrentReading())
                .consumption(consumption)
                .readingDate(reading.getReadingDate())
                .readingMonth(reading.getReadingMonth())
                .readingYear(reading.getReadingYear())
                .createdAt(reading.getCreatedAt())
                .build();
    }

    public static TariffResponse toTariffResponse(Tariff tariff) {
        return TariffResponse.builder()
                .id(tariff.getId())
                .name(tariff.getName())
                .meterType(tariff.getMeterType())
                .tariffType(tariff.getTariffType())
                .version(tariff.getVersion())
                .effectiveFrom(tariff.getEffectiveFrom())
                .effectiveTo(tariff.getEffectiveTo())
                .active(tariff.isActive())
                .fixedServiceCharge(tariff.getFixedServiceCharge())
                .unitRate(tariff.getUnitRate())
                .tiers(tariff.getTiers().stream().map(EntityMapper::toTariffTierResponse).toList())
                .createdAt(tariff.getCreatedAt())
                .build();
    }

    public static TariffTierResponse toTariffTierResponse(TariffTier tier) {
        return TariffTierResponse.builder()
                .id(tier.getId())
                .minUnits(tier.getMinUnits())
                .maxUnits(tier.getMaxUnits())
                .ratePerUnit(tier.getRatePerUnit())
                .build();
    }

    public static TaxResponse toTaxResponse(Tax tax) {
        return TaxResponse.builder()
                .id(tax.getId())
                .name(tax.getName())
                .percentage(tax.getPercentage())
                .version(tax.getVersion())
                .effectiveFrom(tax.getEffectiveFrom())
                .effectiveTo(tax.getEffectiveTo())
                .active(tax.isActive())
                .createdAt(tax.getCreatedAt())
                .build();
    }

    public static PenaltyResponse toPenaltyResponse(Penalty penalty) {
        return PenaltyResponse.builder()
                .id(penalty.getId())
                .name(penalty.getName())
                .percentage(penalty.getPercentage())
                .gracePeriodDays(penalty.getGracePeriodDays())
                .version(penalty.getVersion())
                .effectiveFrom(penalty.getEffectiveFrom())
                .effectiveTo(penalty.getEffectiveTo())
                .active(penalty.isActive())
                .createdAt(penalty.getCreatedAt())
                .build();
    }

    public static BillResponse toBillResponse(Bill bill) {
        return BillResponse.builder()
                .id(bill.getId())
                .billReference(bill.getBillReference())
                .customerId(bill.getCustomer().getId())
                .customerName(bill.getCustomer().getFullNames())
                .billingMonth(bill.getBillingMonth())
                .billingYear(bill.getBillingYear())
                .subtotal(bill.getSubtotal())
                .taxAmount(bill.getTaxAmount())
                .penaltyAmount(bill.getPenaltyAmount())
                .totalAmount(bill.getTotalAmount())
                .paidAmount(bill.getPaidAmount())
                .outstandingBalance(bill.getOutstandingBalance())
                .status(bill.getStatus())
                .approvedBy(bill.getApprovedBy())
                .approvedAt(bill.getApprovedAt())
                .lineItems(bill.getLineItems().stream().map(EntityMapper::toBillLineItemResponse).toList())
                .createdAt(bill.getCreatedAt())
                .build();
    }

    public static BillLineItemResponse toBillLineItemResponse(BillLineItem item) {
        return BillLineItemResponse.builder()
                .id(item.getId())
                .meterId(item.getMeter().getId())
                .meterNumber(item.getMeter().getMeterNumber())
                .meterType(item.getMeter().getMeterType())
                .consumption(item.getConsumption())
                .amount(item.getAmount())
                .description(item.getDescription())
                .build();
    }

    public static PaymentResponse toPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .billId(payment.getBill().getId())
                .billReference(payment.getBill().getBillReference())
                .amountPaid(payment.getAmountPaid())
                .paymentMethod(payment.getPaymentMethod())
                .paymentDate(payment.getPaymentDate())
                .recordedBy(payment.getRecordedBy())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    public static NotificationResponse toNotificationResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .customerId(notification.getCustomer().getId())
                .customerName(notification.getCustomer().getFullNames())
                .message(notification.getMessage())
                .type(notification.getType())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    public static DocumentResponse toDocumentResponse(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .fileName(document.getFileName())
                .originalFileName(document.getOriginalFileName())
                .contentType(document.getContentType())
                .fileSize(document.getFileSize())
                .userId(document.getUser() != null ? document.getUser().getId() : null)
                .customerId(document.getCustomer() != null ? document.getCustomer().getId() : null)
                .createdAt(document.getCreatedAt())
                .build();
    }
}
