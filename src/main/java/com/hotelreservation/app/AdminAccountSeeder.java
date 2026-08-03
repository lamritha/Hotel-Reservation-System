package com.hotelreservation.app;

import com.hotelreservation.model.AdminRole;
import com.hotelreservation.model.AdminUser;
import com.hotelreservation.repository.AdminUserRepository;
import com.hotelreservation.security.PasswordService;
import com.hotelreservation.util.AppLogger;
import com.hotelreservation.util.JpaUtil;

import java.util.logging.Level;
import java.util.logging.Logger;

public class AdminAccountSeeder {

    private static final Logger LOGGER =
            AppLogger.getLogger(AdminAccountSeeder.class);

    private final AdminUserRepository adminUserRepository;
    private final PasswordService passwordService;

    public AdminAccountSeeder(
            AdminUserRepository adminUserRepository,
            PasswordService passwordService
    ) {
        this.adminUserRepository = adminUserRepository;
        this.passwordService = passwordService;
    }

    public void seedDefaultAccounts() {
        seedAccountIfMissing(
                "admin",
                "admin123",
                "System",
                "Administrator",
                "admin@grandstayhotel.com",
                AdminRole.ADMIN
        );

        seedAccountIfMissing(
                "manager",
                "manager123",
                "Hotel",
                "Manager",
                "manager@grandstayhotel.com",
                AdminRole.MANAGER
        );
    }

    private void seedAccountIfMissing(
            String username,
            String rawPassword,
            String firstName,
            String lastName,
            String email,
            AdminRole role
    ) {
        if (adminUserRepository.findByUsername(username).isPresent()) {
            return;
        }

        String passwordHash =
                passwordService.hashPassword(rawPassword);

        AdminUser adminUser = new AdminUser(
                username,
                passwordHash,
                firstName,
                lastName,
                email,
                role
        );

        AdminUser savedAdmin =
                adminUserRepository.save(adminUser);

        AppLogger.audit(
                LOGGER,
                Level.INFO,
                "SYSTEM",
                "ADMIN_ACCOUNT_CREATED",
                "AdminUser",
                String.valueOf(savedAdmin.getAdminId()),
                "Default " + role
                        + " account created for username "
                        + username + "."
        );
    }

    public static void main(String[] args) {
        try {
            AdminAccountSeeder seeder = new AdminAccountSeeder(
                    new AdminUserRepository(),
                    new PasswordService()
            );

            seeder.seedDefaultAccounts();
            System.out.println(
                    "Default administrator accounts are ready."
            );

        } catch (Exception exception) {
            AppLogger.exception(
                    LOGGER,
                    "Failed to seed administrator accounts.",
                    exception
            );

            System.err.println(
                    "Administrator account seeding failed: "
                            + exception.getMessage()
            );

        } finally {
            JpaUtil.close();
        }
    }
}