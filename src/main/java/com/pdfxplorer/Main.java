package com.pdfxplorer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.pdfxplorer.controller.MainController;
public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/layout.fxml"));
        Scene scene = new Scene(loader.load(), 800, 600);
        stage.setTitle("PDFXplorer");
        stage.setScene(scene);
        stage.show();
        MainController controller = loader.getController(); // Extract once
        scene.setOnKeyPressed(event -> {
            if (event.isControlDown() || event.isMetaDown()) {
                switch (event.getCode()) {
                    case PLUS, EQUALS -> controller.zoomInFromShortcut();
                    case MINUS -> controller.zoomOutFromShortcut();
                }
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}