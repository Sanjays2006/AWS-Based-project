package com.cloudidentity.service;

import com.cloudidentity.dto.request.RegisterRequest;
import com.cloudidentity.dto.request.UpdateUserRequest;
import com.cloudidentity.dto.response.PagedResponse;
import com.cloudidentity.dto.response.UserResponse;
import com.cloudidentity.entity.Role;
import com.cloudidentity.entity.User;
import com.cloudidentity.exception.DuplicateResourceException;
import com.cloudidentity.exception.ResourceNotFoundException;
import com.cloudidentity.repository.RoleRepository;
import com.cloudidentity.repository.UserRepository;
import com.cloudidentity.util.MapperUtil;
import com.cloudidentity.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final MapperUtil mapperUtil;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> listUsers(String search, int page, int size, String sortBy, String sortDir) {
        Pageable pageable = PaginationUtil.createPageable(page, size, sortBy, sortDir);
        Page<User> userPage = userRepository.searchUsers(search, pageable);
        
        return PaginationUtil.toPagedResponse(userPage, 
                userPage.getContent().stream().map(mapperUtil::toUserResponse).collect(Collectors.toList()));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return mapperUtil.toUserResponse(user);
    }

    @Transactional
    public UserResponse createUser(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email is already registered");
        }

        Role role = roleRepository.findByName("VIEWER")
                .orElseThrow(() -> new ResourceNotFoundException("Role VIEWER not found"));

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .fullName(request.getFullName())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .isActive(true)
                .mfaEnabled(false)
                .roles(Set.of(role))
                .build();

        userRepository.save(user);
        
        auditLogService.logAction(null, "CREATE_USER", "User", user.getId(), 
                "Created new user account: " + user.getUsername(), null, null);

        return mapperUtil.toUserResponse(user);
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email is already in use by another user");
        }

        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());

        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }

        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            Set<Role> roles = new HashSet<>();
            for (String roleName : request.getRoles()) {
                Role role = roleRepository.findByName(roleName)
                        .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));
                roles.add(role);
            }
            user.setRoles(roles);
        }

        userRepository.save(user);

        auditLogService.logAction(null, "UPDATE_USER", "User", user.getId(), 
                "Updated details of user: " + user.getUsername(), null, null);

        return mapperUtil.toUserResponse(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        
        // Hard or soft delete? Soft delete is safer for audit logging references
        user.setIsActive(false);
        userRepository.save(user);

        auditLogService.logAction(null, "DELETE_USER", "User", user.getId(), 
                "Soft-deleted/Disabled user account: " + user.getUsername(), null, null);
    }

    @Transactional
    public void toggleStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setIsActive(!user.getIsActive());
        userRepository.save(user);

        auditLogService.logAction(null, "TOGGLE_USER_STATUS", "User", user.getId(), 
                "Toggled status of user: " + user.getUsername() + " to " + user.getIsActive(), null, null);
    }
}
