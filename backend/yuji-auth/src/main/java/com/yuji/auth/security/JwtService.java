package com.yuji.auth.security;

import com.yuji.common.security.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

/**
 * JWT 签发与解析（HMAC-SHA256）。
 */
@Service
public class JwtService {

    @Value("${yuji.jwt.secret}")
    private String secret;

    @Value("${yuji.jwt.expire-days:7}")
    private int expireDays;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String issue(Long userId, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(now)
                .expiration(Date.from(now.toInstant().plus(Duration.ofDays(expireDays))))
                .signWith(key())
                .compact();
    }

    /**
     * @throws io.jsonwebtoken.JwtException 过期或签名无效时抛出，由过滤器捕获
     */
    public LoginUser parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new LoginUser(Long.valueOf(claims.getSubject()), claims.get("role", String.class));
    }
}
