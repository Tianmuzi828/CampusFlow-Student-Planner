package com.campusflow.app;

import com.campusflow.app.data.CampusFlowRepository;
import com.campusflow.app.data.DataAccessException;
import com.campusflow.app.data.DatabaseManager;
import com.campusflow.app.i18n.I18n;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class CampusFlowApplication extends Application {
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        initializeLanguage();
        Scene scene = new Scene(loadMainView(), 1180, 760);
        stage.setTitle(I18n.text("app.title"));
        stage.setMinWidth(1040);
        stage.setMinHeight(680);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

    public static void reloadMainView() {
        if (primaryStage == null) {
            return;
        }
        try {
            primaryStage.getScene().setRoot(loadMainView());
            primaryStage.setTitle(I18n.text("app.title"));
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not reload the CampusFlow interface.", exception
            );
        }
    }

    private static Parent loadMainView() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                CampusFlowApplication.class.getResource("main-view.fxml"),
                I18n.bundle()
        );
        return loader.load();
    }

    private void initializeLanguage() {
        try {
            DatabaseManager databaseManager = new DatabaseManager();
            databaseManager.initialize();
            I18n.setLanguage(
                    new CampusFlowRepository(databaseManager)
                            .findSettings().getLanguage()
            );
        } catch (DataAccessException exception) {
            // English remains available if settings cannot be loaded yet.
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
