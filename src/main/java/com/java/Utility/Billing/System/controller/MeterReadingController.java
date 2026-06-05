package com.java.Utility.Billing.System.controller;

import com.java.Utility.Billing.System.dto.request.MeterReadingRequest;
import com.java.Utility.Billing.System.dto.response.ApiResponse;
import com.java.Utility.Billing.System.dto.response.MeterReadingResponse;
import com.java.Utility.Billing.System.dto.response.PageResponse;
import com.java.Utility.Billing.System.service.MeterReadingService;
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
@RequestMapping("/api/meter-readings")
@RequiredArgsConstructor
@Tag(name = "Meter Readings", description = "Meter reading capture APIs")
@SecurityRequirement(name = "Bearer Authentication")
public class MeterReadingController {

    private final MeterReadingService meterReadingService;

    @PostMapping
    @PreAuthorize("hasRole('OPERATOR')")
    @Operation(summary = "Capture meter reading")
    public ApiResponse<MeterReadingResponse> create(@Valid @RequestBody MeterReadingRequest request) {
        return ApiResponse.success("Reading captured", meterReadingService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'FINANCE')")
    @Operation(summary = "Get all meter readings")
    public ApiResponse<PageResponse<MeterReadingResponse>> getAll(@ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.success(meterReadingService.getAll(pageable));
    }

    @GetMapping("/meter/{meterId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'FINANCE', 'CUSTOMER')")
    @Operation(summary = "Get readings by meter")
    public ApiResponse<PageResponse<MeterReadingResponse>> getByMeter(
            @PathVariable Long meterId, @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.success(meterReadingService.getByMeter(meterId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'FINANCE')")
    @Operation(summary = "Get reading by ID")
    public ApiResponse<MeterReadingResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(meterReadingService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OPERATOR')")
    @Operation(summary = "Update meter reading")
    public ApiResponse<MeterReadingResponse> update(
            @PathVariable Long id, @Valid @RequestBody MeterReadingRequest request) {
        return ApiResponse.success(meterReadingService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete meter reading")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        meterReadingService.delete(id);
        return ApiResponse.success("Reading deleted successfully");
    }
}
