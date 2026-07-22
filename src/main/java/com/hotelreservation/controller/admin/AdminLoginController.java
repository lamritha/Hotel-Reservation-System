package com.hotelreservation.controller.admin;

import com.hotelreservation.util.SceneNavigator;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class AdminLoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private ComboBox<String> roleComboBox;

    @FXML
    private void initialize() {
        roleComboBox.setItems(FXCollections.observableArrayList("Admin", "Manager"));
    }

    @FXML
    private void login() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String role = roleComboBox.getValue();

        if (username.isEmpty() || password.isEmpty() || role == null) {
            showError("Please enter username, password, and select a role.");
            return;
        }

        if (username.equals("admin") && password.equals("admin123")) {
            SceneNavigator.switchTo("/views/admin/AdminDashboardView.fxml");
        } else {
            showError("Invalid username or password.");
        }
    }

    @FXML
    private void backToWelcome() {
        SceneNavigator.switchTo("/views/kiosk/WelcomeView.fxml");
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Admin Login Failed");
        alert.setHeaderText("Login Error");
        alert.setContentText(message);
        alert.showAndWait();
    }
}