package com.picklebook.controller.web;

import com.picklebook.dto.request.ChangePasswordForm;
import com.picklebook.dto.request.ProfileForm;
import com.picklebook.dto.view.UserView;
import com.picklebook.exception.BusinessException;
import com.picklebook.security.SecurityUtils;
import com.picklebook.service.FileStorageService;
import com.picklebook.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Hồ sơ cá nhân: cập nhật thông tin, ảnh đại diện và đổi mật khẩu.
 */
@Controller
@RequestMapping("/tai-khoan")
public class ProfileController {

    private final UserService userService;
    private final FileStorageService fileStorageService;

    public ProfileController(UserService userService, FileStorageService fileStorageService) {
        this.userService = userService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public String profilePage(Model model) {
        Long userId = requireUserId();
        UserView user = userService.getViewById(userId);
        ProfileForm form = new ProfileForm();
        form.setFullName(user.getFullName());
        form.setEmail(user.getEmail());
        form.setPhone(user.getPhone());
        model.addAttribute("profileForm", form);
        model.addAttribute("user", user);
        return "my/profile";
    }

    @PostMapping
    public String updateProfile(@Valid @ModelAttribute("profileForm") ProfileForm form,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        Long userId = requireUserId();
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", userService.getViewById(userId));
            return "my/profile";
        }
        try {
            userService.updateProfile(userId, form);
        } catch (BusinessException ex) {
            bindingResult.rejectValue("email", "emailUsed", ex.getMessage());
            model.addAttribute("user", userService.getViewById(userId));
            return "my/profile";
        }
        redirectAttributes.addFlashAttribute("success", "Đã cập nhật thông tin cá nhân.");
        return "redirect:/tai-khoan";
    }

    @PostMapping("/avatar")
    public String updateAvatar(@RequestParam("avatar") MultipartFile avatar,
                               RedirectAttributes redirectAttributes) {
        Long userId = requireUserId();
        String oldAvatar = userService.getViewById(userId).getAvatarUrl();
        try {
            String url = fileStorageService.store(avatar, "avatars");
            userService.updateAvatar(userId, url);
            fileStorageService.delete(oldAvatar);
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật ảnh đại diện.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/tai-khoan";
    }

    @GetMapping("/mat-khau")
    public String passwordPage(Model model) {
        if (!model.containsAttribute("changePasswordForm")) {
            model.addAttribute("changePasswordForm", new ChangePasswordForm());
        }
        return "my/password";
    }

    @PostMapping("/mat-khau")
    public String changePassword(@Valid @ModelAttribute("changePasswordForm") ChangePasswordForm form,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes) {
        Long userId = requireUserId();
        if (!form.isPasswordConfirmed()) {
            bindingResult.rejectValue("confirmPassword", "mismatch", "Mật khẩu xác nhận không khớp");
        }
        if (bindingResult.hasErrors()) {
            return "my/password";
        }
        try {
            userService.changePassword(userId, form);
        } catch (BusinessException ex) {
            bindingResult.reject("password", ex.getMessage());
            return "my/password";
        }
        redirectAttributes.addFlashAttribute("success", "Đổi mật khẩu thành công.");
        return "redirect:/tai-khoan";
    }

    private Long requireUserId() {
        return SecurityUtils.currentUserId()
                .orElseThrow(() -> new BusinessException("Vui lòng đăng nhập để tiếp tục"));
    }
}
