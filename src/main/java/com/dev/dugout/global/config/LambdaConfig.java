package com.dev.dugout.global.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;

@Configuration
public class LambdaConfig {

    @Bean
    public LambdaClient lambdaClient() {
        return LambdaClient.builder()
                .region(Region.AP_NORTHEAST_2)
                // 별도의 인증 설정이 없으면 DefaultCredentialsProvider가
                // EC2의 IAM Role을 자동으로 인식
                .build();
    }
}
