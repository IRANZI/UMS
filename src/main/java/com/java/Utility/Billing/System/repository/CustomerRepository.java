package com.java.Utility.Billing.System.repository;

import com.java.Utility.Billing.System.entity.Customer;
import com.java.Utility.Billing.System.enums.CustomerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    boolean existsByNationalId(String nationalId);
    boolean existsByEmail(String email);
    Optional<Customer> findByNationalId(String nationalId);
    Optional<Customer> findByEmail(String email);
    Optional<Customer> findByUserId(Long userId);

    @Query("SELECT c FROM Customer c WHERE " +
           "LOWER(c.fullNames) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "c.nationalId LIKE CONCAT('%', :search, '%') OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "c.phoneNumber LIKE CONCAT('%', :search, '%')")
    Page<Customer> search(@Param("search") String search, Pageable pageable);

    Page<Customer> findByStatus(CustomerStatus status, Pageable pageable);
}
