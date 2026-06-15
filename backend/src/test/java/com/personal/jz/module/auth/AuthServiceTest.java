package com.personal.jz.module.auth;

import com.personal.jz.module.auth.dto.RegisterRequest;
import com.personal.jz.module.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AuthServiceTest {

    @Autowired private AuthService authService;

    @Test
    void register_creates_user_and_default_book() {
        var resp = authService.register(make("svc01", "svc01@x.dev", "Test12345"));
        assertNotNull(resp.getToken());
        assertNotNull(resp.getUser().getId());
        assertNotNull(resp.getUser().getCurrentBookId());
    }

    @Test
    void me_returns_user() {
        var resp = authService.register(make("svc02", "svc02@x.dev", "Test12345"));
        var me = authService.me(resp.getUser().getId());
        assertEquals("svc02", me.getUsername());
    }

    @Test
    void change_password_updates_hash() {
        var resp = authService.register(make("svc03", "svc03@x.dev", "Test12345"));
        authService.changePassword(resp.getUser().getId(), "Test12345", "NewPass123");
    }

    private RegisterRequest make(String u, String e, String p) {
        RegisterRequest r = new RegisterRequest();
        r.setUsername(u); r.setEmail(e); r.setPassword(p);
        return r;
    }
}
