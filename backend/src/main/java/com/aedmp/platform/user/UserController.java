package com.aedmp.platform.user;

import com.aedmp.platform.common.web.PageResponse;
import com.aedmp.platform.user.dto.CreateUserRequest;
import com.aedmp.platform.user.dto.UpdateUserRequest;
import com.aedmp.platform.user.dto.UserSummaryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('USER_MANAGE')")
public class UserController {

    private final UserService userService;

    @GetMapping
    public PageResponse<UserSummaryResponse> list(@RequestParam(required = false) String search, Pageable pageable) {
        return PageResponse.of(userService.list(search, pageable), UserSummaryResponse::from);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserSummaryResponse create(@Valid @RequestBody CreateUserRequest request) {
        return UserSummaryResponse.from(userService.create(request));
    }

    @PatchMapping("/{id}")
    public UserSummaryResponse update(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
        return UserSummaryResponse.from(userService.update(id, request));
    }
}
