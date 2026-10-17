package com.cloudidentity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private boolean isActive;
    private boolean mfaEnabled;
    private LocalDateTime lastLoginAt;
    private List<String> roles;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
