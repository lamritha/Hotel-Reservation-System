package com.hotelreservation.security;

import com.hotelreservation.model.AdminRole;
import com.hotelreservation.model.AdminUser;

import java.util.Objects;
import java.util.Optional;

public class AdminSession {

    private AdminUser currentUser;

    public void start(AdminUser adminUser) {
        currentUser = Objects.requireNonNull(
                adminUser,
                "Admin user cannot be null."
        );
    }

    public Optional<AdminUser> getCurrentUser() {
        return Optional.ofNullable(currentUser);
    }

    public AdminUser requireCurrentUser() {
        if (currentUser == null) {
            throw new IllegalStateException(
                    "An authenticated administrator is required."
            );
        }

        return currentUser;
    }

    public boolean isAuthenticated() {
        return currentUser != null;
    }

    public boolean hasRole(AdminRole role) {
        return currentUser != null && currentUser.getRole() == role;
    }

    public String getActorName() {
        return currentUser == null
                ? "UNAUTHENTICATED"
                : currentUser.getUsername();
    }

    public void clear() {
        currentUser = null;
    }
}