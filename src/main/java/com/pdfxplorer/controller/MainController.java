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
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import com.pdfxplorer.service.PdfReaderService;
import com.pdfxplorer.model.PdfDocument;

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
    private PdfDocument currentPdfModel;

    @FXML
    public void initialize() {
        openButton.setOnAction(e -> handleOpenPdf());
        searchButton.setOnAction(e -> performSearch());

        zoomInButton.setOnAction(e -> {
            zoomLevel += 0.1f;
            refreshPagesWithZoom();
        });

        zoomOutButton.setOnAction(e -> {
            zoomLevel = Math.max(0.5f, zoomLevel - 0.1f); // prevent too much zoom out
            refreshPagesWithZoom();
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
            try {
                openPdf(selectedFile);
            } catch (Exception ex) {
                showErrorAlert("Error Opening PDF", "Could not open the selected PDF file: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    public void openPdf(File file) {
        try {
            if (document != null) {
                document.close();
            }

            // Use the service to load the document
            document = PdfReaderService.loadDocument(file);

            // Assign the renderer properly
            renderer = new PDFRenderer(document);

            // Create model instance
            currentPdfModel = new PdfDocument(file.getName(), document.getNumberOfPages());

            // Clear and reload all pages
            refreshPagesWithZoom();

            // Scroll to the top
            scrollPane.setVvalue(0);

            // Reset current page
            currentPage = 0;

        } catch (IOException e) {
            showErrorAlert("Error Opening PDF", "Could not open the selected PDF file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void refreshPagesWithZoom() {
        if (document == null || renderer == null) return;

        multiPageContainer.getChildren().clear();

        try {
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                // Adjust rendering DPI based on zoom level
                float dpi = 150 * zoomLevel;
                BufferedImage img = renderer.renderImageWithDPI(i, dpi);
                Image fxImage = SwingFXUtils.toFXImage(img, null);

                ImageView view = new ImageView(fxImage);
                view.setPreserveRatio(true);
                view.setFitWidth(800 * zoomLevel); // Scale width with zoom

                // Add an identifier to track pages
                view.setId("page-" + i);

                multiPageContainer.getChildren().add(view);
            }
        } catch (IOException e) {
            showErrorAlert("Error Rendering PDF", "Could not render the PDF pages: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showPage(int pageIndex) {
        currentPage = pageIndex;
        // For continuous scroll view, we'll scroll to the page
        if (multiPageContainer.getChildren().size() > pageIndex) {
            Platform.runLater(() -> {
                // Get the page view
                ImageView pageView = (ImageView) multiPageContainer.getChildren().get(pageIndex);

                // Calculate its position within the scroll pane
                double pageY = pageView.getBoundsInParent().getMinY();
                double scrollHeight = scrollPane.getContent().getBoundsInLocal().getHeight();
                double scrollPaneHeight = scrollPane.getHeight();

                // Set scroll position
                double vvalue = pageY / (scrollHeight - scrollPaneHeight);
                scrollPane.setVvalue(vvalue);
            });
        }
    }

    private void drawHighlight(float x, float y, float width, float height) {
        // Clear previous highlights
        imageContainer.getChildren().removeIf(node -> node instanceof Rectangle);

        // Create new highlight
        Rectangle highlight = new Rectangle(x, y, width, height);
        highlight.setFill(Color.YELLOW.deriveColor(0, 1.0, 1.0, 0.3));
        highlight.setStroke(Color.ORANGE);

        // Add to container
        imageContainer.getChildren().add(highlight);
    }

    private void performSearch() {
        if (document == null || searchField.getText().isEmpty()) return;

        // Clear previous highlights
        imageContainer.getChildren().removeIf(node -> node instanceof Rectangle);

        String query = searchField.getText().toLowerCase();
        try {
            boolean found = false;
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                SearchHighlighter highlighter = new SearchHighlighter(query);
                highlighter.setStartPage(i + 1);
                highlighter.setEndPage(i + 1);
                highlighter.getText(document);

                if (highlighter.isFound()) {
                    showPage(i);
                    found = true;

                    Platform.runLater(() -> {
                        // Calculate scaling based on zoom
                        double dpi = 150 * zoomLevel;
                        double scale = dpi / 72.0;

                        float x = (float) (highlighter.getX() * scale);
                        float y = (float) (highlighter.getY() * scale);
                        float w = (float) (highlighter.getWidth() * scale);
                        float h = (float) (highlighter.getHeight() * scale);

                        // Get page view to calculate offset
                        ImageView pageView = (ImageView) multiPageContainer.getChildren().get(currentPage);
                        double pageY = pageView.getBoundsInParent().getMinY();

                        // Apply page offset to highlight coordinates
                        Rectangle highlight = new Rectangle(x, pageY + y - h, w, h);
                        highlight.setFill(Color.YELLOW.deriveColor(0, 1.0, 1.0, 0.3));
                        highlight.setStroke(Color.ORANGE);

                        // Add highlight as a child of the VBox (so it scrolls with content)
                        imageContainer.getChildren().add(highlight);
                    });

                    break;
                }
            }

            if (!found) {
                // Show not found message
                showInfoAlert("Search Results", "No matches found for \"" + query + "\"");
            }
        } catch (IOException e) {
            showErrorAlert("Search Error", "Error while searching: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void zoomInFromShortcut() {
        zoomLevel += 0.1f;
        refreshPagesWithZoom();
    }

    public void zoomOutFromShortcut() {
        zoomLevel = Math.max(0.5f, zoomLevel - 0.1f);
        refreshPagesWithZoom();
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

    private void showErrorAlert(String title, String content) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showInfoAlert(String title, String content) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}