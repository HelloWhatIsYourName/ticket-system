package com.example.aiticket.system;

import org.springframework.stereotype.Service;

@Service
public class AuditLogService {
    private final UserSecurityMapper mapper;

    public AuditLogService(UserSecurityMapper mapper) {
        this.mapper = mapper;
    }

    public void record(Long actorUserId, String actorUsername, String action, String targetType,
                       String targetId, String result, String message, String ipAddress, String userAgent) {
        mapper.insertAuditLog(actorUserId, actorUsername, action, targetType, targetId, result, message, ipAddress, userAgent);
    }
}
