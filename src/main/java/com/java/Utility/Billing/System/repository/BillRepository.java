package com.java.Utility.Billing.System.repository;

import com.java.Utility.Billing.System.entity.Bill;
import com.java.Utility.Billing.System.enums.BillStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BillRepository extends JpaRepository<Bill, Long> {
    Optional<Bill> findByBillReference(String billReference);
    boolean existsByCustomerIdAndBillingMonthAndBillingYear(Long customerId, int month, int year);

    @Query("SELECT b FROM Bill b WHERE b.customer.id = :customerId")
    Page<Bill> findByCustomerId(@Param("customerId") Long customerId, Pageable pageable);

    @Query("SELECT b FROM Bill b WHERE " +
           "b.billReference LIKE CONCAT('%', :search, '%') OR " +
           "LOWER(b.customer.fullNames) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Bill> search(@Param("search") String search, Pageable pageable);

    Page<Bill> findByStatus(BillStatus status, Pageable pageable);
}
