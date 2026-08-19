package com.aedmp.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aedmp.storage")
public record StorageProperties(String mode, String localPath, String s3Bucket) {
}
