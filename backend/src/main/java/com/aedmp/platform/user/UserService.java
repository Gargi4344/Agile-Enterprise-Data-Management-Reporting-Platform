package com.aedmp.platform.user;

import com.aedmp.platform.audit.AuditResult;
import com.aedmp.platform.audit.AuditService;
import com.aedmp.platform.common.exception.BadRequestException;
import com.aedmp.platform.common.exception.ConflictException;
import com.aedmp.platform.common.exception.NotFoundException;
import com.aedmp.platform.user.dto.CreateUserRequest;
import com.aedmp.platform.user.dto.UpdateUserRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public Page<User> list(String search, Pageable pageable) {
        if (search == null || search.isBlank()) {
            return userRepository.findAll(pageable);
        }
        return userRepository.findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(search, search, pageable);
    }

    @Transactional
    public User create(CreateUserRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("A user with email " + request.email() + " already exists");
        }
        Set<Role> roles = resolveRoles(request.roleNames());
        User user = User.builder()
                .email(request.email().toLowerCase())
                .fullName(request.fullName())
                .passwordHash(passwordEncoder.encode(request.password()))
                .status(UserStatus.ACTIVE)
                .roles(roles)
                .build();
        User saved = userRepository.save(user);
        auditService.record("USER_CREATE", "USER", String.valueOf(saved.getId()), AuditResult.SUCCESS);
        return saved;
    }

    @Transactional
    public User update(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id).orElseThrow(() -> new NotFoundException("User " + id + " not found"));
        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName());
        }
        if (request.status() != null) {
            try {
                user.setStatus(UserStatus.valueOf(request.status()));
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Unknown status: " + request.status());
            }
        }
        if (request.roleNames() != null) {
            user.setRoles(resolveRoles(request.roleNames()));
        }
        User saved = userRepository.save(user);
        auditService.record("USER_UPDATE", "USER", String.valueOf(saved.getId()), AuditResult.SUCCESS);
        return saved;
    }

    private Set<Role> resolveRoles(Set<String> roleNames) {
        Set<Role> roles = new HashSet<>();
        for (String name : roleNames) {
            roles.add(roleRepository.findByNameIgnoreCase(name)
                    .orElseThrow(() -> new BadRequestException("Unknown role: " + name)));
        }
        return roles;
    }
}
