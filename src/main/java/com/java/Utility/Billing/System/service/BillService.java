package com.java.Utility.Billing.System.service;

import com.java.Utility.Billing.System.dto.request.BillGenerationRequest;
import com.java.Utility.Billing.System.dto.response.BillResponse;
import com.java.Utility.Billing.System.dto.response.PageResponse;
import com.java.Utility.Billing.System.entity.*;
import com.java.Utility.Billing.System.enums.BillStatus;
import com.java.Utility.Billing.System.enums.MeterStatus;
import com.java.Utility.Billing.System.enums.RoleName;
import com.java.Utility.Billing.System.enums.TariffType;
import com.java.Utility.Billing.System.exception.BadRequestException;
import com.java.Utility.Billing.System.exception.ResourceNotFoundException;
import com.java.Utility.Billing.System.mapper.EntityMapper;
import com.java.Utility.Billing.System.repository.BillRepository;
import com.java.Utility.Billing.System.repository.MeterReadingRepository;
import com.java.Utility.Billing.System.repository.MeterRepository;
import com.java.Utility.Billing.System.repository.TariffRepository;
import com.java.Utility.Billing.System.repository.TaxRepository;
import com.java.Utility.Billing.System.repository.UserRepository;
import com.java.Utility.Billing.System.security.SecurityUtils;
import com.java.Utility.Billing.System.util.PageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillService {

    private final BillRepository billRepository;
    private final CustomerService customerService;
    private final MeterRepository meterRepository;
    private final MeterReadingRepository meterReadingRepository;
    private final TariffRepository tariffRepository;
    private final TaxRepository taxRepository;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    @Transactional
    public BillResponse generateBill(BillGenerationRequest request) {
        Customer customer = customerService.findCustomer(request.getCustomerId());

        customerService.ensureCustomerCanReceiveBills(customer);
        if (billRepository.existsByCustomerIdAndBillingMonthAndBillingYear(
                customer.getId(), request.getBillingMonth(), request.getBillingYear())) {
            throw new BadRequestException("Bill already exists for this billing period");
        }

        List<Meter> meters = meterRepository.findByCustomerId(customer.getId());
        if (meters.isEmpty()) {
            throw new BadRequestException("Customer has no meters");
        }

        LocalDate billingDate = LocalDate.of(request.getBillingYear(), request.getBillingMonth(), 1);
        BigDecimal subtotal = BigDecimal.ZERO;
        List<BillLineItem> lineItems = new ArrayList<>();

        for (Meter meter : meters) {
            if (meter.getStatus() != MeterStatus.ACTIVE) continue;

            MeterReading reading = meterReadingRepository
                    .findByMeterIdAndReadingMonthAndReadingYear(meter.getId(),
                            request.getBillingMonth(), request.getBillingYear())
                    .orElse(null);

            if (reading == null) continue;

            BigDecimal consumption = reading.getCurrentReading().subtract(reading.getPreviousReading());
            Tariff tariff = tariffRepository.findActiveTariffForDate(meter.getMeterType(), billingDate)
                    .orElseThrow(() -> new BadRequestException(
                            "No active tariff for " + meter.getMeterType() + " on " + billingDate));

            BigDecimal lineAmount = calculateLineAmount(tariff, consumption);
            subtotal = subtotal.add(lineAmount);

            BillLineItem lineItem = BillLineItem.builder()
                    .meter(meter)
                    .consumption(consumption)
                    .amount(lineAmount)
                    .description(meter.getMeterType() + " consumption - " + consumption + " units")
                    .build();
            lineItems.add(lineItem);
        }

        if (lineItems.isEmpty()) {
            throw new BadRequestException("No meter readings found for billing period");
        }

        Tax tax = taxRepository.findActiveTaxForDate(billingDate).orElse(null);
        BigDecimal taxAmount = BigDecimal.ZERO;
        if (tax != null) {
            taxAmount = subtotal.multiply(tax.getPercentage())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        BigDecimal totalAmount = subtotal.add(taxAmount);
        String billReference = "BILL-" + request.getBillingYear()
                + String.format("%02d", request.getBillingMonth()) + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Bill bill = Bill.builder()
                .billReference(billReference)
                .customer(customer)
                .billingMonth(request.getBillingMonth())
                .billingYear(request.getBillingYear())
                .subtotal(subtotal)
                .taxAmount(taxAmount)
                .penaltyAmount(BigDecimal.ZERO)
                .totalAmount(totalAmount)
                .paidAmount(BigDecimal.ZERO)
                .outstandingBalance(totalAmount)
                .status(BillStatus.PENDING)
                .lineItems(new ArrayList<>())
                .build();

        for (BillLineItem item : lineItems) {
            item.setBill(bill);
            bill.getLineItems().add(item);
        }

        bill = billRepository.save(bill);

        // Send emails: customer + Finance/Admin staff
        sendBillGeneratedEmails(bill, customer);

        log.info("Bill generated: {} for customer {}", billReference, customer.getFullNames());
        return EntityMapper.toBillResponse(bill);
    }

    @Transactional
    public BillResponse approveBill(Long id) {
        Bill bill = findBill(id);
        if (bill.getStatus() != BillStatus.PENDING) {
            throw new BadRequestException("Only pending bills can be approved");
        }
        bill.setStatus(BillStatus.APPROVED);
        bill.setApprovedBy(securityUtils.getCurrentUserEmail());
        bill.setApprovedAt(LocalDateTime.now());
        bill = billRepository.save(bill);

        // Notify customer that bill is approved and ready for payment
        sendBillApprovedEmail(bill);

        log.info("Bill approved: {}", bill.getBillReference());
        return EntityMapper.toBillResponse(bill);
    }

    /**
     * Emails the customer and all active Finance/Admin users when a bill is generated.
     */
    private void sendBillGeneratedEmails(Bill bill, Customer customer) {
        String billingPeriod = formatBillingPeriod(bill.getBillingMonth(), bill.getBillingYear());
        String generatedBy = securityUtils.getCurrentUserEmail() != null
                ? securityUtils.getCurrentUserEmail() : "system";

        // Customer email uses exam format: "Dear <Name>, Your <Month/Year> utility bill of <Amount> FRW..."
        String monthYear = formatMonthYear(bill.getBillingMonth(), bill.getBillingYear());
        boolean customerEmailed = emailService.sendBillGeneratedToCustomer(
                customer.getEmail(),
                customer.getFullNames(),
                monthYear,
                bill.getTotalAmount().toPlainString());

        if (customerEmailed) {
            log.info("Bill processed email sent to customer {}", customer.getEmail());
        } else {
            log.warn("Failed to send bill processed email to customer {}", customer.getEmail());
        }

        List<User> staffRecipients = userRepository.findActiveUsersByRoles(
                List.of(RoleName.ROLE_FINANCE, RoleName.ROLE_ADMIN));

        for (User staff : staffRecipients) {
            boolean sent = emailService.sendBillProcessedToStaff(
                    staff.getEmail(),
                    staff.getFullNames(),
                    customer.getFullNames(),
                    customer.getEmail(),
                    bill.getBillReference(),
                    billingPeriod,
                    bill.getTotalAmount().toPlainString(),
                    generatedBy);
            if (sent) {
                log.info("Bill processed alert sent to staff {}", staff.getEmail());
            } else {
                log.warn("Failed to send bill processed alert to staff {}", staff.getEmail());
            }
        }
    }

    /** Emails customer when bill moves from PENDING to APPROVED. */
    private void sendBillApprovedEmail(Bill bill) {
        Customer customer = bill.getCustomer();
        String billingPeriod = formatBillingPeriod(bill.getBillingMonth(), bill.getBillingYear());

        boolean sent = emailService.sendBillApprovedToCustomer(
                customer.getEmail(),
                customer.getFullNames(),
                bill.getBillReference(),
                billingPeriod,
                bill.getTotalAmount().toPlainString(),
                bill.getOutstandingBalance().toPlainString());

        if (sent) {
            log.info("Bill approved email sent to customer {}", customer.getEmail());
        } else {
            log.warn("Failed to send bill approved email to customer {}", customer.getEmail());
        }
    }

    /** Billing period for staff emails, e.g. "June 2026". */
    private String formatBillingPeriod(int month, int year) {
        return Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + year;
    }

    /** Month/Year format for customer bill email, e.g. "June/2026". */
    private String formatMonthYear(int month, int year) {
        return Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH) + "/" + year;
    }

    public BillResponse getById(Long id) {
        return EntityMapper.toBillResponse(findBill(id));
    }

    public BillResponse getByReference(String reference) {
        Bill bill = billRepository.findByBillReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found: " + reference));
        return EntityMapper.toBillResponse(bill);
    }

    public PageResponse<BillResponse> getAll(Pageable pageable) {
        Page<BillResponse> page = billRepository.findAll(pageable).map(EntityMapper::toBillResponse);
        return PageMapper.toPageResponse(page);
    }

    public PageResponse<BillResponse> search(String query, Pageable pageable) {
        Page<BillResponse> page = billRepository.search(query, pageable).map(EntityMapper::toBillResponse);
        return PageMapper.toPageResponse(page);
    }

    public PageResponse<BillResponse> getByCustomer(Long customerId, Pageable pageable) {
        Page<BillResponse> page = billRepository.findByCustomerId(customerId, pageable).map(EntityMapper::toBillResponse);
        return PageMapper.toPageResponse(page);
    }

    @Transactional
    public void delete(Long id) {
        Bill bill = findBill(id);
        if (bill.getStatus() == BillStatus.PAID) {
            throw new BadRequestException("Cannot delete a paid bill");
        }
        billRepository.delete(bill);
    }

    public Bill findBill(Long id) {
        return billRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with id: " + id));
    }

    private BigDecimal calculateLineAmount(Tariff tariff, BigDecimal consumption) {
        BigDecimal amount = tariff.getFixedServiceCharge();

        if (tariff.getTariffType() == TariffType.FLAT) {
            amount = amount.add(consumption.multiply(tariff.getUnitRate()));
        } else {
            BigDecimal remaining = consumption;
            for (TariffTier tier : tariff.getTiers()) {
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
                BigDecimal tierMax = tier.getMaxUnits() != null
                        ? tier.getMaxUnits().subtract(tier.getMinUnits())
                        : remaining;
                BigDecimal unitsInTier = remaining.min(tierMax);
                amount = amount.add(unitsInTier.multiply(tier.getRatePerUnit()));
                remaining = remaining.subtract(unitsInTier);
            }
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
