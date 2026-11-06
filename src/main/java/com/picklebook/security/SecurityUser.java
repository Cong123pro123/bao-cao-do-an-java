package com.picklebook.security;

import com.picklebook.entity.User;
import com.picklebook.entity.enums.RoleName;
import com.picklebook.entity.enums.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Principal đăng nhập của PickleBook. Chứa thêm id và tên để controller/view
 * không phải truy vấn lại người dùng.
 */
public class SecurityUser implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final String fullName;
    private final RoleName role;
    private final boolean active;

    public SecurityUser(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPasswordHash();
        this.fullName = user.getFullName();
        this.role = user.getRole().getName();
        this.active = user.getStatus() == UserStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public RoleName getRole() {
        return role;
    }

    public boolean isAdmin() {
        return role == RoleName.ROLE_ADMIN;
    }

    public boolean isStaff() {
        return role == RoleName.ROLE_STAFF;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
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
