package com.pdfxplorer.util;

import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SearchHighlighter extends PDFTextStripper {
    private final String searchWord;
    private float x, y, width, height;
    private boolean found = false;

    private final List<TextPosition> allTextPositions = new ArrayList<>();

    public SearchHighlighter(String searchWord) throws IOException {
        this.searchWord = searchWord.toLowerCase();
        setSortByPosition(true);
    }

    @Override
    protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
        allTextPositions.addAll(textPositions);
    }

    @Override
    public String getText(org.apache.pdfbox.pdmodel.PDDocument doc) throws IOException {
        String text = super.getText(doc);  // calls PDFBox's original getText()
        searchUsingKMP();                  // now run the KMP match
        return text;                       // return the full extracted text
    }

    private void searchUsingKMP() {
        if (allTextPositions.isEmpty() || searchWord.isEmpty()) return;

        char[] text = new char[allTextPositions.size()];
        for (int i = 0; i < text.length; i++) {
            text[i] = Character.toLowerCase(allTextPositions.get(i).getUnicode().charAt(0));
        }

        char[] pattern = searchWord.toCharArray();
        int matchIndex = kmpSearch(text, pattern);
        if (matchIndex == -1) return;

        TextPosition start = allTextPositions.get(matchIndex);
        TextPosition end = allTextPositions.get(matchIndex + pattern.length - 1);

        this.x = start.getXDirAdj();
        this.y = start.getPageHeight() - start.getYDirAdj();
        this.width = end.getXDirAdj() + end.getWidthDirAdj() - this.x;
        this.height = start.getHeightDir();
        this.found = true;
    }

    private int kmpSearch(char[] text, char[] pattern) {
        int[] lps = computeLPS(pattern);
        int i = 0, j = 0;

        while (i < text.length) {
            if (text[i] == pattern[j]) {
                i++; j++;
                if (j == pattern.length) return i - j;
            } else if (j != 0) {
                j = lps[j - 1];
            } else {
                i++;
            }
        }
        return -1;
    }

    private int[] computeLPS(char[] pattern) {
        int[] lps = new int[pattern.length];
        int len = 0, i = 1;
        while (i < pattern.length) {
            if (pattern[i] == pattern[len]) {
                lps[i++] = ++len;
            } else if (len != 0) {
                len = lps[len - 1];
            } else {
                lps[i++] = 0;
            }
        }
        return lps;
    }

    public boolean isFound() { return found; }
    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
}