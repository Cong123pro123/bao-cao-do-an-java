package com.picklebook.service;

import com.picklebook.entity.enums.AuditAction;

/**
 * Ghi nhật ký hoạt động quản trị (ai làm gì, lúc nào, IP nào).
 */
public interface AuditLogService {

    void log(AuditAction action, String entityName, Long entityId, String description);
}
