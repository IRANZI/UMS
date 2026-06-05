package com.java.Utility.Billing.System.service;

import com.java.Utility.Billing.System.dto.request.TaxRequest;
import com.java.Utility.Billing.System.dto.response.PageResponse;
import com.java.Utility.Billing.System.dto.response.TaxResponse;
import com.java.Utility.Billing.System.entity.Tax;
import com.java.Utility.Billing.System.exception.ResourceNotFoundException;
import com.java.Utility.Billing.System.mapper.EntityMapper;
import com.java.Utility.Billing.System.repository.TaxRepository;
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
public class TaxService {

    private final TaxRepository taxRepository;

    @Transactional
    public TaxResponse create(TaxRequest request) {
        int nextVersion = getNextVersion(request.getName());
        Tax tax = Tax.builder()
                .name(request.getName())
                .percentage(request.getPercentage())
                .version(nextVersion)
                .effectiveFrom(request.getEffectiveFrom())
                .effectiveTo(request.getEffectiveTo())
                .active(true)
                .build();
        tax = taxRepository.save(tax);
        log.info("Tax created: {} v{}", tax.getName(), tax.getVersion());
        return EntityMapper.toTaxResponse(tax);
    }

    public TaxResponse getById(Long id) {
        return EntityMapper.toTaxResponse(findTax(id));
    }

    public PageResponse<TaxResponse> getAll(Pageable pageable) {
        Page<TaxResponse> page = taxRepository.findAll(pageable).map(EntityMapper::toTaxResponse);
        return PageMapper.toPageResponse(page);
    }

    @Transactional
    public TaxResponse update(Long id, TaxRequest request) {
        Tax existing = findTax(id);
        existing.setActive(false);
        existing.setEffectiveTo(request.getEffectiveFrom().minusDays(1));
        taxRepository.save(existing);
        return create(request);
    }

    @Transactional
    public void delete(Long id) {
        Tax tax = findTax(id);
        tax.setActive(false);
        taxRepository.save(tax);
    }

    public Tax findTax(Long id) {
        return taxRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tax not found with id: " + id));
    }

    private int getNextVersion(String name) {
        return taxRepository.findAll().stream()
                .filter(t -> t.getName().equals(name))
                .mapToInt(Tax::getVersion)
                .max().orElse(0) + 1;
    }
}
