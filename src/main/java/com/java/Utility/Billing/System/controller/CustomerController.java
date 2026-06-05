package com.java.Utility.Billing.System.controller;

import com.java.Utility.Billing.System.dto.request.CustomerRequest;
import com.java.Utility.Billing.System.dto.response.ApiResponse;
import com.java.Utility.Billing.System.dto.response.CustomerResponse;
import com.java.Utility.Billing.System.dto.response.PageResponse;
import com.java.Utility.Billing.System.enums.CustomerStatus;
import com.java.Utility.Billing.System.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "Customers", description = "Customer management: fullNames, nationalId (unique), email, phoneNumber, address, status (ACTIVE/INACTIVE)")
@SecurityRequirement(name = "Bearer Authentication")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(summary = "Create customer",
            description = "Stores: fullNames, nationalId (unique), email, phoneNumber, address, status (ACTIVE/INACTIVE). Prevents duplicate National ID.")
    public ApiResponse<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
        return ApiResponse.success("Customer created successfully", customerService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE', 'OPERATOR')")
    @Operation(summary = "Get all customers (paginated)")
    public ApiResponse<PageResponse<CustomerResponse>> getAll(
            @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(customerService.getAll(pageable));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE', 'OPERATOR')")
    @Operation(summary = "Get customers by status", description = "Filter by ACTIVE or INACTIVE")
    public ApiResponse<PageResponse<CustomerResponse>> getByStatus(
            @PathVariable CustomerStatus status,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.success(customerService.getByStatus(status, pageable));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE', 'OPERATOR')")
    @Operation(summary = "Search customers by name, nationalId, email, or phone")
    public ApiResponse<PageResponse<CustomerResponse>> search(
            @RequestParam String query,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.success(customerService.search(query, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE', 'OPERATOR', 'CUSTOMER')")
    @Operation(summary = "Get customer by ID")
    public ApiResponse<CustomerResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(customerService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(summary = "Update customer details")
    public ApiResponse<CustomerResponse> update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return ApiResponse.success("Customer updated successfully", customerService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(summary = "Update customer status only", description = "Set ACTIVE or INACTIVE. Inactive customers cannot receive bills.")
    public ApiResponse<CustomerResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam CustomerStatus status) {
        return ApiResponse.success("Customer status updated", customerService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete customer")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return ApiResponse.success("Customer deleted successfully");
    }
}
