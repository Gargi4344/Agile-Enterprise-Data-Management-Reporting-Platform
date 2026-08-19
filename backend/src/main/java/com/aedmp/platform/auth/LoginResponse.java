package com.aedmp.platform.auth;

import com.aedmp.platform.user.dto.UserSummaryResponse;

import java.time.Instant;

public record LoginResponse(String token, Instant expiresAt, UserSummaryResponse user) {
}
