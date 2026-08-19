package com.aedmp.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aedmp.security.jwt")
public record SecurityProperties(String secret, long expirationMinutes, String issuer) {
}
