package com.aedmp.platform.auth;

import com.aedmp.platform.security.AuthenticatedUser;
import com.aedmp.platform.user.dto.UserSummaryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserSummaryResponse me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return UserSummaryResponse.from(principal.getUser());
    }
}
