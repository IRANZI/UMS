package com.java.Utility.Billing.System.dto.response;

import com.java.Utility.Billing.System.enums.RoleName;
import com.java.Utility.Billing.System.enums.UserStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String fullNames;
    private String email;
    private String phoneNumber;
    private UserStatus status;
    private boolean emailVerified;
    private boolean mustChangePassword;
    private String profilePicturePath;
    private Set<RoleName> roles;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
