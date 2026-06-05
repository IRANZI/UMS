package com.java.Utility.Billing.System.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DocumentResponse {
    private Long id;
    private String fileName;
    private String originalFileName;
    private String contentType;
    private long fileSize;
    private Long userId;
    private Long customerId;
    private LocalDateTime createdAt;
}
