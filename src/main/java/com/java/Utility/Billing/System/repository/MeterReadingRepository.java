package com.java.Utility.Billing.System.repository;

import com.java.Utility.Billing.System.entity.MeterReading;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MeterReadingRepository extends JpaRepository<MeterReading, Long> {
    boolean existsByMeterIdAndReadingMonthAndReadingYear(Long meterId, int month, int year);
    java.util.Optional<MeterReading> findByMeterIdAndReadingMonthAndReadingYear(Long meterId, int month, int year);
    Page<MeterReading> findByMeterId(Long meterId, Pageable pageable);
    Optional<MeterReading> findTopByMeterIdOrderByReadingYearDescReadingMonthDesc(Long meterId);
}
