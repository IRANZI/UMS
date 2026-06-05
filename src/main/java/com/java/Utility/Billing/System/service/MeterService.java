package com.java.Utility.Billing.System.service;

import com.java.Utility.Billing.System.dto.request.MeterRequest;
import com.java.Utility.Billing.System.dto.response.MeterResponse;
import com.java.Utility.Billing.System.dto.response.PageResponse;
import com.java.Utility.Billing.System.entity.Customer;
import com.java.Utility.Billing.System.entity.Meter;
import com.java.Utility.Billing.System.enums.MeterStatus;
import com.java.Utility.Billing.System.exception.BadRequestException;
import com.java.Utility.Billing.System.exception.ResourceNotFoundException;
import com.java.Utility.Billing.System.mapper.EntityMapper;
import com.java.Utility.Billing.System.repository.MeterRepository;
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
public class MeterService {

    private final MeterRepository meterRepository;
    private final CustomerService customerService;

    @Transactional
    public MeterResponse create(MeterRequest request) {
        if (meterRepository.existsByMeterNumber(request.getMeterNumber())) {
            throw new BadRequestException("Meter number already exists");
        }
        Customer customer = customerService.findCustomer(request.getCustomerId());
        Meter meter = Meter.builder()
                .meterNumber(request.getMeterNumber())
                .meterType(request.getMeterType())
                .installationDate(request.getInstallationDate())
                .status(request.getStatus() != null ? request.getStatus() : MeterStatus.ACTIVE)
                .customer(customer)
                .build();
        meter = meterRepository.save(meter);
        log.info("Meter created: {}", meter.getMeterNumber());
        return EntityMapper.toMeterResponse(meter);
    }

    public MeterResponse getById(Long id) {
        return EntityMapper.toMeterResponse(findMeter(id));
    }

    public PageResponse<MeterResponse> getAll(Pageable pageable) {
        Page<MeterResponse> page = meterRepository.findAll(pageable).map(EntityMapper::toMeterResponse);
        return PageMapper.toPageResponse(page);
    }

    public PageResponse<MeterResponse> search(String query, Pageable pageable) {
        Page<MeterResponse> page = meterRepository.search(query, pageable).map(EntityMapper::toMeterResponse);
        return PageMapper.toPageResponse(page);
    }

    public PageResponse<MeterResponse> getByCustomer(Long customerId, Pageable pageable) {
        Page<MeterResponse> page = meterRepository.findByCustomerId(customerId, pageable).map(EntityMapper::toMeterResponse);
        return PageMapper.toPageResponse(page);
    }

    @Transactional
    public MeterResponse update(Long id, MeterRequest request) {
        Meter meter = findMeter(id);
        if (!meter.getMeterNumber().equals(request.getMeterNumber())
                && meterRepository.existsByMeterNumber(request.getMeterNumber())) {
            throw new BadRequestException("Meter number already exists");
        }
        Customer customer = customerService.findCustomer(request.getCustomerId());
        meter.setMeterNumber(request.getMeterNumber());
        meter.setMeterType(request.getMeterType());
        meter.setInstallationDate(request.getInstallationDate());
        if (request.getStatus() != null) {
            meter.setStatus(request.getStatus());
        }
        meter.setCustomer(customer);
        meter = meterRepository.save(meter);
        log.info("Meter updated: {}", meter.getMeterNumber());
        return EntityMapper.toMeterResponse(meter);
    }

    @Transactional
    public void delete(Long id) {
        Meter meter = findMeter(id);
        meterRepository.delete(meter);
        log.info("Meter deleted: {}", meter.getMeterNumber());
    }

    public Meter findMeter(Long id) {
        return meterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meter not found with id: " + id));
    }
}
