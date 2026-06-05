package com.java.Utility.Billing.System.repository;

import com.java.Utility.Billing.System.entity.Penalty;
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

public interface PenaltyRepository extends JpaRepository<Penalty, Long> {

    @Query("SELECT p FROM Penalty p WHERE p.active = true AND p.effectiveFrom <= :date " +
           "AND (p.effectiveTo IS NULL OR p.effectiveTo >= :date) ORDER BY p.version DESC")
    List<Penalty> findActivePenaltiesForDate(@Param("date") LocalDate date, Pageable pageable);

    default Optional<Penalty> findActivePenaltyForDate(LocalDate date) {
        List<Penalty> penalties = findActivePenaltiesForDate(date, Pageable.ofSize(1));
        return penalties.isEmpty() ? Optional.empty() : Optional.of(penalties.get(0));
    }

    Page<Penalty> findByActive(boolean active, Pageable pageable);
}
