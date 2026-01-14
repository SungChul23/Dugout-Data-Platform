package com.dev.dugout.global.config;

import com.dev.dugout.domain.user.service.CustomUserDetailsService;
import com.dev.dugout.global.jwt.JwtAuthenticationFilter;
import com.dev.dugout.global.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http

                // 1. 시큐리티 필터 체인에서 CORS 활성화
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())

                // 세션 관리 정책을 STATELESS로 설정 (JWT 사용 필수)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                //요청한 URL에 들어갈 자격이 있나요?
                .authorizeHttpRequests(auth -> auth
                        // OPTIONS 요청(Preflight)은 인증 없이 모두 허용
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/v1/members/**").permitAll()
                        .requestMatchers("/api/v1/fanexperience/**").permitAll()
                        .requestMatchers("/api/v1/news/**").permitAll()
                        .requestMatchers("/api/v1/schedule/**").permitAll()
                        .requestMatchers("/api/v1/dashboard", "/api/v1/dashboard/**").authenticated()
                        .anyRequest().authenticated()
                )
                //JWT 인증 필터를 UsernamePasswordAuthenticationFilter보다 먼저 실행하도록 등록
                //전통 아이디 및 비번으로 로그인하는 방법 대신에 내가 만든 JWT 필터로 먼저 인증을 끝내자
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, customUserDetailsService),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 2. 통합 CORS 설정 (WebConfig에 있던 내용을 여기로 옮김)
    @Bean
    //사용자가 보낸 요청이 허용된 도메인인가?
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