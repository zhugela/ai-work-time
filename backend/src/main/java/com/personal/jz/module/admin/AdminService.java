package com.personal.jz.module.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personal.jz.common.api.PageResponse;
import com.personal.jz.common.exception.BizException;
import com.personal.jz.common.exception.ErrorCodeEnums;
import com.personal.jz.entity.AuditLog;
import com.personal.jz.entity.AuthToken;
import com.personal.jz.entity.SysUser;
import com.personal.jz.repository.AuditLogRepository;
import com.personal.jz.repository.AuthTokenRepository;
import com.personal.jz.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final SysUserRepository userRepo;
    private final AuthTokenRepository tokenRepo;
    private final AuditLogRepository auditRepo;

    public Map<String, Object> stats() {
        long users = userRepo.selectCount(null);
        long books = userRepo.selectCount(null); // 简化：真实 SQL 需 join
        return Map.of("userCount", users, "bookCount", books, "txCount", 0, "activeUserCount", 0);
    }

    @Transactional
    public void enableUser(Long selfId, Long targetId, boolean enabled) {
        if (selfId.equals(targetId)) throw new BizException(ErrorCodeEnums.CANNOT_DISABLE_SELF);
        SysUser u = userRepo.selectById(targetId);
        if (u == null) throw new BizException(ErrorCodeEnums.TARGET_USER_NOT_FOUND);
        u.setEnabled(enabled ? 1 : 0);
        userRepo.updateById(u);
        if (!enabled) {
            tokenRepo.update(null, new com.baomidou.mybatisplus.core.toolkit.Wrappers.<AuthToken>lambdaUpdate()
                    .set(AuthToken::getRevoked, 1).eq(AuthToken::getUserId, targetId));
        }
    }

    public PageResponse<AuditLog> auditLogs(Long userId, String action, int page, int size) {
        Page<AuditLog> p = new Page<>(page, size);
        var w = new LambdaQueryWrapper<AuditLog>()
                .eq(userId != null, AuditLog::getUserId, userId)
                .eq(action != null && !action.isEmpty(), AuditLog::getAction, action)
                .orderByDesc(AuditLog::getCreatedAt);
        Page<AuditLog> res = auditRepo.selectPage(p, w);
        return PageResponse.of(res.getRecords(), page, size, res.getTotal());
    }
}
