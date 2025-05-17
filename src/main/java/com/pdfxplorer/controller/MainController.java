package com.pdfxplorer.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.pdfxplorer.pdf.PythonPdfRenderer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.geometry.Insets;
import javafx.scene.Node;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.ScrollPane;
import javafx.geometry.Bounds;
import javafx.scene.Scene;

public class MainController {
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private VBox pdfContainer;
    @FXML
    private StackPane contentContainer;
    @FXML
    private Button openButton;
    @FXML
    private ComboBox<String> zoomLevelComboBox;
    @FXML
    private ToggleButton fitWidthToggle;
    @FXML
    private ToggleButton fitPageToggle;
    @FXML
    private Button rotateLeftButton;
    @FXML
    private Button rotateRightButton;
    @FXML
    private ProgressIndicator loadingIndicator;
    @FXML
    private Label pageInfoLabel;
    @FXML
    private Label zoomLabel;
    @FXML
    private ImageView pdfImageView;
    @FXML
    private Button prevPageButton;
    @FXML
    private Button nextPageButton;
    @FXML
    private TabPane sidebarTabPane;
    @FXML
    private VBox thumbnailContainer;
    @FXML
    private Label fileNameLabel;
    @FXML
    private Label pageSizeLabel;
    @FXML
    private Label fileSizeLabel;
    @FXML
    private Label createdDateLabel;
    @FXML
    private Label modifiedDateLabel;
    @FXML
    private ListView<String> recentFilesListView;

    private PythonPdfRenderer pdfRenderer;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static final double[] ZOOM_LEVELS = { 0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 2.0, 3.0, 4.0 };
    private List<String> recentFiles = new ArrayList<>();
    private static final int MAX_RECENT_FILES = 5;

    private String currentPdfPath;
    private int currentPage = 0;
    private int totalPages = 0;
    private double currentZoom = 1.0;

    private ObservableList<String> recentFilesList = FXCollections.observableArrayList();

    private Map<Integer, VBox> pageContainers = new HashMap<>();
    private Map<Integer, ImageView> thumbnailViews = new HashMap<>();
    private VBox currentSelectedThumbnail = null;
    private double lastScrollPosition = 0;
    private boolean isScrolling = false;

