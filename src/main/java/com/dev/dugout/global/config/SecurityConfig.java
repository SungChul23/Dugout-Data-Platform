package com.dev.dugout.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

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
                .csrf(csrf -> csrf.disable()) // 로컬 테스트용
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/members/**").permitAll()
                        .requestMatchers("/api/v1/fanexperience/**").permitAll()
                        .requestMatchers("/api/v1/news/**").permitAll()
                        .anyRequest().authenticated()
                );
        return http.build();
    }
}