package com.cloudidentity.controller;

import com.cloudidentity.dto.request.LoginRequest;
import com.cloudidentity.dto.response.AuthResponse;
import com.cloudidentity.dto.response.UserResponse;
import com.cloudidentity.entity.User;
import com.cloudidentity.repository.UserRepository;
import com.cloudidentity.security.UserPrincipal;
import com.cloudidentity.service.AuthService;
import com.cloudidentity.util.MapperUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MapperUtil mapperUtil;

    @InjectMocks
    private AuthController authController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Should return 200 OK and JWT token upon successful login")
    void testLogin_Success() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("AdminPass123!");

        AuthResponse authResponse = AuthResponse.builder()
                .token("mock-jwt-token")
                .tokenType("Bearer")
                .expiresInMs(3600000L)
                .username("admin")
                .email("admin@cloudidentity.io")
                .roles(java.util.List.of("ROLE_ADMIN"))
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-jwt-token"))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }
}
