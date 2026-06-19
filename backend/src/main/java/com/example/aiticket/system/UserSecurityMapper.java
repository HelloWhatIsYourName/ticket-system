package com.example.aiticket.system;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface UserSecurityMapper {
    Optional<UserAccount> findActiveUserByUsername(@Param("username") String username);

    Optional<UserAccount> findActiveUserById(@Param("userId") Long userId);

    List<String> findRoleCodesByUserId(@Param("userId") Long userId);

    List<String> findPermissionCodesByUserId(@Param("userId") Long userId);

    List<MenuSummary> findVisibleMenusByUserId(@Param("userId") Long userId);

    void updateLastLoginAt(@Param("userId") Long userId);

    void insertAuditLog(@Param("actorUserId") Long actorUserId,
                        @Param("actorUsername") String actorUsername,
                        @Param("action") String action,
                        @Param("targetType") String targetType,
                        @Param("targetId") String targetId,
                        @Param("result") String result,
                        @Param("message") String message,
                        @Param("ipAddress") String ipAddress,
                        @Param("userAgent") String userAgent);
}
