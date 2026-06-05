package com.java.Utility.Billing.System.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Logs mail configuration at startup so you can confirm credentials loaded correctly.
 * Never logs the actual password.
 */
@Component
@Slf4j
public class MailStartupLogger {

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${spring.mail.host:}")
    private String host;

    @Value("${spring.mail.port:0}")
    private int port;

    @Value("${spring.mail.username:}")
    private String username;

    @Value("${spring.mail.password:}")
    private String password;

    @Value("${app.mail.from:}")
    private String from;

    @EventListener(ApplicationReadyEvent.class)
    public void logMailStatus() {
        if (!mailEnabled) {
            log.warn("Email is DISABLED (app.mail.enabled=false). Credential emails will not be sent.");
            return;
        }

        boolean passwordSet = StringUtils.hasText(password);
        log.info("Email ready: smtp={}:{} | user={} | from={} | passwordConfigured={}",
                host, port, username, from, passwordSet);

        if (!passwordSet) {
            log.warn("spring.mail.password is missing. Edit application-local.properties and RESTART.");
        }
    }
}
