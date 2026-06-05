package com.java.Utility.Billing.System.service;

import com.java.Utility.Billing.System.dto.request.TariffRequest;
import com.java.Utility.Billing.System.dto.request.TariffTierRequest;
import com.java.Utility.Billing.System.dto.response.PageResponse;
import com.java.Utility.Billing.System.dto.response.TariffResponse;
import com.java.Utility.Billing.System.entity.Tariff;
import com.java.Utility.Billing.System.entity.TariffTier;
import com.java.Utility.Billing.System.enums.TariffType;
import com.java.Utility.Billing.System.exception.BadRequestException;
import com.java.Utility.Billing.System.exception.ResourceNotFoundException;
import com.java.Utility.Billing.System.mapper.EntityMapper;
import com.java.Utility.Billing.System.repository.TariffRepository;
import com.java.Utility.Billing.System.util.PageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TariffService {

    private final TariffRepository tariffRepository;

    @Transactional
    public TariffResponse create(TariffRequest request) {
        validateTariffRequest(request);
        int nextVersion = getNextVersion(request.getName(), request.getMeterType());

        Tariff tariff = Tariff.builder()
                .name(request.getName())
                .meterType(request.getMeterType())
                .tariffType(request.getTariffType())
                .version(nextVersion)
                .effectiveFrom(request.getEffectiveFrom())
                .effectiveTo(request.getEffectiveTo())
                .active(true)
                .fixedServiceCharge(request.getFixedServiceCharge())
                .unitRate(request.getUnitRate())
                .tiers(new ArrayList<>())
                .build();

        if (request.getTariffType() == TariffType.TIERED && request.getTiers() != null) {
            for (TariffTierRequest tierReq : request.getTiers()) {
                TariffTier tier = TariffTier.builder()
                        .tariff(tariff)
                        .minUnits(tierReq.getMinUnits())
                        .maxUnits(tierReq.getMaxUnits())
                        .ratePerUnit(tierReq.getRatePerUnit())
                        .build();
                tariff.getTiers().add(tier);
            }
        }

        tariff = tariffRepository.save(tariff);
        log.info("Tariff created: {} v{}", tariff.getName(), tariff.getVersion());
        return EntityMapper.toTariffResponse(tariff);
    }

    public TariffResponse getById(Long id) {
        return EntityMapper.toTariffResponse(findTariff(id));
    }

    public PageResponse<TariffResponse> getAll(Pageable pageable) {
        Page<TariffResponse> page = tariffRepository.findAll(pageable).map(EntityMapper::toTariffResponse);
        return PageMapper.toPageResponse(page);
    }

    @Transactional
    public TariffResponse update(Long id, TariffRequest request) {
        Tariff existing = findTariff(id);
        existing.setActive(false);
        existing.setEffectiveTo(request.getEffectiveFrom().minusDays(1));
        tariffRepository.save(existing);
        return create(request);
    }

    @Transactional
    public void delete(Long id) {
        Tariff tariff = findTariff(id);
        tariff.setActive(false);
        tariffRepository.save(tariff);
        log.info("Tariff deactivated: {}", tariff.getName());
    }

    public Tariff findTariff(Long id) {
        return tariffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tariff not found with id: " + id));
    }

    private int getNextVersion(String name, com.java.Utility.Billing.System.enums.MeterType meterType) {
        List<Tariff> existing = tariffRepository.findAll().stream()
                .filter(t -> t.getName().equals(name) && t.getMeterType() == meterType)
                .toList();
        return existing.stream().mapToInt(Tariff::getVersion).max().orElse(0) + 1;
    }

    private void validateTariffRequest(TariffRequest request) {
        if (request.getTariffType() == TariffType.FLAT && request.getUnitRate() == null) {
            throw new BadRequestException("Unit rate is required for flat tariffs");
        }
        if (request.getTariffType() == TariffType.TIERED
                && (request.getTiers() == null || request.getTiers().isEmpty())) {
            throw new BadRequestException("Tiers are required for tiered tariffs");
        }
    }
}
