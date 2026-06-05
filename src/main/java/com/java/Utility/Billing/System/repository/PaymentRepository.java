package com.java.Utility.Billing.System.repository;

import com.java.Utility.Billing.System.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Page<Payment> findByBillId(Long billId, Pageable pageable);
    Page<Payment> findByBillCustomerId(Long customerId, Pageable pageable);
}
