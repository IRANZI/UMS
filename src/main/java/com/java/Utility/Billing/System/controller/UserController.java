package com.java.Utility.Billing.System.controller;

import com.java.Utility.Billing.System.dto.request.AdminCreateUserRequest;
import com.java.Utility.Billing.System.dto.request.RoleAssignRequest;
import com.java.Utility.Billing.System.dto.request.UserUpdateRequest;
import com.java.Utility.Billing.System.dto.response.ApiResponse;
import com.java.Utility.Billing.System.dto.response.CredentialsDeliveryResponse;
import com.java.Utility.Billing.System.dto.response.PageResponse;
import com.java.Utility.Billing.System.dto.response.UserResponse;
import com.java.Utility.Billing.System.enums.UserStatus;
import com.java.Utility.Billing.System.service.UserService;
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
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management APIs")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create Operator/Finance user",
            description = "Admin-created users are pre-verified. A credentials email (temporary password) is sent — not an OTP verification email.")
    public ApiResponse<UserResponse> create(@Valid @RequestBody AdminCreateUserRequest request) {
        return ApiResponse.success(
                "User created. A credentials email with the temporary password has been sent (check spam if not received).",
                userService.createByAdmin(request));
    }

    @PostMapping("/{id}/resend-credentials")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Resend login credentials",
            description = "Generates a new temporary password. Returns it in the response if email delivery fails.")
    public ApiResponse<CredentialsDeliveryResponse> resendCredentials(@PathVariable Long id) {
        CredentialsDeliveryResponse result = userService.resendCredentials(id);
        return ApiResponse.success(result.getDeliveryNote(), result);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all users with pagination")
    public ApiResponse<PageResponse<UserResponse>> getAll(
            @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(userService.getAll(pageable));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Search users")
    public ApiResponse<PageResponse<UserResponse>> search(
            @RequestParam String query,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.success(userService.search(query, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(summary = "Get user by ID")
    public ApiResponse<UserResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(userService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user (sends role change email if roles are updated)")
    public ApiResponse<UserResponse> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.success(userService.update(id, request));
    }

    @PatchMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign or update user roles (sends notification email)")
    public ApiResponse<UserResponse> assignRoles(@PathVariable Long id,
                                                 @Valid @RequestBody RoleAssignRequest request) {
        return ApiResponse.success(
                "Roles updated. Notification email sent to the user.",
                userService.assignRoles(id, request.getRoles()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete user")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.success("User deleted successfully");
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Filter users by status")
    public ApiResponse<PageResponse<UserResponse>> getByStatus(
            @PathVariable UserStatus status,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.success(userService.getByStatus(status, pageable));
    }
}
