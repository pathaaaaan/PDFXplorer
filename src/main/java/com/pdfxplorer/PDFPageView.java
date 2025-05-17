package com.pdfxplorer;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A custom PDF page view that supports highlighting.
 * Each page is contained in its own StackPane to allow
 * overlaying highlights directly on the page.
 */
public class PDFPageView extends StackPane {
    private int pageIndex;
    private ImageView pageImageView;
    private List<Rectangle> highlights = new ArrayList<>();
    private float currentZoom = 1.0f;
    private double pageWidth;
    private double pageHeight;
    private static final float PDF_UNITS_PER_POINT = 72.0f;

    // Text selection
    private Point2D selectionStart;
    private Rectangle selectionRect;
    private List<TextPosition> pageTextPositions = new ArrayList<>();
    private PDDocument document;
    private double displayScale;

    private int rotation = 0;

    public PDFPageView(int pageIndex) {
        this.pageIndex = pageIndex;
        this.setAlignment(Pos.CENTER);
        this.setStyle("-fx-background-color: white;");
        initializeTextSelection();
    }

    private void initializeTextSelection() {
        selectionRect = new Rectangle();
        selectionRect.setFill(Color.DODGERBLUE.deriveColor(0, 1, 1, 0.3));
        selectionRect.setStroke(Color.DODGERBLUE);
        selectionRect.setVisible(false);
        selectionRect.setMouseTransparent(true);

        this.setOnMousePressed(this::handleMousePressed);
        this.setOnMouseDragged(this::handleMouseDragged);
        this.setOnMouseReleased(this::handleMouseReleased);
        this.setCursor(Cursor.TEXT);
    }

    private void handleMousePressed(MouseEvent event) {
        // Clear previous selection
        selectionRect.setVisible(false);
        pageTextPositions.clear();

        // Get mouse position relative to the ImageView
        Point2D imageViewPoint = pageImageView.sceneToLocal(event.getSceneX(), event.getSceneY());
        selectionStart = imageViewPoint;

        // Create new selection rectangle at the correct position
        selectionRect.setX(imageViewPoint.getX());
        selectionRect.setY(imageViewPoint.getY());
        selectionRect.setWidth(0);
        selectionRect.setHeight(0);

        if (!this.getChildren().contains(selectionRect)) {
            this.getChildren().add(selectionRect);
        }
    }

    private void handleMouseDragged(MouseEvent event) {
        // Get mouse position relative to the ImageView
        Point2D imageViewPoint = pageImageView.sceneToLocal(event.getSceneX(), event.getSceneY());

        // Update selection rectangle
        double x = Math.min(imageViewPoint.getX(), selectionStart.getX());
        double y = Math.min(imageViewPoint.getY(), selectionStart.getY());
        double width = Math.abs(imageViewPoint.getX() - selectionStart.getX());
        double height = Math.abs(imageViewPoint.getY() - selectionStart.getY());

        selectionRect.setX(x);
        selectionRect.setY(y);
        selectionRect.setWidth(width);
        selectionRect.setHeight(height);
        selectionRect.setVisible(true);
    }

    private void handleMouseReleased(MouseEvent event) {
        if (selectionRect.isVisible()) {
            copySelectedText();
        }
    }

