package com.java.Utility.Billing.System.service;

import com.java.Utility.Billing.System.dto.request.PenaltyRequest;
import com.java.Utility.Billing.System.dto.response.PageResponse;
import com.java.Utility.Billing.System.dto.response.PenaltyResponse;
import com.java.Utility.Billing.System.entity.Penalty;
import com.java.Utility.Billing.System.exception.ResourceNotFoundException;
import com.java.Utility.Billing.System.mapper.EntityMapper;
import com.java.Utility.Billing.System.repository.PenaltyRepository;
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
public class PenaltyService {

    private final PenaltyRepository penaltyRepository;

    @Transactional
    public PenaltyResponse create(PenaltyRequest request) {
        int nextVersion = getNextVersion(request.getName());
        Penalty penalty = Penalty.builder()
                .name(request.getName())
                .percentage(request.getPercentage())
                .gracePeriodDays(request.getGracePeriodDays())
                .version(nextVersion)
                .effectiveFrom(request.getEffectiveFrom())
                .effectiveTo(request.getEffectiveTo())
                .active(true)
                .build();
        penalty = penaltyRepository.save(penalty);
        log.info("Penalty created: {} v{}", penalty.getName(), penalty.getVersion());
        return EntityMapper.toPenaltyResponse(penalty);
    }

    public PenaltyResponse getById(Long id) {
        return EntityMapper.toPenaltyResponse(findPenalty(id));
    }

    public PageResponse<PenaltyResponse> getAll(Pageable pageable) {
        Page<PenaltyResponse> page = penaltyRepository.findAll(pageable).map(EntityMapper::toPenaltyResponse);
        return PageMapper.toPageResponse(page);
    }

    @Transactional
    public PenaltyResponse update(Long id, PenaltyRequest request) {
        Penalty existing = findPenalty(id);
        existing.setActive(false);
        existing.setEffectiveTo(request.getEffectiveFrom().minusDays(1));
        penaltyRepository.save(existing);
        return create(request);
    }

    @Transactional
    public void delete(Long id) {
        Penalty penalty = findPenalty(id);
        penalty.setActive(false);
        penaltyRepository.save(penalty);
    }

    public Penalty findPenalty(Long id) {
        return penaltyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Penalty not found with id: " + id));
    }

    private int getNextVersion(String name) {
        return penaltyRepository.findAll().stream()
                .filter(p -> p.getName().equals(name))
                .mapToInt(Penalty::getVersion)
                .max().orElse(0) + 1;
    }
}
