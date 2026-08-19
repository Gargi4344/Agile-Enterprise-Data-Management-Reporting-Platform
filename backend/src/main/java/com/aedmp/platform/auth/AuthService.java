package com.aedmp.platform.auth;

import com.aedmp.platform.audit.AuditResult;
import com.aedmp.platform.audit.AuditService;
import com.aedmp.platform.security.AuthenticatedUser;
import com.aedmp.platform.security.JwtService;
import com.aedmp.platform.user.dto.UserSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuditService auditService;

    public LoginResponse login(LoginRequest request) {
        try {
            var authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
            AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
            String token = jwtService.issue(principal);
            Instant expiresAt = jwtService.expirationFor(Instant.now());
            auditService.record("AUTH_LOGIN", "USER", String.valueOf(principal.getId()), AuditResult.SUCCESS);
            return new LoginResponse(token, expiresAt, UserSummaryResponse.from(principal.getUser()));
        } catch (BadCredentialsException | org.springframework.security.authentication.DisabledException ex) {
            auditService.record("AUTH_LOGIN", "USER", request.email(), AuditResult.DENIED);
            throw ex;
        }
    }
}
