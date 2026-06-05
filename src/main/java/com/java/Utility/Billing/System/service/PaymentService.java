package com.java.Utility.Billing.System.service;

import com.java.Utility.Billing.System.dto.request.PaymentRequest;
import com.java.Utility.Billing.System.dto.response.PageResponse;
import com.java.Utility.Billing.System.dto.response.PaymentResponse;
import com.java.Utility.Billing.System.entity.Bill;
import com.java.Utility.Billing.System.entity.Customer;
import com.java.Utility.Billing.System.entity.Payment;
import com.java.Utility.Billing.System.enums.BillStatus;
import com.java.Utility.Billing.System.exception.BadRequestException;
import com.java.Utility.Billing.System.exception.ResourceNotFoundException;
import com.java.Utility.Billing.System.mapper.EntityMapper;
import com.java.Utility.Billing.System.repository.BillRepository;
import com.java.Utility.Billing.System.repository.PaymentRepository;
import com.java.Utility.Billing.System.security.SecurityUtils;
import com.java.Utility.Billing.System.util.PageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BillRepository billRepository;
    private final BillService billService;
    private final EmailService emailService;
    private final SecurityUtils securityUtils;

    @Transactional
    public PaymentResponse recordPayment(PaymentRequest request) {
        Bill bill = billService.findBill(request.getBillId());

        if (bill.getStatus() == BillStatus.PAID) {
            throw new BadRequestException("Bill is already fully paid");
        }
        if (bill.getStatus() == BillStatus.PENDING) {
            throw new BadRequestException("Bill must be approved before payment");
        }
        if (request.getAmountPaid().compareTo(bill.getOutstandingBalance()) > 0) {
            throw new BadRequestException("Payment amount exceeds outstanding balance");
        }

        bill.setPaidAmount(bill.getPaidAmount().add(request.getAmountPaid()));
        bill.setOutstandingBalance(bill.getOutstandingBalance().subtract(request.getAmountPaid()));

        boolean fullyPaid = false;
        if (bill.getOutstandingBalance().compareTo(BigDecimal.ZERO) <= 0) {
            bill.setOutstandingBalance(BigDecimal.ZERO);
            bill.setStatus(BillStatus.PAID);
            fullyPaid = true;
        } else {
            bill.setStatus(BillStatus.PARTIALLY_PAID);
        }

        billRepository.save(bill);

        Payment payment = Payment.builder()
                .bill(bill)
                .amountPaid(request.getAmountPaid())
                .paymentMethod(request.getPaymentMethod())
                .paymentDate(request.getPaymentDate())
                .recordedBy(securityUtils.getCurrentUserEmail())
                .build();

        payment = paymentRepository.save(payment);

        if (fullyPaid) {
            Customer customer = bill.getCustomer();
            String monthYear = Month.of(bill.getBillingMonth())
                    .getDisplayName(TextStyle.FULL, Locale.ENGLISH) + "/" + bill.getBillingYear();
            emailService.sendNotificationEmail(customer.getEmail(), "Payment Received",
                    String.format("Dear %s,\nYour %s utility bill of %s FRW has been fully paid. Thank you!",
                            customer.getFullNames(), monthYear, bill.getTotalAmount()));
        }

        log.info("Payment recorded: {} FRW for bill {}", request.getAmountPaid(), bill.getBillReference());
        return EntityMapper.toPaymentResponse(payment);
    }

    public PaymentResponse getById(Long id) {
        return EntityMapper.toPaymentResponse(findPayment(id));
    }

    public PageResponse<PaymentResponse> getAll(Pageable pageable) {
        Page<PaymentResponse> page = paymentRepository.findAll(pageable).map(EntityMapper::toPaymentResponse);
        return PageMapper.toPageResponse(page);
    }

    public PageResponse<PaymentResponse> getByBill(Long billId, Pageable pageable) {
        Page<PaymentResponse> page = paymentRepository.findByBillId(billId, pageable).map(EntityMapper::toPaymentResponse);
        return PageMapper.toPageResponse(page);
    }

    @Transactional
    public void delete(Long id) {
        Payment payment = findPayment(id);
        paymentRepository.delete(payment);
    }

    private Payment findPayment(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
    }
}
