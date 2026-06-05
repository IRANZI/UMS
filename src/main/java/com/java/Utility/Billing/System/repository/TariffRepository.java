package com.java.Utility.Billing.System.repository;

import com.java.Utility.Billing.System.entity.Tariff;
import com.java.Utility.Billing.System.enums.MeterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TariffRepository extends JpaRepository<Tariff, Long> {

    @Query("SELECT t FROM Tariff t WHERE t.meterType = :meterType AND t.active = true " +
           "AND t.effectiveFrom <= :date AND (t.effectiveTo IS NULL OR t.effectiveTo >= :date) " +
           "ORDER BY t.version DESC")
    List<Tariff> findActiveTariffsForDate(@Param("meterType") MeterType meterType, @Param("date") LocalDate date, Pageable pageable);

    default Optional<Tariff> findActiveTariffForDate(MeterType meterType, LocalDate date) {
        List<Tariff> tariffs = findActiveTariffsForDate(meterType, date, Pageable.ofSize(1));
        return tariffs.isEmpty() ? Optional.empty() : Optional.of(tariffs.get(0));
    }

    Page<Tariff> findByMeterType(MeterType meterType, Pageable pageable);
    Page<Tariff> findByActive(boolean active, Pageable pageable);
}
