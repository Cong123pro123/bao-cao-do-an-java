package com.picklebook.config;

import com.picklebook.dto.view.UserView;
import com.picklebook.security.SecurityUtils;
import com.picklebook.service.SettingService;
import com.picklebook.service.UserService;
import com.picklebook.util.TimeUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.Year;
import java.util.Map;

/**
 * Bổ sung dữ liệu dùng chung cho mọi view: cấu hình website, người dùng đang
 * đăng nhập, đường dẫn hiện tại và năm bản quyền.
 *
 * <p>Các truy vấn được bọc try/catch để trang lỗi vẫn hiển thị được khi
 * database gặp sự cố.</p>
 */
@ControllerAdvice
public class GlobalModelAdvice {

    private static final Logger log = LoggerFactory.getLogger(GlobalModelAdvice.class);

    private final SettingService settingService;
    private final UserService userService;

    public GlobalModelAdvice(SettingService settingService, UserService userService) {
        this.settingService = settingService;
        this.userService = userService;
    }

    @ModelAttribute("site")
    public Map<String, String> site() {
        try {
            return settingService.asMap();
        } catch (RuntimeException ex) {
            log.warn("Không đọc được cấu hình website: {}", ex.getMessage());
            return Map.of();
        }
    }

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("currentUser")
    public UserView currentUser() {
        try {
            return SecurityUtils.currentUserId()
                    .map(userService::getViewById)
                    .orElse(null);
        } catch (RuntimeException ex) {
            log.debug("Không tải được người dùng hiện tại: {}", ex.getMessage());
            return null;
        }
    }

    @ModelAttribute("currentYear")
    public int currentYear() {
        return Year.now(TimeUtils.ZONE).getValue();
    }
}
