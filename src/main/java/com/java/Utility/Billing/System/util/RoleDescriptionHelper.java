package com.java.Utility.Billing.System.util;

import com.java.Utility.Billing.System.enums.RoleName;

import java.util.Set;
import java.util.stream.Collectors;

public final class RoleDescriptionHelper {

    private RoleDescriptionHelper() {}

    public static String describeRole(RoleName role) {
        return switch (role) {
            case ROLE_ADMIN -> "System Administrator – configure tariffs, manage users, approve bills, and oversee all system operations.";
            case ROLE_OPERATOR -> "Operator – capture and manage meter readings for water and electricity meters.";
            case ROLE_FINANCE -> "Finance Officer – approve bills, record payments, and manage financial transactions.";
            case ROLE_CUSTOMER -> "Customer – view utility bills, payment history, and account notifications.";
        };
    }

    public static String describeRoles(Set<RoleName> roles) {
        return roles.stream()
                .map(role -> "- " + role.name().replace("ROLE_", "") + ": " + describeRole(role))
                .collect(Collectors.joining("\n"));
    }

    public static String formatRolesForDisplay(Set<RoleName> roles) {
        return roles.stream()
                .map(r -> r.name().replace("ROLE_", ""))
                .collect(Collectors.joining(", "));
    }
}
