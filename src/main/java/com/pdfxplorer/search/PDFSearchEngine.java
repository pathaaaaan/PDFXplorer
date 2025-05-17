package com.pdfxplorer.search;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PDFSearchEngine {
    public static class SearchOptions {
        private final String query;
        private final boolean caseSensitive;
        private final boolean wholeWord;

        public SearchOptions(String query, boolean caseSensitive, boolean wholeWord) {
            this.query = query;
            this.caseSensitive = caseSensitive;
            this.wholeWord = wholeWord;
        }

        public Pattern createSearchPattern() {
            String regex = wholeWord ? "\\b" + Pattern.quote(query) + "\\b" : Pattern.quote(query);
            return Pattern.compile(regex, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
        }
    }

    public static class SearchResult {
        private final int pageIndex;
        private final String matchedText;
        private final List<TextPosition> textPositions;

        public SearchResult(int pageIndex, String matchedText, List<TextPosition> textPositions) {
            this.pageIndex = pageIndex;
            this.matchedText = matchedText;
            this.textPositions = textPositions;
        }

        public int getPageIndex() {
            return pageIndex;
        }

        public String getMatchedText() {
            return matchedText;
        }

        public List<TextPosition> getTextPositions() {
            return textPositions;
        }
    }

    public List<SearchResult> search(PDDocument document, SearchOptions options) throws IOException {
        List<SearchResult> results = new ArrayList<>();

        PDFTextStripper stripper = new PDFTextStripper() {
            @Override
            protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
                Pattern pattern = options.createSearchPattern();
                Matcher matcher = pattern.matcher(text);

                while (matcher.find()) {
                    int start = matcher.start();
                    int end = matcher.end();

                    // Get text positions for the matched text
                    List<TextPosition> matchPositions = new ArrayList<>();
                    int currentPos = 0;

                    for (TextPosition pos : textPositions) {
                        String charText = pos.getUnicode();
                        if (currentPos >= start && currentPos < end) {
                            matchPositions.add(pos);
                        }
                        currentPos += charText.length();
                    }

                    if (!matchPositions.isEmpty()) {
                        results.add(new SearchResult(
                                getCurrentPageNo() - 1,
                                text.substring(start, end),
                                matchPositions));
                    }
                }
            }
        };

        for (int i = 0; i < document.getNumberOfPages(); i++) {
            stripper.setStartPage(i + 1);
            stripper.setEndPage(i + 1);
            stripper.getText(document);
        }

        return results;
    }
}