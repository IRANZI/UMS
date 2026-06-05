package com.java.Utility.Billing.System.dto.request;

import com.java.Utility.Billing.System.enums.RoleName;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

@Data
public class RoleAssignRequest {

    @NotEmpty(message = "At least one role is required")
    private Set<RoleName> roles;
}
