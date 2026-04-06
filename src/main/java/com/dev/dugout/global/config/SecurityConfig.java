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
                        // 1. 공통/예외 요청
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 2. [Members] 누구나 접근 가능한 문 (로그인, 회원가입 등)
                        .requestMatchers("/api/v1/members/login", "/api/v1/members/signup", "/api/v1/members/check-id").permitAll()

                        // 3. [Members] 로그인한 사람만 들어올 수 있는 방 (내 정보, 로그아웃, 탈퇴)
                        .requestMatchers("/api/v1/members/me", "/api/v1/members/logout").authenticated()

                        // 4. [Service] 일반 데이터 조회 (더그아웃의 풍부한 야구 데이터들)
                        .requestMatchers(
                                "/api/v1/fanexperience/**", // 팀추천
                                "/api/v1/news/**", // 뉴스
                                "/api/v1/schedule/**", //경기 스케줄
                                "/api/v1/players/**", // 선수 관련
                                "/api/v1/prediction/**", // 선수 성적 예측 관련
                                "/api/v1/fa-market/**", // FA 시장 관련
                                "/api/v1/tickets/teams", // 티켓팅 관련
                                "/api/v1/notices", // 공지사항 관련
                                "/api/v1/ingest/kbo/notify", // 데이터 적재 관련
                                "/api/v1/ingest/ml/gg", // 골든글러브 예측 관련
                                "/api/v1/performance/**",// 팀별 순위 관련
                                "/api/v1/leaderboard/**" // 지표별 top 5 리더보드 관련
                        ).permitAll()

                        // 5. [Private] 대시보드 등 개인화 서비스
                        .requestMatchers("/api/v1/dashboard", "/api/v1/dashboard/**").authenticated()

                        // 6. 나머지는 기본적으로 인증 필요 (안전빵)
                        .anyRequest().authenticated()
                )

                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, customUserDetailsService),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 2. 통합 CORS 설정 (WebConfig에 있던 내용을 여기로 옮김)
    @Bean
    //사용자가 보낸 요청이 허용된 도메인인가?
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 쿠키 인증 시에는 도메인을 명확하게 명시하는 것이 브라우저 호환성에 가장 좋음
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "https://dugout.cloud",
                "https://www.dugout.cloud"
        ));

        // 개발 환경(IDX 등)을 위한 패턴은 유지
        configuration.setAllowedOriginPatterns(List.of(
                "https://*.idx.google.com",
                "https://*.usercontent.goog",
                "https://*.run.app"
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