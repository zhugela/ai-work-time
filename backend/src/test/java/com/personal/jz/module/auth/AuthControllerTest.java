package com.personal.jz.module.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.personal.jz.module.auth.dto.LoginRequest;
import com.personal.jz.module.auth.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import jakarta.annotation.PostConstruct;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class AuthControllerTest {

    @Autowired private WebApplicationContext wac;
    @Autowired private ObjectMapper om;
    private MockMvc mvc;

    @PostConstruct
    void init() { mvc = MockMvcBuilders.webAppContextSetup(wac).build(); }

    @Test
    void register_then_login() throws Exception {
        RegisterRequest r = new RegisterRequest();
        r.setUsername("alice01");
        r.setEmail("a01@test.dev");
        r.setPassword("Test12345");
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(r)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.token").exists());

        LoginRequest l = new LoginRequest();
        l.setAccount("alice01");
        l.setPassword("Test12345");
        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(l)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.token").exists());
    }

    @Test
    void register_weak_password_should_400() throws Exception {
        RegisterRequest r = new RegisterRequest();
        r.setUsername("bob01");
        r.setEmail("b01@test.dev");
        r.setPassword("123");
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(r)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void register_duplicate_username_should_409() throws Exception {
        RegisterRequest r = new RegisterRequest();
        r.setUsername("dup01");
        r.setEmail("d01@test.dev");
        r.setPassword("Test12345");
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(r))).andExpect(status().isOk());
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(r)))
            .andExpect(status().isConflict());
    }

    @Test
    void login_wrong_password_should_401() throws Exception {
        LoginRequest l = new LoginRequest();
        l.setAccount("alice01");
        l.setPassword("WrongPass1");
        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(l)))
            .andExpect(status().isUnauthorized());
    }
}
