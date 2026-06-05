package com.java.Utility.Billing.System.service;

import com.java.Utility.Billing.System.dto.response.EmailConfigResponse;
import com.java.Utility.Billing.System.dto.response.EmailTestResponse;
import com.java.Utility.Billing.System.enums.RoleName;
import com.java.Utility.Billing.System.exception.BadRequestException;
import com.java.Utility.Billing.System.util.RoleDescriptionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Sends all system emails via Gmail SMTP.
 * Toggle with app.mail.enabled; credentials live in application-local.properties.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.system.login-url:http://localhost:8081/swagger-ui.html}")
    private String loginUrl;

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String smtpHost;

    @Value("${spring.mail.port:587}")
    private int smtpPort;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    @Value("${spring.mail.password:}")
    private String smtpPassword;

    /**
     * Returns current mail settings for admin diagnostics (password is never exposed).
     */
    public EmailConfigResponse getMailConfigStatus() {
        boolean passwordConfigured = StringUtils.hasText(smtpPassword);
        String hint;
        if (!mailEnabled) {
            hint = "Set app.mail.enabled=true in application-local.properties and restart.";
        } else if (!passwordConfigured) {
            hint = "Add spring.mail.password (Gmail App Password, no spaces) to application-local.properties and restart.";
        } else {
            hint = "Config looks ready. Call POST /api/system/email/test to verify SMTP login.";
        }

        return EmailConfigResponse.builder()
                .mailEnabled(mailEnabled)
                .smtpHost(smtpHost)
                .smtpPort(smtpPort)
                .smtpUsername(smtpUsername)
                .fromAddress(fromEmail)
                .passwordConfigured(passwordConfigured)
                .configSource("application-local.properties (loaded last — overrides application.properties)")
                .setupHint(hint)
                .build();
    }

    /**
     * Sends a test message to confirm Gmail SMTP authentication works.
     */
    public EmailTestResponse sendTestEmail(String to) {
        validateMailReady();

        String body = """
                Utility Billing System - Email Test

                If you received this message, Gmail SMTP is configured correctly.

                Sent at: %s
                From: %s
                SMTP: %s:%d

                You can now test:
                - POST /api/users (credentials email)
                - POST /api/users/{id}/resend-credentials
                - POST /api/auth/forgot-password (OTP email)
                """.formatted(LocalDateTime.now(), fromEmail, smtpHost, smtpPort);

        boolean sent = send(to, "Utility Billing System - Email Test", body, null);
        if (!sent) {
            throw new BadRequestException(buildSmtpFailureMessage());
        }

        return EmailTestResponse.builder()
                .sent(true)
                .mailEnabled(mailEnabled)
                .from(fromEmail)
                .to(to)
                .smtpHost(smtpHost)
                .smtpPort(smtpPort)
                .message("Test email sent successfully to " + to + ". Check inbox and spam folder.")
                .build();
    }

    /** Welcome message after self-registration. */
    public void sendWelcomeEmail(String to, String name) {
        send(to, "Welcome to Utility Billing System",
                "Dear " + name + ",\n\nWelcome to the WASAC/REG Utility Billing System. " +
                        "Please verify your email to activate your account.\n\nRegards,\nWASAC/REG Team",
                null);
    }

    /** OTP email for POST /api/auth/verify-email. */
    public void sendVerificationEmail(String to, String name, String otp) {
        send(to, "Verify Your Email",
                "Dear " + name + ",\n\nYour email verification OTP is: " + otp +
                        "\n\nThis OTP expires in 10 minutes.\n\nRegards,\nWASAC/REG Team",
                null);
    }

    /** OTP email for POST /api/auth/reset-password. */
    public void sendPasswordResetEmail(String to, String name, String otp) {
        send(to, "Password Reset Request",
                "Dear " + name + ",\n\nYour password reset OTP is: " + otp +
                        "\n\nThis OTP expires in 10 minutes.\n\nRegards,\nWASAC/REG Team",
                null);
    }

    /**
     * Credentials email when admin creates Finance/Operator via POST /api/users.
     * Returns false if mail is disabled or SMTP fails (caller may return password in API response).
     */
    public boolean sendAccountCredentialsEmail(String to, String name, String temporaryPassword,
                                               Set<RoleName> roles) {
        String roleDisplay = RoleDescriptionHelper.formatRolesForDisplay(roles);
        String responsibilities = RoleDescriptionHelper.describeRoles(roles);

        String body = """
                Dear %s,

                Your WASAC/REG Utility Billing System account has been created by an administrator.

                ACCOUNT DETAILS
                ---------------
                Username / Email: %s
                Temporary Password: %s
                Assigned Role(s): %s

                HOW TO ACCESS THE SYSTEM
                ------------------------
                1. Open the system login page: %s
                2. Sign in using your email and the temporary password above.
                3. You will be required to change your password immediately after your first login.
                4. Use Postman or Swagger UI to call POST /api/auth/change-password with your new password.

                YOUR RESPONSIBILITIES
                ---------------------
                %s

                For security, do not share your credentials. Change your temporary password on first login.

                Regards,
                WASAC/REG Utility Billing Team
                """.formatted(name, to, temporaryPassword, roleDisplay, loginUrl, responsibilities);

        return send(to, "Your Utility Billing System Account Credentials", body, temporaryPassword);
    }

    public boolean isMailEnabled() {
        return mailEnabled;
    }

    /** Notifies user when admin changes their role. */
    public void sendRoleChangeEmail(String to, String name, Set<RoleName> newRoles) {
        String roleDisplay = RoleDescriptionHelper.formatRolesForDisplay(newRoles);
        String responsibilities = RoleDescriptionHelper.describeRoles(newRoles);

        String body = """
                Dear %s,

                Your role assignment on the WASAC/REG Utility Billing System has been updated.

                NEW ROLE(S): %s

                ASSOCIATED RESPONSIBILITIES
                ---------------------------
                %s

                Please sign in at %s to continue performing your assigned duties.
                If you did not expect this change, contact your system administrator immediately.

                Regards,
                WASAC/REG Utility Billing Team
                """.formatted(name, roleDisplay, responsibilities, loginUrl);

        send(to, "Your System Role Has Been Updated", body, null);
    }

    /**
     * Email to customer when a bill is generated (POST /api/bills/generate).
     * Exam format: "Dear &lt;CustomerName&gt;, Your &lt;Month/Year&gt; utility bill of &lt;Amount&gt; FRW..."
     */
    public boolean sendBillGeneratedToCustomer(String customerEmail, String customerName,
                                                String monthYear, String totalAmount) {
        String body = """
                Dear %s,
                Your %s utility bill of %s FRW has been successfully processed.
                """.formatted(customerName, monthYear, totalAmount);

        return send(customerEmail, "Utility Bill Generated", body, null);
    }

    /**
     * Email to Finance/Admin staff when a bill is generated for a customer.
     */
    public boolean sendBillProcessedToStaff(String staffEmail, String staffName,
                                             String customerName, String customerEmail,
                                             String billReference, String billingPeriod,
                                             String totalAmount, String generatedBy) {
        String body = """
                Dear %s,

                A utility bill has been processed and is awaiting approval.

                CUSTOMER
                --------
                Name: %s
                Email: %s

                BILL DETAILS
                ------------
                Reference: %s
                Billing Period: %s
                Total Amount: %s FRW
                Generated By: %s
                Status: PENDING APPROVAL

                Please review and approve the bill in Swagger:
                PATCH /api/bills/{id}/approve

                System: %s

                Regards,
                WASAC/REG Utility Billing Team
                """.formatted(staffName, customerName, customerEmail, billReference, billingPeriod,
                totalAmount, generatedBy, loginUrl);

        return send(staffEmail, "Bill Processed - Pending Approval [" + billReference + "]", body, null);
    }

    /**
     * Email to customer when Finance approves the bill — ready for payment.
     */
    public boolean sendBillApprovedToCustomer(String customerEmail, String customerName,
                                               String billReference, String billingPeriod,
                                               String totalAmount, String outstandingBalance) {
        String body = """
                Dear %s,

                Your utility bill has been approved and is ready for payment.

                BILL DETAILS
                ------------
                Reference: %s
                Billing Period: %s
                Total Amount: %s FRW
                Amount Due: %s FRW

                Please make your payment at your earliest convenience.

                System: %s

                Regards,
                WASAC/REG Utility Billing Team
                """.formatted(customerName, billReference, billingPeriod, totalAmount,
                outstandingBalance, loginUrl);

        return send(customerEmail, "Your Utility Bill Is Approved - " + billReference, body, null);
    }

    /** Bill/payment notifications and other alerts. */
    public boolean sendNotificationEmail(String to, String subject, String message) {
        return send(to, subject, message, null);
    }

    private void validateMailReady() {
        if (!mailEnabled) {
            throw new BadRequestException("Mail is disabled. Set app.mail.enabled=true in application-local.properties");
        }
        if (!StringUtils.hasText(smtpUsername)) {
            throw new BadRequestException("spring.mail.username is empty. Set it in application-local.properties");
        }
        if (!StringUtils.hasText(smtpPassword)) {
            throw new BadRequestException(
                    "spring.mail.password is empty. Add your Gmail App Password to application-local.properties");
        }
    }

    /**
     * Core send method. When mail is disabled, logs instead of sending.
     * On SMTP failure, logs the full error chain for debugging.
     */
    private boolean send(String to, String subject, String body, String sensitiveValue) {
        if (!mailEnabled) {
            if (sensitiveValue != null) {
                log.warn("MAIL DISABLED - Credentials for {} | temporary password: {}", to, sensitiveValue);
            } else {
                log.info("Email disabled. Would send to {}: {}", to, subject);
            }
            return false;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        try {
            mailSender.send(message);
            log.info("Email sent to {} | subject: {}", to, subject);
            return true;
        } catch (Exception ex) {
            String errorDetail = extractErrorMessage(ex);
            if (sensitiveValue != null) {
                log.error("Failed to send credentials email to {}. Temporary password (share manually): {} | SMTP: {}",
                        to, sensitiveValue, errorDetail, ex);
            } else {
                log.error("Failed to send email to {} | subject: {} | SMTP: {}", to, subject, errorDetail, ex);
            }
            return false;
        }
    }

    /** Walks the exception cause chain for a readable SMTP error. */
    private String extractErrorMessage(Throwable ex) {
        StringBuilder details = new StringBuilder();
        Throwable current = ex;
        while (current != null) {
            if (StringUtils.hasText(current.getMessage())) {
                if (!details.isEmpty()) {
                    details.append(" -> ");
                }
                details.append(current.getMessage());
            }
            current = current.getCause();
        }
        return details.isEmpty() ? ex.getClass().getSimpleName() : details.toString();
    }

    private String buildSmtpFailureMessage() {
        return "Gmail SMTP failed. Steps: "
                + "1) Generate App Password at https://myaccount.google.com/apppasswords "
                + "2) Put it in application-local.properties as spring.mail.password (no spaces) "
                + "3) Restart the app (password changes are NOT picked up without restart) "
                + "4) Call GET /api/system/email/status to confirm passwordConfigured=true";
    }
}
