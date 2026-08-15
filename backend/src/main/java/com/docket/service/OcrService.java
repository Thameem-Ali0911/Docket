package com.docket.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.Word;

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
     * Extracts text from a given file (PDF or Image), along with a confidence signal.
     * @param file The file to process
     * @return OcrResult containing extracted text and a confidence score (0-100), or
     *         OcrResult.NOT_APPLICABLE confidence when text came from a PDF's embedded
     *         text layer rather than image recognition.
     * @throws Exception If an error occurs during extraction
     */
    public OcrResult extractText(File file) throws Exception {
        String fileName = file.getName().toLowerCase();
        try {
            if (fileName.endsWith(".pdf")) {
                return extractTextFromPdf(file);
            } else if (fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                return extractTextFromImage(file);
            } else {
                throw new IllegalArgumentException("Unsupported file type for OCR: " + fileName);
            }
        } catch (Exception e) {
            throw e;
        } catch (Throwable t) {
            // Tess4J/Tesseract is a native (JNI) library - a missing/mismatched native binary
            // or tessdata path surfaces as an Error (UnsatisfiedLinkError, NoClassDefFoundError),
            // not an Exception. Re-wrap as a checked Exception so every caller's normal
            // `catch (Exception e)` handling actually catches this instead of it silently
            // killing whatever thread is running OCR.
            throw new Exception("Native OCR engine failure: " + t.getClass().getSimpleName()
                + (t.getMessage() != null ? " - " + t.getMessage() : ""), t);
        }
    }

    private OcrResult extractTextFromPdf(File file) throws Exception {
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            // If text is extracted directly from the PDF and it's long enough, use it.
            // This came from the embedded text layer, not image recognition, so there's
            // no OCR confidence to report - trust it as-is.
            if (text != null && text.trim().length() > 50) {
                return new OcrResult(text.trim(), OcrResult.NOT_APPLICABLE);
            }

            // Fallback to OCR if the PDF is scanned (empty or very short text)
            return extractTextWithOcr(document);
        }
    }

    private OcrResult extractTextWithOcr(PDDocument document) throws IOException, TesseractException {
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        StringBuilder extractedText = new StringBuilder();
        double confidenceSum = 0;
        int confidenceCount = 0;

        for (int page = 0; page < document.getNumberOfPages(); page++) {
            // Render at 300 DPI for good OCR quality
            BufferedImage bim = pdfRenderer.renderImageWithDPI(page, 300, ImageType.RGB);
            String text = tesseract.doOCR(bim);
            extractedText.append(text).append("\n");

            List<Word> words = tesseract.getWords(bim, ITessAPI.TessPageIteratorLevel.RIL_WORD);
            for (Word word : words) {
                confidenceSum += word.getConfidence();
                confidenceCount++;
            }
        }

        double avgConfidence = confidenceCount > 0 ? confidenceSum / confidenceCount : 0;
        return new OcrResult(extractedText.toString().trim(), avgConfidence);
    }

    private OcrResult extractTextFromImage(File file) throws TesseractException, IOException {
        String text = tesseract.doOCR(file).trim();

        BufferedImage bim = ImageIO.read(file);
        double avgConfidence = 0;
        if (bim != null) {
            List<Word> words = tesseract.getWords(bim, ITessAPI.TessPageIteratorLevel.RIL_WORD);
            double confidenceSum = 0;
            int confidenceCount = 0;
            for (Word word : words) {
                confidenceSum += word.getConfidence();
                confidenceCount++;
            }
            avgConfidence = confidenceCount > 0 ? confidenceSum / confidenceCount : 0;
        }

        return new OcrResult(text, avgConfidence);
    }
}
