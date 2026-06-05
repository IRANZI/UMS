package com.java.Utility.Billing.System.controller;

import com.java.Utility.Billing.System.dto.request.TariffRequest;
import com.java.Utility.Billing.System.dto.response.ApiResponse;
import com.java.Utility.Billing.System.dto.response.PageResponse;
import com.java.Utility.Billing.System.dto.response.TariffResponse;
import com.java.Utility.Billing.System.service.TariffService;
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
@RequestMapping("/api/tariffs")
@RequiredArgsConstructor
@Tag(name = "Tariffs", description = "Tariff configuration APIs")
@SecurityRequirement(name = "Bearer Authentication")
public class TariffController {

    private final TariffService tariffService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new tariff version")
    public ApiResponse<TariffResponse> create(@Valid @RequestBody TariffRequest request) {
        return ApiResponse.success("Tariff created", tariffService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(summary = "Get all tariffs")
    public ApiResponse<PageResponse<TariffResponse>> getAll(@ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.success(tariffService.getAll(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(summary = "Get tariff by ID")
    public ApiResponse<TariffResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(tariffService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update tariff (creates new version)")
    public ApiResponse<TariffResponse> update(@PathVariable Long id, @Valid @RequestBody TariffRequest request) {
        return ApiResponse.success(tariffService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate tariff")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        tariffService.delete(id);
        return ApiResponse.success("Tariff deactivated");
    }
}
