package com.java.Utility.Billing.System.controller;

import com.java.Utility.Billing.System.dto.request.MeterRequest;
import com.java.Utility.Billing.System.dto.response.ApiResponse;
import com.java.Utility.Billing.System.dto.response.MeterResponse;
import com.java.Utility.Billing.System.dto.response.PageResponse;
import com.java.Utility.Billing.System.service.MeterService;
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
@RequestMapping("/api/meters")
@RequiredArgsConstructor
@Tag(name = "Meters", description = "Meter management APIs")
@SecurityRequirement(name = "Bearer Authentication")
public class MeterController {

    private final MeterService meterService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Create a new meter")
    public ApiResponse<MeterResponse> create(@Valid @RequestBody MeterRequest request) {
        return ApiResponse.success("Meter created", meterService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'FINANCE')")
    @Operation(summary = "Get all meters")
    public ApiResponse<PageResponse<MeterResponse>> getAll(@ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.success(meterService.getAll(pageable));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'FINANCE')")
    @Operation(summary = "Search meters")
    public ApiResponse<PageResponse<MeterResponse>> search(
            @RequestParam String query, @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.success(meterService.search(query, pageable));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'FINANCE', 'CUSTOMER')")
    @Operation(summary = "Get meters by customer")
    public ApiResponse<PageResponse<MeterResponse>> getByCustomer(
            @PathVariable Long customerId, @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.success(meterService.getByCustomer(customerId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'FINANCE', 'CUSTOMER')")
    @Operation(summary = "Get meter by ID")
    public ApiResponse<MeterResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(meterService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Update meter")
    public ApiResponse<MeterResponse> update(@PathVariable Long id, @Valid @RequestBody MeterRequest request) {
        return ApiResponse.success(meterService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete meter")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        meterService.delete(id);
        return ApiResponse.success("Meter deleted successfully");
    }
}
