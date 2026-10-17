package com.cloudidentity.security;

import com.cloudidentity.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adapts the {@link User} entity to Spring Security's {@link UserDetails} interface.
 * Wraps the user without directly coupling the entity to Spring Security.
 */
@Getter
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String email;
    private final String password;
    private final boolean active;
    private final boolean accountNonLocked;
    private final Collection<? extends GrantedAuthority> authorities;

    public static UserPrincipal create(User user) {
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(SecurityConstants.ROLE_PREFIX + role.getName()))
                .toList();

        return new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPasswordHash(),
                Boolean.TRUE.equals(user.getIsActive()),
                Boolean.TRUE.equals(user.getAccountNonLocked()),
                authorities
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
