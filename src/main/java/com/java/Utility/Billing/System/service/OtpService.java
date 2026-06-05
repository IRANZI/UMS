package com.java.Utility.Billing.System.service;

import com.java.Utility.Billing.System.entity.OtpRecord;
import com.java.Utility.Billing.System.enums.OtpType;
import com.java.Utility.Billing.System.exception.BadRequestException;
import com.java.Utility.Billing.System.repository.OtpRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpRecordRepository otpRecordRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.otp.expiry-minutes}")
    private int expiryMinutes;

    @Value("${app.otp.length}")
    private int otpLength;

    @Transactional
    public String generateOtp(String email, OtpType type) {
        String otp = generateRandomOtp();
        OtpRecord record = OtpRecord.builder()
                .email(email)
                .otp(otp)
                .type(type)
                .expiryDate(LocalDateTime.now().plusMinutes(expiryMinutes))
                .used(false)
                .build();
        otpRecordRepository.save(record);
        return otp;
    }

    @Transactional
    public void verifyOtp(String email, String otp, OtpType type) {
        OtpRecord record = otpRecordRepository
                .findTopByEmailAndTypeAndUsedFalseOrderByCreatedAtDesc(email, type)
                .orElseThrow(() -> new BadRequestException("Invalid or expired OTP"));

        if (record.isUsed()) {
            throw new BadRequestException("OTP already used");
        }
        if (record.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP has expired");
        }
        if (!record.getOtp().equals(otp)) {
            throw new BadRequestException("Invalid OTP");
        }
        record.setUsed(true);
        otpRecordRepository.save(record);
    }

    private String generateRandomOtp() {
        int bound = (int) Math.pow(10, otpLength);
        int otp = secureRandom.nextInt(bound / 10, bound);
        return String.valueOf(otp);
    }
}
