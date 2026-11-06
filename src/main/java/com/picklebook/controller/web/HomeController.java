package com.picklebook.controller.web;

import com.picklebook.service.SettingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Trang chủ, giới thiệu và chính sách - các trang công khai dùng cấu hình
 * website do Admin nhập.
 */
@Controller
public class HomeController {

    private final SettingService settingService;

    public HomeController(SettingService settingService) {
        this.settingService = settingService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("heroTitle", settingService.get("home.hero_title", "Đặt sân Pickleball. Chơi là ghiền."));
        model.addAttribute("heroSubtitle", settingService.get("home.hero_subtitle",
                "Xem khung giờ trống theo thời gian thực, đặt sân trong 30 giây, hủy đổi linh hoạt."));
        model.addAttribute("heroImage", settingService.get("home.hero_image", "/img/banners/banner-1.svg"));
        return "home/index";
    }

    @GetMapping("/gioi-thieu")
    public String about(Model model) {
        model.addAttribute("aboutText", settingService.get("footer.about",
                "PickleBook mang đến trải nghiệm đặt sân pickleball hiện đại, minh bạch và tiện lợi."));
        return "home/about";
    }

    @GetMapping("/chinh-sach")
    public String policy(Model model) {
        model.addAttribute("cancelPolicy", settingService.get("policy.cancel", ""));
        model.addAttribute("rules", settingService.get("policy.rules", ""));
        model.addAttribute("cancelBeforeHours", settingService.getInt("booking.cancel_before_hours", 12));
        model.addAttribute("rescheduleBeforeHours", settingService.getInt("booking.reschedule_before_hours", 12));
        model.addAttribute("depositPercent", settingService.getInt("booking.deposit_percent", 30));
        model.addAttribute("reminderHours", settingService.getInt("booking.reminder_hours", 3));
        return "home/policy";
    }
}
