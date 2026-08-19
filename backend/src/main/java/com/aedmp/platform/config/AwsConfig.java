package com.aedmp.platform.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@ConditionalOnProperty(name = "aedmp.storage.mode", havingValue = "s3")
public class AwsConfig {

    @Bean
    public S3Client s3Client() {
        return S3Client.builder().build();
    }
}
