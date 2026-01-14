package com.dev.dugout.global.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


//요청 가로채기: 헤더에 담긴 JWT 토큰을 확인
//검증: 토큰이 유효하다면 customUserDetailsService를 통해 사용자 정보를 불러옴
//인증 완료: 사용자가 확인되면 "이 사람은 인증된 사용자다"라는 증명서를 SecurityContextHolder라는 곳에 넣어둠
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    //전달받은 loginId로 실제 데이터베이스에서 유저 정보 찾자
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. 헤더에서 토큰 추출
        String bearerToken = request.getHeader("Authorization");

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);

            // 2. 토큰 검증
            if (jwtTokenProvider.validateToken(token)) {
                String loginId = jwtTokenProvider.getLoginId(token);
                log.info("JWT 토큰 검증 성공. 로그인 아이디: {}", loginId); // 인증 시도 로그

                try {
                    // 3. 인증 객체 생성 및 SecurityContext에 저장
                    UserDetails userDetails = userDetailsService.loadUserByUsername(loginId);
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                    //인증 정보 보관함
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("SecurityContext에 '{}' 유저의 인증 정보를 저장했습니다.", loginId); // 상세 디버깅 로그
                } catch (Exception e) {
                    log.error("사용자 정보를 불러오는 중 오류 발생: {}", e.getMessage()); // 예외 발생 시 로그
                }
            } else {
                log.warn("유효하지 않은 JWT 토큰이 감지되었습니다. 요청 URL: {}", request.getRequestURI()); // 경고 로그
            }
        }
        filterChain.doFilter(request, response);
    }
}
