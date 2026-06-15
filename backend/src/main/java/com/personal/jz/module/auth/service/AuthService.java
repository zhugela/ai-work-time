package com.personal.jz.module.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.personal.jz.common.exception.BizException;
import com.personal.jz.common.exception.ErrorCodeEnums;
import com.personal.jz.entity.AuthToken;
import com.personal.jz.entity.Book;
import com.personal.jz.entity.SysUser;
import com.personal.jz.module.auth.dto.LoginRequest;
import com.personal.jz.module.auth.dto.LoginResponse;
import com.personal.jz.module.auth.dto.RegisterRequest;
import com.personal.jz.module.auth.dto.UserVO;
import com.personal.jz.repository.AuthTokenRepository;
import com.personal.jz.repository.BookRepository;
import com.personal.jz.repository.SysUserRepository;
import com.personal.jz.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserRepository userRepo;
    private final BookRepository bookRepo;
    private final AuthTokenRepository tokenRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwt;

    private static final int MAX_FAILED = 5;
    private static final int LOCK_MINUTES = 30;
    private static final int BOOK_LIMIT = 50;

    @Transactional
    public LoginResponse register(RegisterRequest req) {
        // 唯一性
        Long exists = userRepo.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, req.getUsername()));
        if (exists != null && exists > 0) throw new BizException(ErrorCodeEnums.USERNAME_TAKEN);

        SysUser u = new SysUser();
        u.setUsername(req.getUsername());
        u.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        u.setNickname(req.getNickname() == null || req.getNickname().isEmpty() ? req.getUsername() : req.getNickname());
        u.setEmail(req.getEmail());
        u.setPhone(req.getPhone());
        u.setRole("USER");
        u.setEnabled(1);
        u.setFailedLoginCount(0);
        userRepo.insert(u);

        // 自动建默认账本
        Book b = new Book();
        b.setUserId(u.getId());
        b.setName("日常账本");
        b.setCurrency("CNY");
        b.setIcon("🏠");
        b.setStatus("ACTIVE");
        bookRepo.insert(b);

        u.setCurrentBookId(b.getId());
        userRepo.updateById(u);

        return issueAndRespond(u);
    }

    public LoginResponse login(LoginRequest req) {
        SysUser u = userRepo.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, req.getAccount()));
        if (u == null) u = userRepo.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, req.getAccount()));
        if (u == null) u = userRepo.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhone, req.getAccount()));
        if (u == null) throw new BizException(ErrorCodeEnums.INVALID_CREDENTIALS);

        if (u.getEnabled() != null && u.getEnabled() == 0) throw new BizException(ErrorCodeEnums.ACCOUNT_DISABLED);

        if (u.getLockedUntil() != null && u.getLockedUntil().isAfter(LocalDateTime.now(ZoneId.systemDefault()))) {
            throw new BizException(ErrorCodeEnums.ACCOUNT_LOCKED);
        }

        if (!passwordEncoder.matches(req.getPassword(), u.getPasswordHash())) {
            int failed = (u.getFailedLoginCount() == null ? 0 : u.getFailedLoginCount()) + 1;
            u.setFailedLoginCount(failed);
            if (failed >= MAX_FAILED) {
                u.setLockedUntil(LocalDateTime.now(ZoneId.systemDefault()).plusMinutes(LOCK_MINUTES));
                u.setFailedLoginCount(0);
            }
            userRepo.updateById(u);
            throw new BizException(ErrorCodeEnums.INVALID_CREDENTIALS);
        }

        // 重置失败计数
        if (u.getFailedLoginCount() != null && u.getFailedLoginCount() > 0) {
            u.setFailedLoginCount(0);
            u.setLockedUntil(null);
        }
        u.setLastLoginAt(LocalDateTime.now(ZoneId.systemDefault()));
        userRepo.updateById(u);
        return issueAndRespond(u);
    }

    public UserVO me(Long uid) {
        SysUser u = userRepo.selectById(uid);
        if (u == null) throw new BizException(ErrorCodeEnums.UNAUTHORIZED);
        return toVO(u);
    }

    @Transactional
    public void changePassword(Long uid, String oldPwd, String newPwd) {
        SysUser u = userRepo.selectById(uid);
        if (u == null) throw new BizException(ErrorCodeEnums.UNAUTHORIZED);
        if (!passwordEncoder.matches(oldPwd, u.getPasswordHash())) throw new BizException(ErrorCodeEnums.WRONG_OLD_PASSWORD);
        if (passwordEncoder.matches(newPwd, u.getPasswordHash())) throw new BizException(ErrorCodeEnums.SAME_PASSWORD);
        u.setPasswordHash(passwordEncoder.encode(newPwd));
        userRepo.updateById(u);
        // 吊销所有 token
        tokenRepo.update(null, com.baomidou.mybatisplus.core.toolkit.Wrappers.<AuthToken>lambdaUpdate()
                .set(AuthToken::getRevoked, 1).eq(AuthToken::getUserId, uid));
    }

    @Transactional
    public void updateProfile(Long uid, String nickname) {
        SysUser u = userRepo.selectById(uid);
        if (u == null) throw new BizException(ErrorCodeEnums.UNAUTHORIZED);
        u.setNickname(nickname);
        userRepo.updateById(u);
    }

    @Transactional
    public void logout(Long uid, String token) {
        AuthToken t = tokenRepo.selectOne(new LambdaQueryWrapper<AuthToken>().eq(AuthToken::getToken, token));
        if (t != null) {
            t.setRevoked(1);
            tokenRepo.updateById(t);
        }
    }

    private LoginResponse issueAndRespond(SysUser u) {
        String token = jwt.issue(u.getId(), u.getRole());
        AuthToken t = new AuthToken();
        t.setUserId(u.getId());
        t.setToken(token);
        t.setIssuedAt(LocalDateTime.now(ZoneId.systemDefault()));
        t.setExpiresAt(LocalDateTime.now(ZoneId.systemDefault()).plusSeconds(jwt.getExpireSeconds()));
        t.setRevoked(0);
        tokenRepo.insert(t);
        return new LoginResponse(token, t.getExpiresAt(), toVO(u));
    }

    public static UserVO toVO(SysUser u) {
        return new UserVO(u.getId(), u.getUsername(), u.getNickname(), u.getEmail(), u.getPhone(),
                u.getRole(), u.getEnabled(), u.getCurrentBookId(), u.getCreatedAt());
    }
}
