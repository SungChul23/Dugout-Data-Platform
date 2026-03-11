package com.dev.dugout.global.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final Key key;
    private final long ACCESS_TOKEN_EXPIRE_TIME = 1000 * 60 * 60; // 1시간
    private final long REFRESH_TOKEN_EXPIRE_TIME = 1000 * 60 * 60 * 24 * 14; // 14일

    //GitHub Actions에서 비밀키 주입
    public JwtTokenProvider(@Value("${jwt.secret}") String secretKey) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createAccessToken(String loginId) {
        return createToken(loginId, ACCESS_TOKEN_EXPIRE_TIME);
    }

    public String createRefreshToken(String loginId) {
        return createToken(loginId, REFRESH_TOKEN_EXPIRE_TIME);
    }

    private String createToken(String loginId, long expireTime) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(loginId)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expireTime))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getLoginId(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }


    // Access Token을 쿠키에 담아 반환
    public ResponseCookie createAccessTokenCookie(String loginId) {
        String token = createAccessToken(loginId); // 기존 로직 활용
        return ResponseCookie.from("accessToken", token)
                .httpOnly(true)
                .secure(true) // HTTPS 배포 환경
                .path("/")
                .maxAge(ACCESS_TOKEN_EXPIRE_TIME / 1000) // 초 단위 변환
                .sameSite("Lax")
                .build();
    }

    //Refresh Token을 쿠키에 담아 반환
    public ResponseCookie createRefreshTokenCookie(String loginId) {
        String token = createRefreshToken(loginId); // 기존 로직 활용
        return ResponseCookie.from("refreshToken", token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(REFRESH_TOKEN_EXPIRE_TIME / 1000)
                .sameSite("Lax")
                .build();
    }

    //로그아웃 시 쿠키를 제거하기 위한 빈 쿠키 생성
    public ResponseCookie createEmptyCookie(String cookieName) {
        return ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0) // 즉시 만료
                .build();
    }
}