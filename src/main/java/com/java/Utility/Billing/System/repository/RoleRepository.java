package com.java.Utility.Billing.System.repository;

import com.java.Utility.Billing.System.entity.Role;
import com.java.Utility.Billing.System.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
