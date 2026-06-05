package com.java.Utility.Billing.System.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmailConfigResponse {
    private boolean mailEnabled;
    private String smtpHost;
    private int smtpPort;
    private String smtpUsername;
    private String fromAddress;
    private boolean passwordConfigured;
    private String configSource;
    private String setupHint;
}
