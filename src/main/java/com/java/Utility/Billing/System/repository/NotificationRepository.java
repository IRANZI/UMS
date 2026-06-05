package com.java.Utility.Billing.System.repository;

import com.java.Utility.Billing.System.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByCustomerId(Long customerId, Pageable pageable);
    Page<Notification> findByCustomerIdAndRead(Long customerId, boolean read, Pageable pageable);
}
