package com.personal.jz.module.auth.controller;

import com.personal.jz.common.api.ApiResponse;
import com.personal.jz.common.security.CurrentUser;
import com.personal.jz.module.auth.dto.LoginRequest;
import com.personal.jz.module.auth.dto.LoginResponse;
import com.personal.jz.module.auth.dto.RegisterRequest;
import com.personal.jz.module.auth.dto.UserVO;
import com.personal.jz.module.auth.service.AuthService;
import com.personal.jz.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwt;

    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ApiResponse.ok(authService.register(req));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return ApiResponse.ok(authService.login(req));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@CurrentUser Long uid, HttpServletRequest req) {
        String h = req.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) authService.logout(uid, h.substring(7));
        return ApiResponse.ok();
    }

    @GetMapping("/me")
    public ApiResponse<UserVO> me(@CurrentUser Long uid) {
        return ApiResponse.ok(authService.me(uid));
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@CurrentUser Long uid, @RequestBody Map<String, String> body) {
        authService.changePassword(uid, body.get("oldPassword"), body.get("newPassword"));
        return ApiResponse.ok();
    }

    @PutMapping("/profile")
    public ApiResponse<Void> updateProfile(@CurrentUser Long uid, @RequestBody Map<String, String> body) {
        authService.updateProfile(uid, body.get("nickname"));
        return ApiResponse.ok();
    }
}
