package com.picklebook.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Các trang lỗi thân thiện, có lối thoát về trang chủ.
 */
@Controller
public class ErrorPageController {

    @GetMapping("/403")
    public String forbidden(Model model) {
        model.addAttribute("status", 403);
        model.addAttribute("message", "Bạn không có quyền truy cập trang này.");
        return "error/403";
    }

    @GetMapping("/404")
    public String notFound(Model model) {
        model.addAttribute("status", 404);
        model.addAttribute("message", "Trang bạn tìm không tồn tại hoặc đã được di chuyển.");
        return "error/404";
    }

    @GetMapping("/500")
    public String serverError(Model model) {
        model.addAttribute("status", 500);
        model.addAttribute("message", "Hệ thống đang gặp sự cố. Vui lòng thử lại sau.");
        return "error/500";
    }
}
