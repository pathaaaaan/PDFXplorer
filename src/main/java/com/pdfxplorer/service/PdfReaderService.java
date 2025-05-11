package com.pdfxplorer.service;

import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.File;
import java.io.IOException;

public class PdfReaderService {

    public static PDDocument loadDocument(File file) throws IOException {
        return PDDocument.load(file);
    }
}