    private void copySelectedText() {
        if (document == null)
            return;

        try {
            PDFTextStripper stripper = new PDFTextStripper() {
                @Override
                protected void processTextPosition(TextPosition text) {
                    // Convert PDF coordinates to view coordinates
                    double scaleFactor = displayScale * 2; // Account for the DPI scaling
                    double viewX = text.getXDirAdj() * scaleFactor;
                    double viewY = (pageHeight / (PDF_UNITS_PER_POINT * 2) - text.getYDirAdj()) * scaleFactor;

                    // Check if the text position is within the selection rectangle
                    if (isPointInSelection(viewX, viewY)) {
                        pageTextPositions.add(text);
                    }
                }
            };

            stripper.setStartPage(pageIndex + 1);
            stripper.setEndPage(pageIndex + 1);
            stripper.getText(document);

            if (!pageTextPositions.isEmpty()) {
                // Sort text positions by their coordinates
                pageTextPositions.sort((a, b) -> {
                    int yCompare = Float.compare(a.getYDirAdj(), b.getYDirAdj());
                    return yCompare != 0 ? -yCompare : Float.compare(a.getXDirAdj(), b.getXDirAdj());
                });

                // Build selected text
                StringBuilder selectedText = new StringBuilder();
                float lastY = pageTextPositions.get(0).getYDirAdj();

                for (TextPosition text : pageTextPositions) {
                    // Add newline if there's significant vertical gap
                    if (Math.abs(text.getYDirAdj() - lastY) > text.getHeight() * 0.5) {
                        selectedText.append("\n");
                        lastY = text.getYDirAdj();
                    }
                    // Add space if there's significant horizontal gap
                    else if (selectedText.length() > 0 && text.getXDirAdj() - lastY > text.getWidth()) {
                        selectedText.append(" ");
                    }
                    selectedText.append(text.getUnicode());
                }

                // Copy to clipboard
                javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString(selectedText.toString().trim());
                clipboard.setContent(content);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isPointInSelection(double x, double y) {
        return x >= selectionRect.getX() && x <= selectionRect.getX() + selectionRect.getWidth() &&
                y >= selectionRect.getY() && y <= selectionRect.getY() + selectionRect.getHeight();
    }

    public void render(PDFRenderer renderer, float zoomLevel, PDDocument doc) throws IOException {
        this.currentZoom = zoomLevel;
        this.document = doc;
        this.getChildren().removeAll(this.getChildren().filtered(node -> !(node instanceof ImageView)));
        highlights.clear();

        // Calculate DPI based on zoom level (72 DPI is the base PDF unit)
        float dpi = PDF_UNITS_PER_POINT * zoomLevel * 2; // Higher base DPI for better quality

        // Render the page
        BufferedImage img = renderer.renderImageWithDPI(pageIndex, dpi);
        Image fxImage = SwingFXUtils.toFXImage(img, null);

        // Store original dimensions
        pageWidth = img.getWidth();
        pageHeight = img.getHeight();

        // Create and configure ImageView
        if (pageImageView == null) {
            pageImageView = new ImageView(fxImage);
            this.getChildren().add(pageImageView);
        } else {
            pageImageView.setImage(fxImage);
        }

        pageImageView.setPreserveRatio(true);
        pageImageView.setSmooth(true);
        pageImageView.setCache(true);

        // Set fit width based on zoom level
        pageImageView.setFitWidth(pageWidth / 2); // Compensate for the higher DPI

        // Calculate display scale for coordinate transformations
        displayScale = pageImageView.getFitWidth() / (pageWidth / 2);
    }

    public void addHighlight(float x, float y, float width, float height) {
        if (pageImageView == null)
            return;

        // Get the current scale of the image view
        double displayScale = pageImageView.getFitWidth() / pageWidth;

        // Convert PDF coordinates (72 units per inch) to screen coordinates
        double scaledX = x * displayScale * 2; // Multiply by 2 to compensate for the DPI scaling
        double scaledY = y * displayScale * 2;
        double scaledWidth = width * displayScale * 2;
        double scaledHeight = height * displayScale * 2;

        Rectangle highlight = new Rectangle(
                scaledX,
                scaledY,
                scaledWidth,
                scaledHeight);

        highlight.setFill(Color.YELLOW.deriveColor(0, 1.0, 1.0, 0.3));
        highlight.setStroke(Color.ORANGE);
        highlight.setStrokeWidth(1.0);
        highlight.setMouseTransparent(true);

        highlights.add(highlight);
        this.getChildren().add(highlight);
    }

    public void clearHighlights() {
        for (Rectangle highlight : highlights) {
            this.getChildren().remove(highlight);
        }
        highlights.clear();
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public void setRotation(int degrees) {
        this.rotation = degrees;
        if (pageImageView != null) {
            pageImageView.setRotate(degrees);

            // Adjust container size based on rotation
            if (rotation % 180 == 90) {
                // For 90 and 270 degrees, swap width and height
                pageImageView.setFitWidth(pageHeight / 2);
                double aspectRatio = pageWidth / pageHeight;
                pageImageView.setFitHeight(pageImageView.getFitWidth() / aspectRatio);
            } else {
                // For 0 and 180 degrees, restore original dimensions
                pageImageView.setFitWidth(pageWidth / 2);
                pageImageView.setFitHeight(-1); // Reset to maintain aspect ratio
            }
        }

        // Update highlight positions
        for (Rectangle highlight : highlights) {
            highlight.setRotate(degrees);
        }
    }

    public int getRotation() {
        return rotation;
    }
}