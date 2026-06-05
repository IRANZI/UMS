package com.java.Utility.Billing.System.repository;

import com.java.Utility.Billing.System.entity.Meter;
import com.java.Utility.Billing.System.enums.MeterStatus;
import com.java.Utility.Billing.System.enums.MeterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MeterRepository extends JpaRepository<Meter, Long> {
    Optional<Meter> findByMeterNumber(String meterNumber);
    boolean existsByMeterNumber(String meterNumber);
    List<Meter> findByCustomerId(Long customerId);
    Page<Meter> findByCustomerId(Long customerId, Pageable pageable);

    @Query("SELECT m FROM Meter m WHERE " +
           "m.meterNumber LIKE CONCAT('%', :search, '%') OR " +
           "LOWER(m.customer.fullNames) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Meter> search(@Param("search") String search, Pageable pageable);

    Page<Meter> findByMeterType(MeterType meterType, Pageable pageable);
    Page<Meter> findByStatus(MeterStatus status, Pageable pageable);
}
