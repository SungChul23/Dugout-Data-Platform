package com.dev.dugout.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. 시큐리티 필터 체인에서 CORS 활성화
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // OPTIONS 요청(Preflight)은 인증 없이 모두 허용
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/v1/members/**").permitAll()
                        .requestMatchers("/api/v1/fanexperience/**").permitAll()
                        .requestMatchers("/api/v1/news/**").permitAll()
                        .requestMatchers("/api/v1/schedule/**").permitAll()
                        .requestMatchers("/api/v1/dashboard/**").authenticated()
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    // 2. 통합 CORS 설정 (WebConfig에 있던 내용을 여기로 옮김)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 기존 WebConfig에 있던 도메인 + 실서비스 도메인 통합
        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:3000",
                "https://*.idx.google.com",
                "https://*.google.com",
                "https://*.usercontent.goog",
                "https://dugout.cloud"
        ));

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true); //프론트 fetch OR axios에서 반드시 사용
        configuration.setMaxAge(3600L); // 브라우저가 CORS 결과를 1시간 동안 캐싱하도록 설정

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}