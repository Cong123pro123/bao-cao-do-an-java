package com.picklebook.service;

import com.picklebook.entity.enums.EmailTemplateCode;

import java.util.Map;

/**
 * Gửi email theo mẫu trong DB. Khi chưa cấu hình SMTP, email được ghi ra log
 * để không chặn luồng nghiệp vụ.
 */
public interface EmailService {

    void sendTemplate(EmailTemplateCode code, String to, Map<String, ?> variables);

    void sendHtml(String to, String subject, String html);

    boolean isEnabled();
}
