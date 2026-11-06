package com.picklebook.service.impl;

import com.picklebook.entity.EmailTemplate;
import com.picklebook.entity.enums.EmailTemplateCode;
import com.picklebook.repository.EmailTemplateRepository;
import com.picklebook.service.EmailService;
import com.picklebook.service.SettingService;
import com.picklebook.util.TemplateRenderer;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Gửi email HTML. Nếu chưa bật SMTP hoặc thiếu thông tin đăng nhập, nội dung
 * email được ghi vào log (chế độ demo) thay vì ném lỗi ra ngoài.
 */
@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final EmailTemplateRepository templateRepository;
    private final SettingService settingService;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final boolean mailEnabledByEnv;
    private final String defaultFrom;

    public EmailServiceImpl(EmailTemplateRepository templateRepository,
                            SettingService settingService,
                            ObjectProvider<JavaMailSender> mailSenderProvider,
                            @Value("${app.mail.enabled:false}") boolean mailEnabledByEnv,
                            @Value("${app.mail.from:PickleBook <no-reply@picklebook.vn>}") String defaultFrom) {
        this.templateRepository = templateRepository;
        this.settingService = settingService;
        this.mailSenderProvider = mailSenderProvider;
        this.mailEnabledByEnv = mailEnabledByEnv;
        this.defaultFrom = defaultFrom;
    }

    @Override
    public boolean isEnabled() {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        return sender != null && mailEnabledByEnv;
    }

    @Override
    public void sendTemplate(EmailTemplateCode code, String to, Map<String, ?> variables) {
        EmailTemplate template = templateRepository.findByCode(code)
                .filter(EmailTemplate::isActive)
                .orElse(null);
        if (template == null) {
            log.warn("Không tìm thấy mẫu email hoặc mẫu đã tắt: {}", code);
            return;
        }
        Map<String, Object> merged = new HashMap<>();
        merged.put("siteName", settingService.get("site.name", "PickleBook"));
        merged.put("sitePhone", settingService.get("site.phone", ""));
        merged.put("siteAddress", settingService.get("site.address", ""));
        if (variables != null) {
            merged.putAll(variables);
        }
        String subject = TemplateRenderer.render(template.getSubject(), merged);
        String html = TemplateRenderer.render(template.getBodyHtml(), merged);
        sendHtml(to, subject, html);
    }

    @Override
    public void sendHtml(String to, String subject, String html) {
        if (to == null || to.isBlank()) {
            log.warn("Bỏ qua email vì thiếu địa chỉ người nhận, subject={}", subject);
            return;
        }
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        String username = settingService.get("spring.mail.username", "");
        if (!isEnabled() || sender == null) {
            log.info("Email chưa bật SMTP - ghi log. To={} | Subject={}", to, subject);
            log.debug("Nội dung email:\n{}", html);
            return;
        }
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            String from = settingService.get("mail.from", defaultFrom);
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            sender.send(message);
            log.info("Đã gửi email tới {} với chủ đề '{}'", to, subject);
        } catch (MessagingException | MailException ex) {
            // Ghi log lỗi nhưng không làm hỏng giao dịch đặt sân.
            log.error("Gửi email thất bại tới {}: {}", to, ex.getMessage());
        }
    }
}