    @FXML
    public void initialize() {
        // Initialize PDF renderer
        pdfRenderer = new PythonPdfRenderer();

        // Configure scroll pane
        scrollPane.setFitToWidth(false);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        // Add scroll listener for page detection
        scrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
            if (!isScrolling) {
                detectVisiblePage();
            }
        });

        // Setup keyboard shortcuts
        setupKeyboardShortcuts();

        // Configure zoom controls
        zoomLevelComboBox.getItems().addAll(
                "25%", "50%", "75%", "100%", "125%", "150%", "200%", "300%", "400%");
        zoomLevelComboBox.setValue("100%");
        zoomLevelComboBox.setOnAction(event -> {
            if (event.getSource() instanceof ComboBox) {
                handleZoomLevelChange();
            }
        });

        // Configure navigation buttons with explicit event handlers
        prevPageButton.setOnAction(event -> {
            System.out.println("Previous button clicked, current page: " + currentPage);
            if (currentPage > 0) {
                navigateToPage(currentPage - 1);
            }
        });

        nextPageButton.setOnAction(event -> {
            System.out.println("Next button clicked, current page: " + currentPage);
            if (currentPage < totalPages - 1) {
                navigateToPage(currentPage + 1);
            }
        });

        // Configure fit buttons
        fitWidthToggle.setOnAction(event -> handleFitWidth());
        fitPageToggle.setOnAction(event -> handleFitPage());

        // Configure rotation buttons
        rotateLeftButton.setOnAction(event -> handleRotate(-90));
        rotateRightButton.setOnAction(event -> handleRotate(90));

        // File menu handlers
        openButton.setOnAction(event -> handleOpenPdf());

        // Initial button states
        updateNavigationButtons();

        // Initialize sidebar
        initializeSidebar();

        // Initialize recent files list
        recentFilesListView.setItems(recentFilesList);
        recentFilesListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selectedFile = recentFilesListView.getSelectionModel().getSelectedItem();
                if (selectedFile != null) {
                    openPdf(new File(selectedFile));
                }
            }
        });

        // Make container focusable for keyboard shortcuts
        contentContainer.setFocusTraversable(true);

        // Add window resize listener to maintain fit modes
        setupResizeListener();
    }

    private void initializeSidebar() {
        sidebarTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        thumbnailContainer.setSpacing(10);
    }

    private void setupKeyboardShortcuts() {
        // Add key event handler to the scene
        Platform.runLater(() -> {
            Scene scene = contentContainer.getScene();
            if (scene != null) {
                scene.setOnKeyPressed(event -> {
                    System.out.println("Key pressed: " + event.getCode() +
                            ", Ctrl held: " + event.isControlDown() +
                            ", Meta (Command) held: " + event.isMetaDown());

                    // Check for either Control or Command (Meta) key
                    boolean isModifierDown = event.isControlDown() || event.isMetaDown();

                    if (isModifierDown) {
                        switch (event.getCode()) {
                            case EQUALS:
                            case PLUS:
                            case ADD:
                                System.out.println("Zoom in shortcut detected");
                                zoomIn();
                                event.consume();
                                break;
                            case MINUS:
                            case SUBTRACT:
                                System.out.println("Zoom out shortcut detected");
                                zoomOut();
                                event.consume();
                                break;
                            case DIGIT0:
                            case NUMPAD0:
                                System.out.println("Reset zoom shortcut detected");
                                resetZoom();
                                event.consume();
                                break;
                        }
                    } else {
                        switch (event.getCode()) {
                            case LEFT:
                            case PAGE_UP:
                                if (!prevPageButton.isDisabled()) {
                                    navigateToPage(currentPage - 1);
                                }
                                event.consume();
                                break;
                            case RIGHT:
                            case PAGE_DOWN:
                                if (!nextPageButton.isDisabled()) {
                                    navigateToPage(currentPage + 1);
                                }
                                event.consume();
                                break;
                            case HOME:
                                navigateToPage(0);
                                event.consume();
                                break;
                            case END:
                                navigateToPage(totalPages - 1);
                                event.consume();
                                break;
                        }
                    }
                });
            } else {
                System.err.println("Scene is null, keyboard shortcuts won't work!");
            }
        });

        // Also add the handler to the container itself for redundancy
        contentContainer.setOnKeyPressed(event -> {
            System.out.println("Container key pressed: " + event.getCode() +
                    ", Ctrl held: " + event.isControlDown() +
                    ", Meta (Command) held: " + event.isMetaDown());

            // Check for either Control or Command (Meta) key
            boolean isModifierDown = event.isControlDown() || event.isMetaDown();

            if (isModifierDown) {
                switch (event.getCode()) {
                    case EQUALS:
                    case PLUS:
                    case ADD:
                        System.out.println("Container zoom in shortcut detected");
                        zoomIn();
                        event.consume();
                        break;
                    case MINUS:
                    case SUBTRACT:
                        System.out.println("Container zoom out shortcut detected");
                        zoomOut();
                        event.consume();
                        break;
                    case DIGIT0:
                    case NUMPAD0:
                        System.out.println("Container reset zoom shortcut detected");
                        resetZoom();
                        event.consume();
                        break;
                }
            }
        });

        // Make sure the container can receive focus
        contentContainer.setFocusTraversable(true);
    }

    private void zoomIn() {
        System.out.println("zoomIn() called - Current zoom: " + currentZoom);

        // Find next zoom level
        double nextZoom = ZOOM_LEVELS[0]; // Default to smallest if nothing else found
        for (double zoomLevel : ZOOM_LEVELS) {
            if (zoomLevel > currentZoom) {
                nextZoom = zoomLevel;
                break;
            }
        }

        System.out.println("Next zoom level selected: " + nextZoom);
        if (nextZoom != currentZoom) {
            currentZoom = nextZoom;
            System.out.println("Applying new zoom level: " + currentZoom);
            Platform.runLater(() -> {
                updateZoomComboBox();
                renderAllPages();
            });
        }
    }

    private void zoomOut() {
        System.out.println("zoomOut() called - Current zoom: " + currentZoom);

        // Find previous zoom level
        double prevZoom = ZOOM_LEVELS[ZOOM_LEVELS.length - 1]; // Default to largest if nothing else found
        for (int i = ZOOM_LEVELS.length - 1; i >= 0; i--) {
            if (ZOOM_LEVELS[i] < currentZoom) {
                prevZoom = ZOOM_LEVELS[i];
                break;
            }
        }

        System.out.println("Previous zoom level selected: " + prevZoom);
        if (prevZoom != currentZoom) {
            currentZoom = prevZoom;
            System.out.println("Applying new zoom level: " + currentZoom);
            Platform.runLater(() -> {
                updateZoomComboBox();
                renderAllPages();
            });
        }
    }

    private void resetZoom() {
        System.out.println("resetZoom() called - Current zoom: " + currentZoom);
        currentZoom = 1.0;
        System.out.println("Resetting zoom to: " + currentZoom);
        Platform.runLater(() -> {
            updateZoomComboBox();
            renderAllPages();
        });
    }

    private void updateZoomComboBox() {
        String zoomText = String.format("%.0f%%", currentZoom * 100);
        System.out.println("Updating zoom combo box to: " + zoomText);
        zoomLevelComboBox.setValue(zoomText);
    }

    @FXML
    private void handleOpenPdf() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open PDF File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF files (*.pdf)", "*.pdf"));

        File selectedFile = fileChooser.showOpenDialog(contentContainer.getScene().getWindow());
        if (selectedFile != null) {
            openPdf(selectedFile);
        }
    }

    public void openPdf(File file) {
        System.out.println("Opening PDF: " + file.getAbsolutePath());
        loadingIndicator.setVisible(true);
        currentPdfPath = file.getAbsolutePath();
        currentPage = 0;

        // Reset containers
        pdfContainer.getChildren().clear();
        pageContainers.clear();
        thumbnailContainer.getChildren().clear();
        thumbnailViews.clear();

        pdfRenderer.getDocumentInfo(currentPdfPath)
                .thenAcceptAsync(info -> {
                    System.out.println("Got document info: " + info.toString());
                    totalPages = info.get("page_count").asInt();
                    System.out.println("Total pages: " + totalPages);

                    Platform.runLater(() -> {
                        updateStatusBar();
                        updateNavigationButtons();
                        renderAllPages();
                        updateThumbnails();
                        updateFileInfo(file);
                        updateRecentFiles(file);

                        // Ensure first page is visible
                        scrollToPage(0);
                        highlightThumbnail(0);

                        // Update UI controls
                        prevPageButton.setDisable(true);
                        nextPageButton.setDisable(totalPages <= 1);

                        // Request focus for keyboard shortcuts
                        contentContainer.requestFocus();
                    });
                }, Platform::runLater)
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        loadingIndicator.setVisible(false);
                        showError("Error Opening PDF",
                                "Could not open the PDF file: " + file.getName(),
                                e.getMessage());
                        System.err.println("Error opening PDF: " + e.getMessage());
                        e.printStackTrace();
                    });
                    return null;
                });
    }

    private void renderAllPages() {
        System.out.println("renderAllPages() called - Current zoom: " + currentZoom);
        loadingIndicator.setVisible(true);

        // Store current scroll position and page
        double scrollPosition = scrollPane.getVvalue();
        int currentVisiblePage = currentPage;

        // Clear and re-render
        pdfContainer.getChildren().clear();
        pageContainers.clear();

        for (int i = 0; i < totalPages; i++) {
            final int pageNum = i;
            VBox pageBox = new VBox();
            pageBox.setUserData(pageNum);
            pageBox.setAlignment(javafx.geometry.Pos.CENTER);
            pageBox.setPadding(new Insets(20)); // Add padding around pages
            pageBox.getStyleClass().add("pdf-page");

            ImageView pageView = new ImageView();
            pageView.setPreserveRatio(true);
            pageView.setSmooth(true);
            pageBox.getChildren().add(pageView);

            pdfContainer.getChildren().add(pageBox);
            pageContainers.put(pageNum, pageBox);

            System.out.println("Requesting render for page " + (pageNum + 1) + " at zoom " + currentZoom);
            renderPage(pageNum, pageView);
        }

        // Restore scroll position after rendering
        Platform.runLater(() -> {
            scrollPane.setVvalue(scrollPosition);
            highlightThumbnail(currentVisiblePage);
            updateStatusBar();
        });
    }

    private void renderPage(int pageNum, ImageView targetView) {
        System.out.println("renderPage() called for page " + (pageNum + 1) + " with zoom " + currentZoom);
        pdfRenderer.renderPage(currentPdfPath, pageNum, currentZoom)
                .thenAcceptAsync(image -> {
                    System.out.println("Page " + (pageNum + 1) + " rendered successfully at zoom " + currentZoom);
                    Platform.runLater(() -> {
                        targetView.setImage(image);
                        if (pageNum == totalPages - 1) {
                            loadingIndicator.setVisible(false);
                            System.out.println("All pages rendered at zoom " + currentZoom);
                        }
                    });
                }, Platform::runLater)
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        System.err.println("Error rendering page " + (pageNum + 1) + ": " + e.getMessage());
                        if (pageNum == totalPages - 1) {
                            loadingIndicator.setVisible(false);
                        }
                        showError("Error Rendering Page",
                                "Could not render page " + (pageNum + 1),
                                e.getMessage());
                    });
                    return null;
                });
    }

    private void updateThumbnails() {
        thumbnailContainer.getChildren().clear();
        thumbnailViews.clear();

        for (int i = 0; i < totalPages; i++) {
            final int pageNum = i;
            VBox thumbnailBox = new VBox(5);
            thumbnailBox.getStyleClass().add("thumbnail");
            thumbnailBox.setMaxWidth(150);

            ImageView thumbnail = new ImageView();
            thumbnail.setFitWidth(150);
            thumbnail.setPreserveRatio(true);
            thumbnail.setSmooth(true);

            Label pageLabel = new Label("Page " + (pageNum + 1));
            pageLabel.getStyleClass().add("thumbnail-label");

            thumbnailBox.getChildren().addAll(thumbnail, pageLabel);
            thumbnailViews.put(pageNum, thumbnail);

            // Add click handler
            thumbnailBox.setOnMouseClicked(e -> {
                scrollToPage(pageNum);
            });

            thumbnailContainer.getChildren().add(thumbnailBox);

            // Render thumbnail
            pdfRenderer.renderPage(currentPdfPath, pageNum, 0.2)
                    .thenAcceptAsync(image -> {
                        thumbnail.setImage(image);
                    }, Platform::runLater);
        }
    }

    private void detectVisiblePage() {
        if (pdfContainer.getChildren().isEmpty())
            return;

        double viewportHeight = scrollPane.getViewportBounds().getHeight();
        double scrollY = scrollPane.getVvalue() * (pdfContainer.getHeight() - viewportHeight);

        System.out.println("Detecting visible page - Scroll Y: " + scrollY +
                ", Viewport Height: " + viewportHeight);

        int bestMatchPage = 0;
        double bestVisibleArea = 0;

        for (Node node : pdfContainer.getChildren()) {
            VBox pageBox = (VBox) node;
            Bounds bounds = pageBox.getBoundsInParent();
            int pageNum = (int) pageBox.getUserData();

            // Calculate visible area of the page
            double pageTop = bounds.getMinY();
            double pageBottom = bounds.getMaxY();
            double pageHeight = bounds.getHeight();

            double visibleTop = Math.max(scrollY, pageTop);
            double visibleBottom = Math.min(scrollY + viewportHeight, pageBottom);
            double visibleArea = Math.max(0, visibleBottom - visibleTop);

            System.out.println("Page " + (pageNum + 1) +
                    " - Top: " + pageTop +
                    ", Bottom: " + pageBottom +
                    ", Visible Area: " + visibleArea);

            if (visibleArea > bestVisibleArea) {
                bestVisibleArea = visibleArea;
                bestMatchPage = pageNum;
            }
        }

        if (bestVisibleArea > 0 && bestMatchPage != currentPage) {
            System.out.println("Most visible page changed to: " + (bestMatchPage + 1));
            updateCurrentPage(bestMatchPage);
        }
    }

    private void updateCurrentPage(int newPage) {
        if (newPage != currentPage && newPage >= 0 && newPage < totalPages) {
            System.out.println("Updating current page from " + (currentPage + 1) + " to " + (newPage + 1));
            currentPage = newPage;
            updateStatusBar();
            updateNavigationButtons();
            highlightThumbnail(currentPage);
        }
    }

    private void scrollToPage(int pageNum) {
        System.out.println("Scrolling to page: " + (pageNum + 1));
        VBox pageBox = pageContainers.get(pageNum);
        if (pageBox != null) {
            isScrolling = true;
            Platform.runLater(() -> {
                try {
                    double contentHeight = pdfContainer.getHeight();
                    double viewportHeight = scrollPane.getViewportBounds().getHeight();
                    double pageTop = pageBox.getBoundsInParent().getMinY();

                    // Calculate scroll position to center the page
                    double pageHeight = pageBox.getBoundsInParent().getHeight();
                    double targetY = pageTop - (viewportHeight - pageHeight) / 2;
                    targetY = Math.max(0, Math.min(targetY, contentHeight - viewportHeight));

                    double vvalue = targetY / (contentHeight - viewportHeight);
                    vvalue = Math.max(0, Math.min(1, vvalue));

                    System.out.println("Scrolling - Page " + (pageNum + 1) +
                            " - Target Y: " + targetY +
                            " - VValue: " + vvalue);

                    scrollPane.setVvalue(vvalue);
                    updateCurrentPage(pageNum);
                } catch (Exception e) {
                    System.err.println("Error during scroll: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    isScrolling = false;
                }
            });
        } else {
            System.err.println("Page container not found for page " + (pageNum + 1));
        }
    }

    private void navigateToPage(int newPage) {
        System.out.println("Navigating to page: " + (newPage + 1) + " of " + totalPages);
        if (newPage >= 0 && newPage < totalPages) {
            // Store current zoom level
            double currentZoomLevel = currentZoom;

            // Update current page
            currentPage = newPage;

            // Scroll to the new page
            scrollToPage(newPage);

            // Ensure zoom level is maintained
            currentZoom = currentZoomLevel;
            updateZoomComboBox();

            // Update UI
            updateStatusBar();
            updateNavigationButtons();
            highlightThumbnail(newPage);

            System.out.println("Navigation complete - Current page: " + (currentPage + 1));

            // Request focus for keyboard shortcuts
            contentContainer.requestFocus();
        } else {
            System.out.println("Invalid page number: " + newPage);
        }
    }

    private void highlightThumbnail(int pageNum) {
        Platform.runLater(() -> {
            if (currentSelectedThumbnail != null) {
                currentSelectedThumbnail.getStyleClass().remove("thumbnail-selected");
            }

            VBox thumbnailBox = (VBox) thumbnailViews.get(pageNum).getParent();
            if (thumbnailBox != null) {
                thumbnailBox.getStyleClass().add("thumbnail-selected");
                currentSelectedThumbnail = thumbnailBox;

                // Ensure thumbnail is visible in the sidebar
                ScrollPane thumbnailScroll = (ScrollPane) thumbnailContainer.getParent();
                double thumbY = thumbnailBox.getBoundsInParent().getMinY();
                double viewportHeight = thumbnailScroll.getViewportBounds().getHeight();
                double contentHeight = thumbnailContainer.getHeight();

                if (contentHeight > viewportHeight) {
                    double vvalue = thumbY / (contentHeight - viewportHeight);
                    vvalue = Math.max(0, Math.min(1, vvalue));
                    thumbnailScroll.setVvalue(vvalue);
                }
            }
        });
    }

    private void updateNavigationButtons() {
        Platform.runLater(() -> {
            boolean prevDisabled = currentPage <= 0;
            boolean nextDisabled = currentPage >= totalPages - 1;

            System.out.println("Updating navigation buttons - Current page: " + (currentPage + 1) +
                    ", Total pages: " + totalPages +
                    ", Prev disabled: " + prevDisabled +
                    ", Next disabled: " + nextDisabled);

            if (prevPageButton != null && nextPageButton != null) {
                prevPageButton.setDisable(prevDisabled);
                nextPageButton.setDisable(nextDisabled);
            } else {
                System.err.println("Navigation buttons are null!");
            }
        });
    }

    private void handleZoomLevelChange() {
        if (zoomLevelComboBox.getValue() == null) {
            System.out.println("Zoom value is null");
            return;
        }

        String zoomText = zoomLevelComboBox.getValue().replace("%", "").trim();
        System.out.println("Handling zoom change: " + zoomText + "%");

        try {
            double newZoom = Double.parseDouble(zoomText) / 100.0;
            newZoom = Math.max(ZOOM_LEVELS[0], Math.min(ZOOM_LEVELS[ZOOM_LEVELS.length - 1], newZoom));

            System.out.println("New zoom level: " + (newZoom * 100) + "%");

            // Store current scroll position relative to content
            double scrollPercentage = scrollPane.getVvalue();
            int currentVisiblePage = currentPage;

            // Update zoom
            currentZoom = newZoom;
            updateZoomComboBox();

            // Re-render all pages with new zoom
            renderAllPages();

            // Restore scroll position after rendering
            Platform.runLater(() -> {
                scrollPane.setVvalue(scrollPercentage);
                highlightThumbnail(currentVisiblePage);
            });

            updateStatusBar();
        } catch (NumberFormatException e) {
            System.err.println("Invalid zoom value: " + zoomText);
            showError("Invalid Zoom Level", "Please enter a valid zoom percentage",
                    "The zoom level must be a number between 25% and 400%");
            updateZoomComboBox(); // Reset to current zoom level
        }
    }

    private void handleFitWidth() {
        System.out.println("Handling fit width - Selected: " + fitWidthToggle.isSelected());
        if (fitWidthToggle.isSelected()) {
            // Unselect fit page if it's selected
            fitPageToggle.setSelected(false);

            // Calculate zoom to fit width
            if (!pdfContainer.getChildren().isEmpty()) {
                VBox pageBox = (VBox) pdfContainer.getChildren().get(currentPage);
                ImageView pageView = (ImageView) pageBox.getChildren().get(0);

                if (pageView.getImage() != null) {
                    double imageWidth = pageView.getImage().getWidth();
                    double viewportWidth = scrollPane.getViewportBounds().getWidth();
                    // Account for padding and margins
                    viewportWidth = Math.max(1, viewportWidth - 40); // 20px padding on each side

                    double newZoom = viewportWidth / imageWidth;
                    System.out.println("Fit Width - Image width: " + imageWidth +
                            ", Viewport width: " + viewportWidth +
                            ", New zoom: " + newZoom);

                    currentZoom = newZoom;
                    updateZoomComboBox();
                    renderAllPages();
                }
            }
        }
    }

    private void handleFitPage() {
        System.out.println("Handling fit page - Selected: " + fitPageToggle.isSelected());
        if (fitPageToggle.isSelected()) {
            // Unselect fit width if it's selected
            fitWidthToggle.setSelected(false);

            // Calculate zoom to fit page
            if (!pdfContainer.getChildren().isEmpty()) {
                VBox pageBox = (VBox) pdfContainer.getChildren().get(currentPage);
                ImageView pageView = (ImageView) pageBox.getChildren().get(0);

                if (pageView.getImage() != null) {
                    double imageWidth = pageView.getImage().getWidth();
                    double imageHeight = pageView.getImage().getHeight();
                    double viewportWidth = scrollPane.getViewportBounds().getWidth();
                    double viewportHeight = scrollPane.getViewportBounds().getHeight();

                    // Account for padding and margins
                    viewportWidth = Math.max(1, viewportWidth - 40); // 20px padding on each side
                    viewportHeight = Math.max(1, viewportHeight - 40); // 20px padding on each side

                    // Calculate zoom factors for both width and height
                    double widthZoom = viewportWidth / imageWidth;
                    double heightZoom = viewportHeight / imageHeight;

                    // Use the smaller zoom factor to ensure the entire page fits
                    double newZoom = Math.min(widthZoom, heightZoom);

                    System.out.println("Fit Page - Image size: " + imageWidth + "x" + imageHeight +
                            ", Viewport size: " + viewportWidth + "x" + viewportHeight +
                            ", New zoom: " + newZoom);

                    currentZoom = newZoom;
                    updateZoomComboBox();
                    renderAllPages();
                }
            }
        }
    }

    private void handleRotate(int degrees) {
        pdfImageView.setRotate((pdfImageView.getRotate() + degrees) % 360);
    }

    private void updateStatusBar() {
        Platform.runLater(() -> {
            if (pageInfoLabel != null) {
                pageInfoLabel.setText(String.format("Page: %d / %d", currentPage + 1, totalPages));
            }
            if (zoomLabel != null) {
                zoomLabel.setText(String.format("Zoom: %.0f%%", currentZoom * 100));
            }
        });
    }

    private void showError(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void updateFileInfo(File file) {
        try {
            Path path = file.toPath();
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            Platform.runLater(() -> {
                fileNameLabel.setText(file.getName());
                pageSizeLabel.setText(String.valueOf(totalPages));
                fileSizeLabel.setText(formatFileSize(file.length()));
                createdDateLabel.setText(dateFormat.format(new Date(attrs.creationTime().toMillis())));
                modifiedDateLabel.setText(dateFormat.format(new Date(attrs.lastModifiedTime().toMillis())));
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String formatFileSize(long bytes) {
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int unitIndex = 0;
        double size = bytes;

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        return String.format("%.2f %s", size, units[unitIndex]);
    }

    private void updateRecentFiles(File file) {
        String filePath = file.getAbsolutePath();
        recentFilesList.remove(filePath);
        recentFilesList.add(0, filePath);
        while (recentFilesList.size() > MAX_RECENT_FILES) {
            recentFilesList.remove(recentFilesList.size() - 1);
        }
    }

    public void shutdown() {
        executorService.shutdown();
    }

    // Add window resize listener to maintain fit modes
    private void setupResizeListener() {
        Platform.runLater(() -> {
            Scene scene = contentContainer.getScene();
            if (scene != null) {
                scene.getWindow().widthProperty().addListener((obs, oldVal, newVal) -> {
                    handleWindowResize();
                });
                scene.getWindow().heightProperty().addListener((obs, oldVal, newVal) -> {
                    handleWindowResize();
                });
            }
        });
    }

    private void handleWindowResize() {
        // Reapply fit mode if active
        if (fitWidthToggle.isSelected()) {
            handleFitWidth();
        } else if (fitPageToggle.isSelected()) {
            handleFitPage();
        }
    }
}