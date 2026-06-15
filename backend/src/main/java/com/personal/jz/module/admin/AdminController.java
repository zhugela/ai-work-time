package com.personal.jz.module.admin;

import com.personal.jz.common.api.ApiResponse;
import com.personal.jz.common.api.PageResponse;
import com.personal.jz.common.security.CurrentUser;
import com.personal.jz.entity.AuditLog;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.ok(adminService.stats());
    }

    @PutMapping("/users/{id}/enabled")
    public ApiResponse<Void> enable(@CurrentUser Long self, @PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        adminService.enableUser(self, id, body.getOrDefault("enabled", true));
        return ApiResponse.ok();
    }

    @GetMapping("/audit-logs")
    public ApiResponse<PageResponse<AuditLog>> auditLogs(@RequestParam(required = false) Long userId,
                                                          @RequestParam(required = false) String action,
                                                          @RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(adminService.auditLogs(userId, action, page, size));
    }
}
