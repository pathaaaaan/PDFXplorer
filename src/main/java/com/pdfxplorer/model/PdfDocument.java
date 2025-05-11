package com.pdfxplorer.model;

public class PdfDocument {
    private String fileName;
    private int pageCount;

    public PdfDocument(String fileName, int pageCount) {
        this.fileName = fileName;
        this.pageCount = pageCount;
    }

    public String getFileName() {
        return fileName;
    }

    public int getPageCount() {
        return pageCount;
    }
}