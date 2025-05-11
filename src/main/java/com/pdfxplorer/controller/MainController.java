package com.pdfxplorer.controller;
import javafx.scene.layout.VBox;
import org.apache.pdfbox.text.PDFTextStripper;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.Scene;
import com.pdfxplorer.util.SearchHighlighter;
import javafx.application.Platform;
// JavaFX
import javafx.scene.layout.StackPane;

import javafx.scene.control.ScrollPane;
// PDFBox
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import javafx.stage.FileChooser;
import java.io.File;
// For highlight region (basic version)
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;

// Optional — for Rectangle2D if you use it:
import java.awt.geom.Rectangle2D;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class MainController {
    @FXML private ScrollPane scrollPane;
    @FXML private VBox multiPageContainer;
    @FXML private Button openButton;
    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private Button zoomInButton;
    @FXML private Button zoomOutButton;
    @FXML private ToggleButton darkModeToggle;
    @FXML private StackPane imageContainer;
    private float zoomLevel = 1.0f;  // 1.0 = 100%, 1.5 = 150%

    private long lastScrollTime = 0;
    private PDDocument document;
    private PDFRenderer renderer;
    private int currentPage = 0;

    @FXML
    public void initialize() {
//        openButton.setOnAction(e -> handleOpenPdf());
        openButton.setOnAction(e -> handleOpenPdf()); // ✅ Correct
        searchButton.setOnAction(e -> performSearch());

        zoomInButton.setOnAction(e -> {
            zoomLevel += 0.1f;
            showPage(currentPage);
        });

        zoomOutButton.setOnAction(e -> {
            zoomLevel = Math.max(0.5f, zoomLevel - 0.1f); // prevent too much zoom out
            showPage(currentPage);
        });

        darkModeToggle.setOnAction(e -> toggleTheme());



    }

    @FXML
    private void handleOpenPdf() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open PDF File");

        // Limit to PDF files
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("PDF files (*.pdf)", "*.pdf");
        fileChooser.getExtensionFilters().add(extFilter);

        File selectedFile = fileChooser.showOpenDialog(scrollPane.getScene().getWindow());

        if (selectedFile != null) {
            openPdf(selectedFile);
        }
    }

    public void openPdf(File file) {
        try {
            if (document != null) {
                document.close();
            }

            document = PDDocument.load(file);
            PDFRenderer renderer = new PDFRenderer(document);
            multiPageContainer.getChildren().clear();

            for (int i = 0; i < document.getNumberOfPages(); i++) {
                BufferedImage img = renderer.renderImageWithDPI(i, 150);
                Image fxImage = SwingFXUtils.toFXImage(img, null);

                ImageView view = new ImageView(fxImage);
                view.setPreserveRatio(true);
                view.setFitWidth(800);

                multiPageContainer.getChildren().add(view);
            }

            scrollPane.setVvalue(0);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showPage(int pageIndex) {
        currentPage = pageIndex;
        // No action needed for continuous scroll since all pages are rendered
    }



    private void drawHighlight(float x, float y, float width, float height) {
        javafx.scene.shape.Rectangle highlight = new javafx.scene.shape.Rectangle(x, y, width, height);
        highlight.setFill(javafx.scene.paint.Color.YELLOW.deriveColor(0, 1.0, 1.0, 0.3));
        highlight.setStroke(javafx.scene.paint.Color.ORANGE);
        imageContainer.getChildren().add(highlight);
    }


    private void performSearch() {
        if (document == null || searchField.getText().isEmpty()) return;

        String query = searchField.getText().toLowerCase();
        try {
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                SearchHighlighter highlighter = new SearchHighlighter(query);
                highlighter.setStartPage(i + 1);
                highlighter.setEndPage(i + 1);
                highlighter.getText(document);

                if (highlighter.isFound()) {
                    showPage(i);

                    Platform.runLater(() -> {
                        double dpi = 150 * zoomLevel;
                        double scale = dpi / 72.0;

                        float x = (float) (highlighter.getX() * scale);
                        float y = (float) (highlighter.getY() * scale);
                        float w = (float) (highlighter.getWidth() * scale);
                        float h = (float) (highlighter.getHeight() * scale);

                        imageContainer.getChildren().removeIf(node -> node instanceof javafx.scene.shape.Rectangle);
                        javafx.scene.shape.Rectangle highlight = new javafx.scene.shape.Rectangle(x, y - h, w, h);
                        highlight.setFill(javafx.scene.paint.Color.YELLOW.deriveColor(0, 1.0, 1.0, 0.3));
                        highlight.setStroke(javafx.scene.paint.Color.ORANGE);
                        imageContainer.getChildren().add(highlight);
                    });

                    return;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void zoomInFromShortcut() {
        zoomLevel += 0.1f;
        showPage(currentPage);
    }

    public void zoomOutFromShortcut() {
        zoomLevel = Math.max(0.5f, zoomLevel - 0.1f);
        showPage(currentPage);
    }

    private boolean darkModeEnabled = false;

    private void toggleTheme() {
        Scene scene = scrollPane.getScene();  // Use ScrollPane's scene for theme
        if (scene == null) return;

        if (darkModeEnabled) {
            scene.getStylesheets().clear(); // Remove dark mode
        } else {
            scene.getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());
        }
        darkModeEnabled = !darkModeEnabled;
    }
}