package com.picklebook.service;

import com.picklebook.dto.request.AdminUserForm;
import com.picklebook.dto.request.ChangePasswordForm;
import com.picklebook.dto.request.ProfileForm;
import com.picklebook.dto.view.UserView;
import com.picklebook.entity.User;
import com.picklebook.entity.enums.RoleName;
import com.picklebook.entity.enums.UserStatus;
import org.springframework.data.domain.Page;

/**
 * Nghiệp vụ người dùng: hồ sơ cá nhân, đổi mật khẩu và quản trị tài khoản.
 */
public interface UserService {

    User getById(Long id);

    User getByEmail(String email);

    UserView getViewById(Long id);

    void updateProfile(Long userId, ProfileForm form);

    void updateAvatar(Long userId, String avatarUrl);

    void changePassword(Long userId, ChangePasswordForm form);

    Page<UserView> search(String keyword, RoleName role, UserStatus status, int page, int size);

    UserView create(AdminUserForm form);

    UserView update(Long id, AdminUserForm form);

    void setStatus(Long id, UserStatus status);

    /**
     * Admin đặt lại mật khẩu: trả về mật khẩu tạm thời để hiển thị một lần cho Admin.
     */
    String resetPassword(Long id);
}
