package com.hotelreservation.security;

import com.hotelreservation.model.AdminRole;
import com.hotelreservation.model.AdminUser;
import com.hotelreservation.repository.AdminUserRepository;
import com.hotelreservation.util.AppLogger;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuthenticationService {

    private static final Logger LOGGER =
            AppLogger.getLogger(AuthenticationService.class);

    private final AdminUserRepository adminUserRepository;
    private final PasswordService passwordService;
    private final AdminSession adminSession;

    public AuthenticationService(
            AdminUserRepository adminUserRepository,
            PasswordService passwordService,
            AdminSession adminSession
    ) {
        this.adminUserRepository = adminUserRepository;
        this.passwordService = passwordService;
        this.adminSession = adminSession;
    }

    public AuthenticationResult authenticate(
            String username,
            String rawPassword,
            AdminRole selectedRole
    ) {
        String normalizedUsername =
                username == null ? "" : username.trim();

        if (normalizedUsername.isEmpty()
                || rawPassword == null
                || rawPassword.isEmpty()
                || selectedRole == null) {

            return loginFailure(
                    normalizedUsername,
                    "Username, password, and role are required."
            );
        }

        Optional<AdminUser> optionalAdmin =
                adminUserRepository.findByUsername(normalizedUsername);

        if (optionalAdmin.isEmpty()) {
            return loginFailure(
                    normalizedUsername,
                    "Invalid username, password, or role."
            );
        }

        AdminUser adminUser = optionalAdmin.get();

        if (!adminUser.isActive()) {
            return loginFailure(
                    normalizedUsername,
                    "This administrator account is inactive."
            );
        }

        if (adminUser.getRole() != selectedRole) {
            return loginFailure(
                    normalizedUsername,
                    "Invalid username, password, or role."
            );
        }

        if (!passwordService.matches(
                rawPassword,
                adminUser.getPasswordHash()
        )) {
            return loginFailure(
                    normalizedUsername,
                    "Invalid username, password, or role."
            );
        }

        adminSession.start(adminUser);

        AppLogger.audit(
                LOGGER,
                Level.INFO,
                adminUser.getUsername(),
                "LOGIN_SUCCESS",
                "AdminUser",
                String.valueOf(adminUser.getAdminId()),
                "Administrator logged in successfully with role "
                        + adminUser.getRole() + "."
        );

        return AuthenticationResult.success(adminUser);
    }

    public void logout() {
        AdminUser currentAdmin =
                adminSession.getCurrentUser().orElse(null);

        if (currentAdmin == null) {
            return;
        }

        AppLogger.audit(
                LOGGER,
                Level.INFO,
                currentAdmin.getUsername(),
                "LOGOUT",
                "AdminUser",
                String.valueOf(currentAdmin.getAdminId()),
                "Administrator logged out."
        );

        adminSession.clear();
    }

    private AuthenticationResult loginFailure(
            String attemptedUsername,
            String message
    ) {
        AppLogger.audit(
                LOGGER,
                Level.WARNING,
                attemptedUsername.isBlank()
                        ? "UNKNOWN"
                        : attemptedUsername,
                "LOGIN_FAILURE",
                "AdminUser",
                attemptedUsername,
                message
        );

        return AuthenticationResult.failure(message);
    }

    public record AuthenticationResult(
            boolean successful,
            String message,
            AdminUser adminUser
    ) {
        public static AuthenticationResult success(
                AdminUser adminUser
        ) {
            return new AuthenticationResult(
                    true,
                    "Login successful.",
                    adminUser
            );
        }

        public static AuthenticationResult failure(String message) {
            return new AuthenticationResult(
                    false,
                    message,
                    null
            );
        }
    }
}