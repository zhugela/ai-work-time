package com.personal.jz.security;

import com.personal.jz.common.api.ApiResponse;
import com.personal.jz.common.exception.BizException;
import com.personal.jz.common.exception.ErrorCodeEnums;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redis; // 可选注入；失败时降级

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String path = req.getRequestURI();
        if (isWhiteListed(path)) {
            chain.doFilter(req, res);
            return;
        }
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            // 不抛错；交给 SecurityConfig 决定是否允许匿名访问
            chain.doFilter(req, res);
            return;
        }
        String token = header.substring(7);
        try {
            Claims claims = jwtTokenProvider.parse(token);
            // 吊销校验
            if (redis != null) {
                Boolean revoked = redis.hasKey("auth:revoked:" + token);
                if (Boolean.TRUE.equals(revoked)) {
                    writeErr(res, ErrorCodeEnums.TOKEN_REVOKED);
                    return;
                }
            }
            Long uid = claims.get("uid", Long.class);
            String role = claims.get("role", String.class);
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    uid, null, List.of(new SimpleGrantedAuthority("ROLE_" + (role == null ? "USER" : role)))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
            chain.doFilter(req, res);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            writeErr(res, ErrorCodeEnums.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            writeErr(res, ErrorCodeEnums.TOKEN_INVALID);
        }
    }

    private boolean isWhiteListed(String path) {
        return path.startsWith("/api/auth/") || path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui")
                || path.startsWith("/actuator") || path.equals("/error") || path.startsWith("/h2-console");
    }

    private void writeErr(HttpServletResponse res, ErrorCodeEnums e) throws IOException {
        res.setStatus(HttpStatus.UNAUTHORIZED.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.getWriter().write("{\"code\":" + e.getCode() + ",\"msg\":\"" + e.getMsg() + "\"}");
    }
}
