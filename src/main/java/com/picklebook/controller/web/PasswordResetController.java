package com.picklebook.controller.web;

import com.picklebook.dto.request.ForgotPasswordForm;
import com.picklebook.dto.request.ResetPasswordForm;
import com.picklebook.exception.BusinessException;
import com.picklebook.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Luồng quên mật khẩu: gửi email chứa link token và đặt mật khẩu mới.
 */
@Controller
public class PasswordResetController {

    private final AuthService authService;

    public PasswordResetController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/quen-mat-khau")
    public String forgotPasswordPage(Model model) {
        if (!model.containsAttribute("forgotPasswordForm")) {
            model.addAttribute("forgotPasswordForm", new ForgotPasswordForm());
        }
        return "auth/forgot-password";
    }

    @PostMapping("/quen-mat-khau")
    public String requestReset(@Valid @ModelAttribute("forgotPasswordForm") ForgotPasswordForm form,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "auth/forgot-password";
        }
        authService.requestPasswordReset(form.getEmail());
        // Luôn trả về thông báo giống nhau để không lộ email có tồn tại hay không.
        redirectAttributes.addFlashAttribute("success",
                "Nếu email tồn tại trong hệ thống, chúng tôi đã gửi liên kết đặt lại mật khẩu. Vui lòng kiểm tra hộp thư.");
        return "redirect:/quen-mat-khau";
    }

    @GetMapping("/dat-lai-mat-khau")
    public String resetPasswordPage(@RequestParam(value = "token", required = false) String token,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        if (token == null || token.isBlank()) {
            redirectAttributes.addFlashAttribute("error",
                    "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.");
            return "redirect:/quen-mat-khau";
        }
        ResetPasswordForm form = new ResetPasswordForm();
        form.setToken(token);
        model.addAttribute("resetPasswordForm", form);
        return "auth/reset-password";
    }

    @PostMapping("/dat-lai-mat-khau")
    public String resetPassword(@Valid @ModelAttribute("resetPasswordForm") ResetPasswordForm form,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (!form.isPasswordConfirmed()) {
            bindingResult.rejectValue("confirmPassword", "mismatch", "Mật khẩu xác nhận không khớp");
        }
        if (bindingResult.hasErrors()) {
            return "auth/reset-password";
        }
        try {
            authService.resetPassword(form.getToken(), form.getPassword());
        } catch (BusinessException ex) {
            model.addAttribute("resetError", ex.getMessage());
            return "auth/reset-password";
        }
        redirectAttributes.addFlashAttribute("success",
                "Đặt lại mật khẩu thành công. Vui lòng đăng nhập bằng mật khẩu mới.");
        return "redirect:/dang-nhap";
    }
}
