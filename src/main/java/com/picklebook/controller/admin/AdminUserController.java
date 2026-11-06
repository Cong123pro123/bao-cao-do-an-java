package com.picklebook.controller.admin;

import com.picklebook.dto.request.AdminUserForm;
import com.picklebook.dto.view.UserView;
import com.picklebook.entity.enums.RoleName;
import com.picklebook.entity.enums.UserStatus;
import com.picklebook.exception.BusinessException;
import com.picklebook.service.CourtBookingService;
import com.picklebook.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserService userService;
    private final CourtBookingService courtBookingService;

    public AdminUserController(UserService userService, CourtBookingService courtBookingService) {
        this.userService = userService;
        this.courtBookingService = courtBookingService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) RoleName role,
                       @RequestParam(required = false) UserStatus status,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        model.addAttribute("users", userService.search(keyword, role, status, page, 25));
        model.addAttribute("roles", RoleName.values());
        model.addAttribute("statuses", UserStatus.values());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedRole", role);
        model.addAttribute("selectedStatus", status);
        return "admin/users";
    }

    @GetMapping("/new")
    public String createPage(Model model) {
        model.addAttribute("userForm", new AdminUserForm());
        populateFormOptions(model);
        return "admin/user-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("userForm") AdminUserForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateFormOptions(model);
            return "admin/user-form";
        }
        try {
            userService.create(form);
            redirectAttributes.addFlashAttribute("success", "Đã tạo tài khoản.");
            return "redirect:/admin/users";
        } catch (BusinessException ex) {
            bindingResult.rejectValue("email", "duplicate", ex.getMessage());
            populateFormOptions(model);
            return "admin/user-form";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        UserView user = userService.getViewById(id);
        model.addAttribute("user", user);
        model.addAttribute("bookings", courtBookingService.getBookingsForUser(id, 0).getContent());
        return "admin/user-detail";
    }

    @GetMapping("/{id}/edit")
    public String editPage(@PathVariable Long id, Model model) {
        UserView user = userService.getViewById(id);
        AdminUserForm form = new AdminUserForm();
        form.setFullName(user.getFullName());
        form.setEmail(user.getEmail());
        form.setPhone(user.getPhone());
        form.setRole(user.getRole());
        form.setStatus(user.getStatus());
        model.addAttribute("userForm", form);
        model.addAttribute("userId", id);
        populateFormOptions(model);
        return "admin/user-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("userForm") AdminUserForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("userId", id);
            populateFormOptions(model);
            return "admin/user-form";
        }
        try {
            userService.update(id, form);
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật tài khoản.");
            return "redirect:/admin/users/" + id;
        } catch (BusinessException ex) {
            bindingResult.rejectValue("email", "invalid", ex.getMessage());
            model.addAttribute("userId", id);
            populateFormOptions(model);
            return "admin/user-form";
        }
    }

    @PostMapping("/{id}/status")
    public String status(@PathVariable Long id,
                         @RequestParam UserStatus status,
                         RedirectAttributes redirectAttributes) {
        try {
            userService.setStatus(id, status);
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật trạng thái tài khoản.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/users/" + id;
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        String temporaryPassword = userService.resetPassword(id);
        redirectAttributes.addFlashAttribute("temporaryPassword", temporaryPassword);
        redirectAttributes.addFlashAttribute("success", "Đã tạo mật khẩu tạm thời. Hãy sao chép và gửi an toàn cho người dùng.");
        return "redirect:/admin/users/" + id;
    }

    private void populateFormOptions(Model model) {
        model.addAttribute("roles", RoleName.values());
        model.addAttribute("statuses", UserStatus.values());
    }
}
