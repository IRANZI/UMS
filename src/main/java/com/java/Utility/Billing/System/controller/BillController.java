package com.java.Utility.Billing.System.controller;

import com.java.Utility.Billing.System.dto.request.BillGenerationRequest;
import com.java.Utility.Billing.System.dto.response.ApiResponse;
import com.java.Utility.Billing.System.dto.response.BillResponse;
import com.java.Utility.Billing.System.dto.response.PageResponse;
import com.java.Utility.Billing.System.service.BillService;
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
@RequestMapping("/api/bills")
@RequiredArgsConstructor
@Tag(name = "Bills", description = "Bill generation and approval APIs")
@SecurityRequirement(name = "Bearer Authentication")
public class BillController {

    private final BillService billService;

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(summary = "Generate monthly bill for a customer")
    public ApiResponse<BillResponse> generate(@Valid @RequestBody BillGenerationRequest request) {
        return ApiResponse.success("Bill generated", billService.generateBill(request));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(summary = "Approve a pending bill")
    public ApiResponse<BillResponse> approve(@PathVariable Long id) {
        return ApiResponse.success("Bill approved", billService.approveBill(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE', 'CUSTOMER')")
    @Operation(summary = "Get all bills")
    public ApiResponse<PageResponse<BillResponse>> getAll(@ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.success(billService.getAll(pageable));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(summary = "Search bills")
    public ApiResponse<PageResponse<BillResponse>> search(
            @RequestParam String query, @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.success(billService.search(query, pageable));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE', 'CUSTOMER')")
    @Operation(summary = "Get bills by customer")
    public ApiResponse<PageResponse<BillResponse>> getByCustomer(
            @PathVariable Long customerId, @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.success(billService.getByCustomer(customerId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE', 'CUSTOMER')")
    @Operation(summary = "Get bill by ID")
    public ApiResponse<BillResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(billService.getById(id));
    }

    @GetMapping("/reference/{reference}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE', 'CUSTOMER')")
    @Operation(summary = "Get bill by reference")
    public ApiResponse<BillResponse> getByReference(@PathVariable String reference) {
        return ApiResponse.success(billService.getByReference(reference));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete bill")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        billService.delete(id);
        return ApiResponse.success("Bill deleted");
    }
}
