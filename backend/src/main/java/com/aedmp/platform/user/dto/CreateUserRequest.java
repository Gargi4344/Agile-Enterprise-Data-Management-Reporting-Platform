package com.aedmp.platform.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateUserRequest(
        @NotBlank @Email String email,
        @NotBlank String fullName,
        @NotBlank @Size(min = 10, message = "Password must be at least 10 characters") String password,
        @NotEmpty Set<String> roleNames
) {
}
