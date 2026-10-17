package com.cloudidentity.security;

import com.cloudidentity.entity.User;
import com.cloudidentity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads user-specific data from the database for Spring Security authentication.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> {
                    log.warn("User not found with username or email: {}", username);
                    return new UsernameNotFoundException(
                            "User not found with username or email: " + username);
                });

        log.debug("Loaded user '{}' with roles: {}", user.getUsername(),
                user.getRoles().stream().map(r -> r.getName()).toList());

        return UserPrincipal.create(user);
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));
        return UserPrincipal.create(user);
    }
}
