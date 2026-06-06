package com.dev.dugout.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("더그아웃 (Dugout) API")
                        .description("""
                                KBO 야구 데이터 인사이트 플랫폼 백엔드 API

                                **인증 방식**
                                - 사용자 엔드포인트: 로그인·회원가입 후 발급된 `accessToken` / `refreshToken` 쿠키 사용
                                - Lambda 내부 엔드포인트(`/api/v1/ingest/**`): `Authorization: Bearer <secret>` 헤더 사용

                                **운영 환경**: `--spring.profiles.active=prod` 적용 시 이 문서는 비활성화됩니다.
                                """)
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("더그아웃 개발팀")
                                .email("kimsam0923@gmail.com")))
                .servers(List.of(
                        new Server().url("/").description("현재 서버")))
                .addSecurityItem(new SecurityRequirement().addList("LambdaSecret"))
                .components(new Components()
                        .addSecuritySchemes("LambdaSecret",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("SecretKey")
                                        .description("AWS Lambda 전용 Bearer 시크릿 키 (`aws.lambda.secret-key`)")));
    }
}
