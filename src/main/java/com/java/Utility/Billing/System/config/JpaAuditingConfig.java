package com.java.Utility.Billing.System.config;

import com.java.Utility.Billing.System.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@RequiredArgsConstructor
public class JpaAuditingConfig {

    private final SecurityUtils securityUtils;

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.of(securityUtils.getCurrentUserEmail());
    }
}
