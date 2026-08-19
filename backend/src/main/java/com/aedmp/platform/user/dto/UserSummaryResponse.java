package com.aedmp.platform.user.dto;

import com.aedmp.platform.user.User;

import java.time.Instant;
import java.util.Set;

public record UserSummaryResponse(
        Long id,
        String email,
        String fullName,
        String status,
        Set<String> roles,
        Set<String> permissions,
        Instant createdAt
) {
    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(
                user.getId(), user.getEmail(), user.getFullName(), user.getStatus().name(),
                user.roleNames(), user.permissionCodes(), user.getCreatedAt());
    }
}
