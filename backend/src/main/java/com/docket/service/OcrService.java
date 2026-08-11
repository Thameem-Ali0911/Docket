package com.docket.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

@Service
public class OcrService {

    private final Tesseract tesseract;

    public OcrService() {
        this.tesseract = new Tesseract();
        // Read TESSDATA_PREFIX from environment. If not set, it might fail or fall back to defaults.
        String tessdataPrefix = System.getenv("TESSDATA_PREFIX");
        if (tessdataPrefix != null && !tessdataPrefix.isEmpty()) {
            this.tesseract.setDatapath(tessdataPrefix);
        } else {
            // For Docker container, this is the default path we set
            this.tesseract.setDatapath("/usr/share/tesseract-ocr/5/tessdata");
        }
    }

    /**
     * Extracts text from a given file (PDF or Image).
     * @param file The file to process
     * @return Extracted text
     * @throws Exception If an error occurs during extraction
     */
    public String extractText(File file) throws Exception {
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".pdf")) {
            return extractTextFromPdf(file);
        } else if (fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return extractTextFromImage(file);
        } else {
            throw new IllegalArgumentException("Unsupported file type for OCR: " + fileName);
        }
    }

    private String extractTextFromPdf(File file) throws Exception {
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            // If text is extracted directly from the PDF and it's long enough, use it.
            if (text != null && text.trim().length() > 50) {
                return text.trim();
            }

            // Fallback to OCR if the PDF is scanned (empty or very short text)
            return extractTextWithOcr(document);
        }
    }

    private String extractTextWithOcr(PDDocument document) throws IOException, TesseractException {
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        StringBuilder extractedText = new StringBuilder();

        for (int page = 0; page < document.getNumberOfPages(); page++) {
            // Render at 300 DPI for good OCR quality
            BufferedImage bim = pdfRenderer.renderImageWithDPI(page, 300, ImageType.RGB);
            String text = tesseract.doOCR(bim);
            extractedText.append(text).append("\n");
        }

        return extractedText.toString().trim();
    }

    private String extractTextFromImage(File file) throws TesseractException {
        return tesseract.doOCR(file).trim();
    }
}
