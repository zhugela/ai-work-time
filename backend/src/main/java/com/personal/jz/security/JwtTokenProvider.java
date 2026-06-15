package com.personal.jz.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Component
public class JwtTokenProvider {

    @Value("${jz.jwt.secret}")
    private String secret;

    @Value("${jz.jwt.expire-seconds}")
    private long expireSeconds;

    @Value("${jz.jwt.issuer}")
    private String issuer;

    private SecretKey key;

    @PostConstruct
    void init() {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(bytes, 0, padded, 0, bytes.length);
            bytes = padded;
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    public String issue(Long userId, String role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .issuer(issuer)
                .subject(String.valueOf(userId))
                .claims(Map.of("uid", userId, "role", role))
                .issuedAt(new Date(now))
                .expiration(new Date(now + expireSeconds * 1000L))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public long getExpireSeconds() { return expireSeconds; }
}
