package com.aedmp.platform.user.dto;

import java.util.Set;

public record UpdateUserRequest(
        String fullName,
        String status,
        Set<String> roleNames
) {
}
