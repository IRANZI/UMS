package com.java.Utility.Billing.System.controller;

import com.java.Utility.Billing.System.dto.request.TestEmailRequest;
import com.java.Utility.Billing.System.dto.response.ApiResponse;
import com.java.Utility.Billing.System.dto.response.EmailConfigResponse;
import com.java.Utility.Billing.System.dto.response.EmailTestResponse;
import com.java.Utility.Billing.System.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin tools for verifying system configuration (email SMTP, etc.).
 */
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
@Tag(name = "System", description = "System configuration checks (email SMTP)")
@SecurityRequirement(name = "Bearer Authentication")
public class SystemController {

    private final EmailService emailService;

    /**
     * Check whether Gmail credentials are loaded — run this before sending test email.
     * passwordConfigured must be true after you update application-local.properties and restart.
     */
    @GetMapping("/email/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Check email configuration",
            description = "Shows if mail is enabled and password is loaded. Does not expose the password.")
    public ApiResponse<EmailConfigResponse> getEmailStatus() {
        EmailConfigResponse status = emailService.getMailConfigStatus();
        return ApiResponse.success(status.getSetupHint(), status);
    }

    /**
     * Sends a real test email through Gmail SMTP to confirm authentication works.
     */
    @PostMapping("/email/test")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Send test email",
            description = "Verifies Gmail SMTP. Run GET /api/system/email/status first, then this.")
    public ApiResponse<EmailTestResponse> sendTestEmail(@Valid @RequestBody TestEmailRequest request) {
        EmailTestResponse result = emailService.sendTestEmail(request.getTo());
        return ApiResponse.success(result.getMessage(), result);
    }
}
