package com.dev.dugout.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

@Configuration
public class BedrockConfig {
    @Bean
    public BedrockRuntimeClient bedrockRuntimeClient() {
        return BedrockRuntimeClient.builder()
                .region(Region.AP_NORTHEAST_2) // 서울 리전
                .build();
    }
}