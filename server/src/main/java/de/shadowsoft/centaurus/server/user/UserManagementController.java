package de.shadowsoft.centaurus.server.user;

import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class UserManagementController {

    private final UserManagementService userManagementService;

    public UserManagementController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping
    public List<UserResponse> listUsers() {
        return userManagementService.listUsers();
    }

    @PostMapping
    public CreateUserResponse createUser(@RequestBody CreateUserRequest request, Authentication authentication) {
        return userManagementService.createUser(request, authentication);
    }

    @PutMapping("/{userId}/role")
    public UserResponse updateRole(
        @PathVariable UUID userId,
        @RequestBody UpdateUserRoleRequest request,
        Authentication authentication
    ) {
        return userManagementService.updateRole(userId, request, authentication);
    }

    @PostMapping("/{userId}/reset-password")
    public ResetUserPasswordResponse resetPassword(@PathVariable UUID userId, Authentication authentication) {
        return userManagementService.resetPassword(userId, authentication);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId, Authentication authentication) {
        userManagementService.deleteUser(userId, authentication);
        return ResponseEntity.noContent().build();
    }
}
