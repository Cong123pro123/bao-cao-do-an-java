package com.picklebook.controller.web;

import com.picklebook.dto.request.RegisterRequest;
import com.picklebook.exception.BusinessException;
import com.picklebook.security.SecurityUtils;
import com.picklebook.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Đăng nhập (form login do Spring Security xử lý), đăng ký tài khoản.
 */
@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/dang-nhap")
    public String loginPage() {
        if (SecurityUtils.currentUser().isPresent()) {
            return "redirect:/";
        }
        return "auth/login";
    }

    @GetMapping("/dang-ky")
    public String registerPage(Model model) {
        if (SecurityUtils.currentUser().isPresent()) {
            return "redirect:/";
        }
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }

    @PostMapping("/dang-ky")
    public String register(@Valid @ModelAttribute("registerRequest") RegisterRequest request,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes) {
        if (!request.isPasswordConfirmed()) {
            bindingResult.rejectValue("confirmPassword", "mismatch", "Mật khẩu xác nhận không khớp");
        }
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        try {
            authService.register(request);
        } catch (BusinessException ex) {
            bindingResult.rejectValue("email", "duplicate", ex.getMessage());
            return "auth/register";
        }
        redirectAttributes.addFlashAttribute("success",
                "Tạo tài khoản thành công. Vui lòng đăng nhập để đặt sân.");
        return "redirect:/dang-nhap";
    }
}
