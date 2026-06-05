package com.java.Utility.Billing.System.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmailTestResponse {
    private boolean sent;
    private boolean mailEnabled;
    private String from;
    private String to;
    private String smtpHost;
    private int smtpPort;
    private String message;
}
