package com.java.Utility.Billing.System.controller;

import com.java.Utility.Billing.System.dto.request.PenaltyRequest;
import com.java.Utility.Billing.System.dto.response.ApiResponse;
import com.java.Utility.Billing.System.dto.response.PageResponse;
import com.java.Utility.Billing.System.dto.response.PenaltyResponse;
import com.java.Utility.Billing.System.service.PenaltyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/penalties")
@RequiredArgsConstructor
@Tag(name = "Penalties", description = "Late payment penalty configuration APIs")
@SecurityRequirement(name = "Bearer Authentication")
public class PenaltyController {

    private final PenaltyService penaltyService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new penalty version")
    public ApiResponse<PenaltyResponse> create(@Valid @RequestBody PenaltyRequest request) {
        return ApiResponse.success("Penalty created", penaltyService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(summary = "Get all penalties")
    public ApiResponse<PageResponse<PenaltyResponse>> getAll(@ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.success(penaltyService.getAll(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(summary = "Get penalty by ID")
    public ApiResponse<PenaltyResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(penaltyService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update penalty (creates new version)")
    public ApiResponse<PenaltyResponse> update(@PathVariable Long id, @Valid @RequestBody PenaltyRequest request) {
        return ApiResponse.success(penaltyService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate penalty")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        penaltyService.delete(id);
        return ApiResponse.success("Penalty deactivated");
    }
}
