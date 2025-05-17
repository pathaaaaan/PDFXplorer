package com.pdfxplorer.util;

import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SearchHighlighter extends PDFTextStripper {
    private final String searchWord;
    private final List<SearchResult> searchResults = new ArrayList<>();
    private final List<TextPosition> allTextPositions = new ArrayList<>();
    private float pageHeight;

    public static class SearchResult {
        private final float x;
        private final float y;
        private final float width;
        private final float height;

        public SearchResult(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public float getX() {
            return x;
        }

        public float getY() {
            return y;
        }

        public float getWidth() {
            return width;
        }

        public float getHeight() {
            return height;
        }
    }

    private static class TextSegment {
        List<TextPosition> positions;
        String text;
        float lineY;

        TextSegment(float lineY) {
            this.positions = new ArrayList<>();
            this.text = "";
            this.lineY = lineY;
        }
    }

    public SearchHighlighter(String searchWord) throws IOException {
        this.searchWord = searchWord.toLowerCase().trim();
        setSortByPosition(true);
        setStartPage(1);
        setEndPage(1);
    }

    @Override
    protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
        // Group text positions by their vertical position (same line)
        float lastY = -1;
        TextSegment currentSegment = null;

        for (TextPosition position : textPositions) {
            float currentY = position.getY();

            // If we're on a new line or this is the first position
            if (lastY == -1 || Math.abs(currentY - lastY) > position.getHeight() * 0.5) {
                if (currentSegment != null) {
                    processTextSegment(currentSegment);
                }
                currentSegment = new TextSegment(currentY);
            }

            // Add position to current segment
            if (currentSegment != null) {
                currentSegment.positions.add(position);
                currentSegment.text += position.getUnicode();
            }

            lastY = currentY;
        }

        // Process the last segment
        if (currentSegment != null) {
            processTextSegment(currentSegment);
        }
    }

    private void processTextSegment(TextSegment segment) {
        if (segment.positions.isEmpty())
            return;

        // Sort positions by X coordinate
        segment.positions.sort((a, b) -> Float.compare(a.getX(), b.getX()));

        // Add positions with proper word boundaries
        List<TextPosition> wordPositions = new ArrayList<>();
        float expectedX = segment.positions.get(0).getX();

        for (int i = 0; i < segment.positions.size(); i++) {
            TextPosition position = segment.positions.get(i);
            float gap = position.getX() - expectedX;

            // If there's a significant gap, it might be a word boundary
            if (gap > position.getWidth() * 0.5) {
                // Process the completed word
                if (!wordPositions.isEmpty()) {
                    addWordToAllPositions(wordPositions);
                    wordPositions = new ArrayList<>();
                }
            }

            wordPositions.add(position);
            expectedX = position.getX() + position.getWidth();

            // Handle the last word in the segment
            if (i == segment.positions.size() - 1 && !wordPositions.isEmpty()) {
                addWordToAllPositions(wordPositions);
            }
        }
    }

    private void addWordToAllPositions(List<TextPosition> wordPositions) {
        // Add a small space between words for better searching
        if (!allTextPositions.isEmpty()) {
            TextPosition lastPos = allTextPositions.get(allTextPositions.size() - 1);
            // Instead of creating a TextPosition directly, we'll just add a space character
            allTextPositions.add(lastPos);
        }
        allTextPositions.addAll(wordPositions);
    }

    @Override
    public String getText(org.apache.pdfbox.pdmodel.PDDocument doc) throws IOException {
        allTextPositions.clear();
        searchResults.clear();
        String text = super.getText(doc);
        if (getCurrentPage() != null) {
            pageHeight = getCurrentPage().getMediaBox().getHeight();
        }
        searchInPositions();
        return text;
    }

    private void searchInPositions() {
        if (allTextPositions.isEmpty() || searchWord.isEmpty())
            return;

        StringBuilder textBuilder = new StringBuilder();
        List<TextPosition> currentPositions = new ArrayList<>();

        for (TextPosition pos : allTextPositions) {
            textBuilder.append(pos.getUnicode());
            currentPositions.add(pos);

            // Keep a sliding window of text that's at least as long as the search word
            while (textBuilder.length() >= searchWord.length()) {
                String currentText = textBuilder.toString().toLowerCase();
                int foundIndex = currentText.indexOf(searchWord);

                if (foundIndex != -1) {
                    // Calculate the positions for the found text
                    int startPosIndex = foundIndex;
                    int endPosIndex = startPosIndex + searchWord.length() - 1;

                    if (startPosIndex < currentPositions.size() && endPosIndex < currentPositions.size()) {
                        TextPosition firstChar = currentPositions.get(startPosIndex);
                        TextPosition lastChar = currentPositions.get(endPosIndex);

                        // Calculate the bounding box in PDF coordinates
                        float x = firstChar.getXDirAdj();
                        float y = firstChar.getYDirAdj();
                        float width = (lastChar.getXDirAdj() + lastChar.getWidthDirAdj()) - x;
                        float height = Math.max(firstChar.getHeightDir(), lastChar.getHeightDir());

                        searchResults.add(new SearchResult(x, y, width, height));
                    }

                    // Remove the matched text from the start to continue searching
                    textBuilder.delete(0, foundIndex + 1);
                    currentPositions.subList(0, foundIndex + 1).clear();
                } else {
                    // Remove one character from the start if no match is found
                    textBuilder.deleteCharAt(0);
                    currentPositions.remove(0);
                }
            }
        }
    }

    public List<SearchResult> getSearchResults() {
        return searchResults;
    }

    public float getPageHeight() {
        return pageHeight;
    }
}