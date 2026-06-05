package com.java.Utility.Billing.System.repository;

import com.java.Utility.Billing.System.entity.Tax;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TaxRepository extends JpaRepository<Tax, Long> {

    @Query("SELECT t FROM Tax t WHERE t.active = true AND t.effectiveFrom <= :date " +
           "AND (t.effectiveTo IS NULL OR t.effectiveTo >= :date) ORDER BY t.version DESC")
    List<Tax> findActiveTaxesForDate(@Param("date") LocalDate date, Pageable pageable);

    default Optional<Tax> findActiveTaxForDate(LocalDate date) {
        List<Tax> taxes = findActiveTaxesForDate(date, Pageable.ofSize(1));
        return taxes.isEmpty() ? Optional.empty() : Optional.of(taxes.get(0));
    }

    Page<Tax> findByActive(boolean active, Pageable pageable);
}
