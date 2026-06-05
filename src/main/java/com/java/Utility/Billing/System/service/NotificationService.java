package com.java.Utility.Billing.System.service;

import com.java.Utility.Billing.System.dto.response.NotificationResponse;
import com.java.Utility.Billing.System.dto.response.PageResponse;
import com.java.Utility.Billing.System.entity.Customer;
import com.java.Utility.Billing.System.entity.Notification;
import com.java.Utility.Billing.System.enums.NotificationType;
import com.java.Utility.Billing.System.exception.ResourceNotFoundException;
import com.java.Utility.Billing.System.mapper.EntityMapper;
import com.java.Utility.Billing.System.repository.NotificationRepository;
import com.java.Utility.Billing.System.util.PageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void createBillNotification(Customer customer, String monthYear, BigDecimal amount) {
        String message = String.format(
                "Dear %s,\nYour %s utility bill of %s FRW has been successfully processed.",
                customer.getFullNames(), monthYear, amount);
        Notification notification = Notification.builder()
                .customer(customer)
                .message(message)
                .type(NotificationType.BILL_GENERATED)
                .read(false)
                .build();
        notificationRepository.save(notification);
    }

    @Transactional
    public void createPaymentNotification(Customer customer, String monthYear, BigDecimal amount) {
        String message = String.format(
                "Dear %s,\nYour %s utility bill of %s FRW has been fully paid. Thank you!",
                customer.getFullNames(), monthYear, amount);
        Notification notification = Notification.builder()
                .customer(customer)
                .message(message)
                .type(NotificationType.PAYMENT_RECEIVED)
                .read(false)
                .build();
        notificationRepository.save(notification);
    }

    public PageResponse<NotificationResponse> getByCustomer(Long customerId, Pageable pageable) {
        Page<NotificationResponse> page = notificationRepository.findByCustomerId(customerId, pageable)
                .map(EntityMapper::toNotificationResponse);
        return PageMapper.toPageResponse(page);
    }

    @Transactional
    public NotificationResponse markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notification.setRead(true);
        notification = notificationRepository.save(notification);
        return EntityMapper.toNotificationResponse(notification);
    }

    @Transactional
    public void delete(Long id) {
        notificationRepository.deleteById(id);
    }
}
