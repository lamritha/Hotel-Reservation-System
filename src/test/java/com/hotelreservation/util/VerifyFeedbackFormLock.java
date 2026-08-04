package com.hotelreservation.util;

import com.hotelreservation.config.AppConfig;
import com.hotelreservation.controller.kiosk.FeedbackController;
import com.hotelreservation.model.AdminRole;
import com.hotelreservation.model.PaymentMethod;
import com.hotelreservation.model.PaymentType;
import com.hotelreservation.model.Reservation;
import com.hotelreservation.model.Room;
import com.hotelreservation.repository.RoomRepository;
import com.hotelreservation.service.BillingPaymentService;
import com.hotelreservation.service.ReservationManagementService;
import com.hotelreservation.service.ReservationRequest;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Loads FeedbackView, submits feedback, verifies form lock, Finish → Welcome.
 */
public class VerifyFeedbackFormLock {

    public static void main(String[] args) throws Exception {
        CountDownLatch fxReady = new CountDownLatch(1);
        Platform.startup(fxReady::countDown);
        if (!fxReady.await(20, TimeUnit.SECONDS)) {
            throw new IllegalStateException("JavaFX toolkit failed to start");
        }

        AtomicReference<String> failure = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                runUiVerification();
            } catch (Throwable t) {
                failure.set(rootMessage(t));
                t.printStackTrace();
            } finally {
                done.countDown();
            }
        });

        if (!done.await(120, TimeUnit.SECONDS)) {
            throw new IllegalStateException("UI verification timed out");
        }

        if (failure.get() != null) {
            System.out.println("FAIL | " + failure.get());
            System.exit(1);
        }
        System.out.println("=== VERIFY RESULT: ALL PASSED ===");
        Platform.exit();
        AppConfig.shutdown();
    }

    private static void runUiVerification() throws Exception {
        AppConfig.initialize();
        var login = AppConfig.getAuthenticationService()
                .authenticate(
                        "admin",
                        "ChangeMe!2026Admin",
                        AdminRole.ADMIN
                );
        if (!login.successful()) {
            throw new IllegalStateException("login failed: " + login.message());
        }

        ReservationManagementService reservations =
                AppConfig.getReservationManagementService();
        BillingPaymentService billing = field("BILLING_PAYMENT_SERVICE");
        RoomRepository rooms = field("ROOM_REPOSITORY");

        LocalDate in = LocalDate.of(2028, 5, 1);
        LocalDate out = LocalDate.of(2028, 5, 3);
        long stamp = System.currentTimeMillis();
        Room room = rooms.findAvailableRoomsByTypeAndDates(
                null, in, out, 1
        ).getFirst();

        Reservation reservation = reservations.createPhoneReservation(
                new ReservationRequest(
                        "Lock",
                        "Form",
                        "lock.form" + stamp + "@email.com",
                        String.format("416-555-%04d", stamp % 10000),
                        "1 Lock Form St",
                        in,
                        out,
                        1,
                        0,
                        List.of(room.getRoomId())
                )
        );
        Long resId = reservation.getReservationId();
        var summary = billing.getSummary(resId);
        billing.processPayment(
                resId,
                summary.outstanding(),
                PaymentMethod.CARD,
                PaymentType.FINAL_PAYMENT,
                "settle for feedback lock verify"
        );
        billing.checkIn(resId);
        billing.checkout(resId);

        BookingSession.reset();
        BookingSession.setFeedbackReservationId("RES-" + resId);

        Stage stage = new Stage();
        SceneNavigator.setMainStage(stage);
        stage.setScene(new Scene(new StackPane(), 1100, 700));

        FXMLLoader loader = new FXMLLoader(
                VerifyFeedbackFormLock.class.getResource(
                        "/views/kiosk/FeedbackView.fxml"
                )
        );
        loader.setControllerFactory(AppConfig::createController);
        Parent root = loader.load();
        FeedbackController controller = loader.getController();
        stage.getScene().setRoot(root);
        stage.show();

        Button back = (Button) root.lookup("#backButton");
        Button submit = (Button) root.lookup("#submitButton");
        Button finish = (Button) root.lookup("#finishButton");
        @SuppressWarnings("unchecked")
        ComboBox<Integer> rating =
                (ComboBox<Integer>) root.lookup("#ratingComboBox");
        TextArea comment = (TextArea) root.lookup("#commentArea");

        if (back == null || submit == null || finish == null
                || rating == null || comment == null) {
            throw new IllegalStateException("FXML lookups failed");
        }

        rating.setValue(5);
        comment.setText("Automated lock-form verification");

        scheduleAlertDismiss();
        submit.fire();

        boolean locked =
                back.isDisabled()
                        && submit.isDisabled()
                        && rating.isDisabled()
                        && comment.isDisabled()
                        && !finish.isDisabled();

        System.out.println(
                "after_submit backDisabled=" + back.isDisabled()
                        + " submitDisabled=" + submit.isDisabled()
                        + " ratingDisabled=" + rating.isDisabled()
                        + " commentDisabled=" + comment.isDisabled()
                        + " finishDisabled=" + finish.isDisabled()
        );

        if (!locked) {
            throw new IllegalStateException(
                    "Form did not lock as expected after submit"
            );
        }

        finish.fire();
        String path = SceneNavigator.getCurrentFxmlPath();
        System.out.println("after_finish path=" + path);
        if (!"/views/kiosk/WelcomeView.fxml".equals(path)) {
            throw new IllegalStateException(
                    "Finish did not navigate to WelcomeView, got: " + path
            );
        }

        System.out.println("PASS | feedback form lock + Finish → Welcome");
        stage.close();
    }

    private static void scheduleAlertDismiss() {
        Thread dismisser = new Thread(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            Platform.runLater(() -> {
                for (Window window : Window.getWindows()) {
                    if (!(window instanceof Stage stage)
                            || !stage.isShowing()) {
                        continue;
                    }
                    if (stage.getModality() == Modality.NONE
                            && stage.getOwner() == null) {
                        continue;
                    }
                    if (stage.getScene() == null
                            || !(stage.getScene().getRoot()
                            instanceof DialogPane pane)) {
                        continue;
                    }
                    for (var button : pane.getButtonTypes()) {
                        var node = pane.lookupButton(button);
                        if (node instanceof Button ok) {
                            ok.fire();
                            return;
                        }
                    }
                }
            });
        }, "alert-dismisser");
        dismisser.setDaemon(true);
        dismisser.start();
    }

    @SuppressWarnings("unchecked")
    private static <T> T field(String name) throws Exception {
        Field f = AppConfig.class.getDeclaredField(name);
        f.setAccessible(true);
        return (T) f.get(null);
    }

    private static String rootMessage(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null) {
            c = c.getCause();
        }
        return c.getMessage() == null ? t.toString() : c.getMessage();
    }
}
