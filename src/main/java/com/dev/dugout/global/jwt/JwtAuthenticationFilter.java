package com.dev.dugout.global.jwt;

import jakarta.servlet.http.Cookie;
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

        // 헤더 대신 쿠키에서 토큰을 가져옵니다.
        String token = resolveTokenFromCookie(request);

        //  token이 존재할 때의 로직 (Bearer 체크 없이 바로 검증)
        if (token != null && jwtTokenProvider.validateToken(token)) {
            String loginId = jwtTokenProvider.getLoginId(token);
            log.info("JWT 쿠키 검증 성공. 로그인 아이디: {}", loginId);

            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(loginId);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("SecurityContext에 '{}' 유저의 인증 정보를 저장했습니다.", loginId);
            } catch (Exception e) {
                log.error("사용자 정보를 불러오는 중 오류 발생: {}", e.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }

    // 쿠키에서 accessToken이라는 이름을 가진 값을 찾는 헬퍼 메서드
    private String resolveTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) { // 우리가 정한 쿠키 이름
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
