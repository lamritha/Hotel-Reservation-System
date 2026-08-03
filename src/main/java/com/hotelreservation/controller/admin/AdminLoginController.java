package com.hotelreservation.controller.admin;

import com.hotelreservation.model.AdminRole;
import com.hotelreservation.security.AuthenticationService;
import com.hotelreservation.util.SceneNavigator;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class AdminLoginController {

    private final AuthenticationService authenticationService;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private ComboBox<String> roleComboBox;

    public AdminLoginController(
            AuthenticationService authenticationService
    ) {
        this.authenticationService = authenticationService;
    }

    @FXML
    private void initialize() {
        roleComboBox.setItems(
                FXCollections.observableArrayList(
                        "Admin",
                        "Manager"
                )
        );

        roleComboBox.getSelectionModel().selectFirst();
    }

    @FXML
    private void login() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        AdminRole selectedRole =
                convertRole(roleComboBox.getValue());

        AuthenticationService.AuthenticationResult result =
                authenticationService.authenticate(
                        username,
                        password,
                        selectedRole
                );

        if (!result.successful()) {
            passwordField.clear();
            showError(result.message());
            return;
        }

        showSuccess(
                result.adminUser().getFullName(),
                result.adminUser().getRole()
        );

        SceneNavigator.switchTo(
                "/views/admin/AdminDashboardView.fxml"
        );
    }

    private AdminRole convertRole(String selectedRole) {
        if (selectedRole == null) {
            return null;
        }

        return switch (selectedRole) {
            case "Admin" -> AdminRole.ADMIN;
            case "Manager" -> AdminRole.MANAGER;
            default -> null;
        };
    }

    @FXML
    private void backToWelcome() {
        SceneNavigator.switchTo(
                "/views/kiosk/WelcomeView.fxml"
        );
    }

    private void showSuccess(
            String administratorName,
            AdminRole role
    ) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Login Successful");
        alert.setHeaderText(
                "Welcome, " + administratorName
        );
        alert.setContentText(
                "You are signed in with the "
                        + role + " role."
        );
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Admin Login Failed");
        alert.setHeaderText("Login Error");
        alert.setContentText(message);
        alert.showAndWait();
    }
}