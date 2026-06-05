package com.java.Utility.Billing.System.repository;

import com.java.Utility.Billing.System.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByUserId(Long userId);
    List<Document> findByCustomerId(Long customerId);
}
