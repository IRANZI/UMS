package com.java.Utility.Billing.System.service;

import com.java.Utility.Billing.System.dto.request.MeterReadingRequest;
import com.java.Utility.Billing.System.dto.response.MeterReadingResponse;
import com.java.Utility.Billing.System.dto.response.PageResponse;
import com.java.Utility.Billing.System.entity.Meter;
import com.java.Utility.Billing.System.entity.MeterReading;
import com.java.Utility.Billing.System.enums.MeterStatus;
import com.java.Utility.Billing.System.exception.BadRequestException;
import com.java.Utility.Billing.System.exception.ResourceNotFoundException;
import com.java.Utility.Billing.System.mapper.EntityMapper;
import com.java.Utility.Billing.System.repository.MeterReadingRepository;
import com.java.Utility.Billing.System.util.PageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeterReadingService {

    private final MeterReadingRepository meterReadingRepository;
    private final MeterService meterService;

    @Transactional
    public MeterReadingResponse create(MeterReadingRequest request) {
        Meter meter = meterService.findMeter(request.getMeterId());

        if (meter.getStatus() != MeterStatus.ACTIVE) {
            throw new BadRequestException("Meter must be active to capture readings");
        }
        if (request.getCurrentReading().compareTo(request.getPreviousReading()) <= 0) {
            throw new BadRequestException("Current reading must be greater than previous reading");
        }

        int month = request.getReadingDate().getMonthValue();
        int year = request.getReadingDate().getYear();

        if (meterReadingRepository.existsByMeterIdAndReadingMonthAndReadingYear(meter.getId(), month, year)) {
            throw new BadRequestException("A reading already exists for this meter in " + month + "/" + year);
        }

        MeterReading reading = MeterReading.builder()
                .meter(meter)
                .previousReading(request.getPreviousReading())
                .currentReading(request.getCurrentReading())
                .readingDate(request.getReadingDate())
                .readingMonth(month)
                .readingYear(year)
                .build();

        reading = meterReadingRepository.save(reading);
        log.info("Meter reading captured for meter {} - {}/{}", meter.getMeterNumber(), month, year);
        return EntityMapper.toMeterReadingResponse(reading);
    }

    public MeterReadingResponse getById(Long id) {
        return EntityMapper.toMeterReadingResponse(findReading(id));
    }

    public PageResponse<MeterReadingResponse> getAll(Pageable pageable) {
        Page<MeterReadingResponse> page = meterReadingRepository.findAll(pageable).map(EntityMapper::toMeterReadingResponse);
        return PageMapper.toPageResponse(page);
    }

    public PageResponse<MeterReadingResponse> getByMeter(Long meterId, Pageable pageable) {
        Page<MeterReadingResponse> page = meterReadingRepository.findByMeterId(meterId, pageable)
                .map(EntityMapper::toMeterReadingResponse);
        return PageMapper.toPageResponse(page);
    }

    @Transactional
    public MeterReadingResponse update(Long id, MeterReadingRequest request) {
        MeterReading reading = findReading(id);
        Meter meter = meterService.findMeter(request.getMeterId());

        if (meter.getStatus() != MeterStatus.ACTIVE) {
            throw new BadRequestException("Meter must be active");
        }
        if (request.getCurrentReading().compareTo(request.getPreviousReading()) <= 0) {
            throw new BadRequestException("Current reading must be greater than previous reading");
        }

        int month = request.getReadingDate().getMonthValue();
        int year = request.getReadingDate().getYear();

        reading.setMeter(meter);
        reading.setPreviousReading(request.getPreviousReading());
        reading.setCurrentReading(request.getCurrentReading());
        reading.setReadingDate(request.getReadingDate());
        reading.setReadingMonth(month);
        reading.setReadingYear(year);

        reading = meterReadingRepository.save(reading);
        return EntityMapper.toMeterReadingResponse(reading);
    }

    @Transactional
    public void delete(Long id) {
        MeterReading reading = findReading(id);
        meterReadingRepository.delete(reading);
    }

    private MeterReading findReading(Long id) {
        return meterReadingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meter reading not found with id: " + id));
    }
}
