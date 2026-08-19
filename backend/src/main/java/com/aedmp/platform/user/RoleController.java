package com.aedmp.platform.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class RoleController {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public record RoleResponse(Long id, String name, String description, Set<String> permissions) {
    }

    public record PermissionResponse(Long id, String code, String description) {
    }

    @GetMapping("/api/roles")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public List<RoleResponse> roles() {
        return roleRepository.findAll().stream()
                .map(r -> new RoleResponse(r.getId(), r.getName(), r.getDescription(),
                        r.getPermissions().stream().map(Permission::getCode).collect(Collectors.toSet())))
                .toList();
    }

    @GetMapping("/api/permissions")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public List<PermissionResponse> permissions() {
        return permissionRepository.findAll().stream()
                .map(p -> new PermissionResponse(p.getId(), p.getCode(), p.getDescription()))
                .toList();
    }
}
