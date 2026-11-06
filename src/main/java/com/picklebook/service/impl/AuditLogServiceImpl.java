package com.picklebook.service.impl;

import com.picklebook.entity.AuditLog;
import com.picklebook.entity.User;
import com.picklebook.entity.enums.AuditAction;
import com.picklebook.repository.AuditLogRepository;
import com.picklebook.repository.UserRepository;
import com.picklebook.security.SecurityUtils;
import com.picklebook.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Ghi audit log trong transaction riêng để lỗi log không làm hỏng nghiệp vụ.
 */
@Service
public class AuditLogServiceImpl implements AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogServiceImpl.class);

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(AuditAction action, String entityName, Long entityId, String description) {
        try {
            AuditLog entry = new AuditLog();
            SecurityUtils.currentUserId()
                    .flatMap(userRepository::findById)
                    .ifPresent(entry::setUser);
            entry.setAction(action);
            entry.setEntityName(entityName);
            entry.setEntityId(entityId);
            entry.setDescription(description);
            entry.setIpAddress(resolveClientIp());
            auditLogRepository.save(entry);
        } catch (RuntimeException ex) {
            log.warn("Không ghi được audit log: {}", ex.getMessage());
        }
    }

    private String resolveClientIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }
        return "system";
    }
}
