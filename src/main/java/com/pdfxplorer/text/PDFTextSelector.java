package com.pdfxplorer.text;

import javafx.geometry.Point2D;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import java.util.ArrayList;
import java.util.List;

public class PDFTextSelector {
    private Rectangle selectionRect;
    private List<Text> selectedTextNodes;
    private Point2D startPoint;

    public static class TextBounds {
        private final double x;
        private final double y;
        private final double width;
        private final double height;
        private final String text;

        public TextBounds(double x, double y, double width, double height, String text) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.text = text;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getWidth() {
            return width;
        }

        public double getHeight() {
            return height;
        }

        public String getText() {
            return text;
        }
    }

    public PDFTextSelector() {
        selectedTextNodes = new ArrayList<>();
    }

    public void startSelection(Point2D point) {
        startPoint = point;
        selectionRect = new Rectangle(point.getX(), point.getY(), 0, 0);
        selectionRect.getStyleClass().add("text-selection");
        selectedTextNodes.clear();
    }

    public void updateSelection(Point2D currentPoint) {
        if (startPoint == null)
            return;

        double width = currentPoint.getX() - startPoint.getX();
        double height = currentPoint.getY() - startPoint.getY();

        // Update selection rectangle
        selectionRect.setX(width < 0 ? currentPoint.getX() : startPoint.getX());
        selectionRect.setY(height < 0 ? currentPoint.getY() : startPoint.getY());
        selectionRect.setWidth(Math.abs(width));
        selectionRect.setHeight(Math.abs(height));
    }

    public void endSelection() {
        startPoint = null;
    }

    public Rectangle getSelectionRectangle() {
        return selectionRect;
    }

    public void updateSelectedNodes(List<Text> pageTextNodes) {
        selectedTextNodes.clear();
        if (selectionRect == null)
            return;

        for (Text textNode : pageTextNodes) {
            if (textNode.getBoundsInParent().intersects(selectionRect.getBoundsInParent())) {
                selectedTextNodes.add(textNode);
            }
        }
    }

    public String getSelectedText() {
        StringBuilder text = new StringBuilder();
        double lastY = -1;

        // Sort text nodes by position
        selectedTextNodes.sort((a, b) -> {
            double yDiff = a.getBoundsInParent().getMinY() - b.getBoundsInParent().getMinY();
            if (Math.abs(yDiff) < 5) { // Same line threshold
                return Double.compare(a.getBoundsInParent().getMinX(), b.getBoundsInParent().getMinX());
            }
            return Double.compare(a.getBoundsInParent().getMinY(), b.getBoundsInParent().getMinY());
        });

        // Build text with proper line breaks
        for (Text node : selectedTextNodes) {
            double currentY = node.getBoundsInParent().getMinY();
            if (lastY != -1 && Math.abs(currentY - lastY) > 5) {
                text.append("\n");
            }
            text.append(node.getText());
            lastY = currentY;
        }

        return text.toString();
    }

    public List<TextBounds> getSelectedTextBounds() {
        List<TextBounds> bounds = new ArrayList<>();
        for (Text node : selectedTextNodes) {
            bounds.add(new TextBounds(
                    node.getBoundsInParent().getMinX(),
                    node.getBoundsInParent().getMinY(),
                    node.getBoundsInParent().getWidth(),
                    node.getBoundsInParent().getHeight(),
                    node.getText()));
        }
        return bounds;
    }

    public void clearSelection() {
        if (selectionRect != null) {
            selectionRect = null;
        }
        selectedTextNodes.clear();
        startPoint = null;
    }
}