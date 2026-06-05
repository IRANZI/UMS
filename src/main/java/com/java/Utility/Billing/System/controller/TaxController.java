package com.java.Utility.Billing.System.controller;

import com.java.Utility.Billing.System.dto.request.TaxRequest;
import com.java.Utility.Billing.System.dto.response.ApiResponse;
import com.java.Utility.Billing.System.dto.response.PageResponse;
import com.java.Utility.Billing.System.dto.response.TaxResponse;
import com.java.Utility.Billing.System.service.TaxService;
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
@RequestMapping("/api/taxes")
@RequiredArgsConstructor
@Tag(name = "Taxes", description = "Tax configuration APIs")
@SecurityRequirement(name = "Bearer Authentication")
public class TaxController {

    private final TaxService taxService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new tax version")
    public ApiResponse<TaxResponse> create(@Valid @RequestBody TaxRequest request) {
        return ApiResponse.success("Tax created", taxService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(summary = "Get all taxes")
    public ApiResponse<PageResponse<TaxResponse>> getAll(@ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.success(taxService.getAll(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(summary = "Get tax by ID")
    public ApiResponse<TaxResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(taxService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update tax (creates new version)")
    public ApiResponse<TaxResponse> update(@PathVariable Long id, @Valid @RequestBody TaxRequest request) {
        return ApiResponse.success(taxService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate tax")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        taxService.delete(id);
        return ApiResponse.success("Tax deactivated");
    }
}
