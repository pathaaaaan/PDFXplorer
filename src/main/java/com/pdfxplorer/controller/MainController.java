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
import com.pdfxplorer.PDFPageView;

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
    private java.util.List<PDFPageView> pageViews = new java.util.ArrayList<>();

    @FXML
    public void initialize() {
        openButton.setOnAction(e -> handleOpenPdf());
        searchButton.setOnAction(e -> performSearch());

        zoomInButton.setOnAction(e -> {
            zoomLevel += 0.1f;
            renderPagesWithZoom();
        });

        zoomOutButton.setOnAction(e -> {
            zoomLevel = Math.max(0.5f, zoomLevel - 0.1f); // prevent too much zoom out
            renderPagesWithZoom();
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
            renderer = new PDFRenderer(document); // Fixed: Properly assign to class variable
            renderPagesWithZoom();

            scrollPane.setVvalue(0);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void renderPagesWithZoom() {
        if (document == null || renderer == null) return;

        try {
            multiPageContainer.getChildren().clear();
            pageViews.clear();

            for (int i = 0; i < document.getNumberOfPages(); i++) {
                PDFPageView pageView = new PDFPageView(i);
                pageView.render(renderer, zoomLevel);

                multiPageContainer.getChildren().add(pageView);
                pageViews.add(pageView);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showPage(int pageIndex) {
        currentPage = pageIndex;
        // For continuous scroll mode, just scroll to the page
        if (pageViews.size() > pageIndex) {
            pageViews.get(pageIndex).requestFocus();
        }
    }

    private void clearHighlights() {
        // Clear highlights on all pages
        for (PDFPageView pageView : pageViews) {
            pageView.clearHighlights();
        }
    }

    private void drawHighlight(float x, float y, float width, float height) {
        Rectangle highlight = new Rectangle(x, y, width, height);
        highlight.setFill(Color.YELLOW.deriveColor(0, 1.0, 1.0, 0.3));
        highlight.setStroke(Color.ORANGE);
        imageContainer.getChildren().add(highlight);
    }

    private void performSearch() {
        if (document == null || searchField.getText().isEmpty()) return;

        clearHighlights();
        String query = searchField.getText().toLowerCase();

        try {
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                SearchHighlighter highlighter = new SearchHighlighter(query);
                highlighter.setStartPage(i + 1);
                highlighter.setEndPage(i + 1);
                highlighter.getText(document);

                if (highlighter.isFound()) {
                    final int foundPage = i;
                    showPage(foundPage);

                    Platform.runLater(() -> {
                        // Add highlight to the specific page
                        if (foundPage < pageViews.size()) {
                            PDFPageView pageView = pageViews.get(foundPage);
                            pageView.addHighlight(
                                    highlighter.getX(),
                                    highlighter.getY() - highlighter.getHeight(),
                                    highlighter.getWidth(),
                                    highlighter.getHeight()
                            );

                            // Scroll to this page
                            scrollPane.setVvalue(
                                    (double) foundPage / Math.max(1, document.getNumberOfPages() - 1)
                            );
                        }
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
        renderPagesWithZoom();
    }

    public void zoomOutFromShortcut() {
        zoomLevel = Math.max(0.5f, zoomLevel - 0.1f);
        renderPagesWithZoom();
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