package com.java.Utility.Billing.System.repository;

import com.java.Utility.Billing.System.entity.User;
import com.java.Utility.Billing.System.enums.RoleName;
import com.java.Utility.Billing.System.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);

    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.fullNames) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "u.phoneNumber LIKE CONCAT('%', :search, '%')")
    Page<User> search(@Param("search") String search, Pageable pageable);

    Page<User> findByStatus(UserStatus status, Pageable pageable);

    /** Active Finance and Admin users who receive bill-processing alerts. */
    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r " +
           "WHERE r.name IN :roles AND u.status = com.java.Utility.Billing.System.enums.UserStatus.ACTIVE")
    List<User> findActiveUsersByRoles(@Param("roles") List<RoleName> roles);
}
