package com.picklebook.dto.request;

import com.picklebook.entity.enums.RoleName;
import com.picklebook.entity.enums.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Form Admin thêm/sửa người dùng. Mật khẩu bỏ trống khi sửa nghĩa là giữ nguyên.
 */
public class AdminUserForm {

    @NotBlank(message = "Vui lòng nhập họ tên")
    @Size(min = 2, max = 120, message = "Họ tên phải từ 2 đến 120 ký tự")
    private String fullName;

    @NotBlank(message = "Vui lòng nhập email")
    @Email(message = "Email không hợp lệ")
    @Size(max = 160, message = "Email quá dài")
    private String email;

    @Pattern(regexp = "^(0\\d{9,10})?$", message = "Số điện thoại phải bắt đầu bằng 0 và có 10-11 chữ số")
    private String phone;

    @NotNull(message = "Vui lòng chọn vai trò")
    private RoleName role = RoleName.ROLE_USER;

    @NotNull(message = "Vui lòng chọn trạng thái")
    private UserStatus status = UserStatus.ACTIVE;

    @Size(max = 72, message = "Mật khẩu tối đa 72 ký tự")
    private String password;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public RoleName getRole() {
        return role;
    }

    public void setRole(RoleName role) {
        this.role = role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
