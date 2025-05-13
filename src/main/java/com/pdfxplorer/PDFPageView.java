package com.pdfxplorer;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

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
    private float zoomLevel = 1.0f;

    public PDFPageView(int pageIndex) {
        this.pageIndex = pageIndex;
        this.setAlignment(Pos.CENTER);
    }

    public void render(PDFRenderer renderer, float zoomLevel) throws IOException {
        this.zoomLevel = zoomLevel;
        this.getChildren().clear();
        highlights.clear();

        // Calculate DPI based on zoom level
        float dpi = 150 * zoomLevel;

        // Render the page
        BufferedImage img = renderer.renderImageWithDPI(pageIndex, dpi);
        Image fxImage = SwingFXUtils.toFXImage(img, null);

        pageImageView = new ImageView(fxImage);
        pageImageView.setPreserveRatio(true);
        pageImageView.setFitWidth(800);

        this.getChildren().add(pageImageView);
    }

    public void addHighlight(float x, float y, float width, float height) {
        // Convert PDF coordinates to page view coordinates
        float dpi = 150 * zoomLevel;
        float scale = dpi / 72.0f;

        Rectangle highlight = new Rectangle(x * scale, y * scale, width * scale, height * scale);
        highlight.setFill(Color.YELLOW.deriveColor(0, 1.0, 1.0, 0.3));
        highlight.setStroke(Color.ORANGE);

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
}