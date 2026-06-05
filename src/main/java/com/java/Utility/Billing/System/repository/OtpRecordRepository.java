package com.java.Utility.Billing.System.repository;

import com.java.Utility.Billing.System.entity.OtpRecord;
import com.java.Utility.Billing.System.enums.OtpType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpRecordRepository extends JpaRepository<OtpRecord, Long> {
    Optional<OtpRecord> findTopByEmailAndTypeAndUsedFalseOrderByCreatedAtDesc(String email, OtpType type);
}
