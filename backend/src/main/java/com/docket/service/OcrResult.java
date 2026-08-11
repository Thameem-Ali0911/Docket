package com.docket.service;

/**
 * Result of an OCR/text-extraction pass.
 *
 * confidence is on a 0-100 scale, mirroring Tesseract's own word-confidence scale.
 * For text pulled directly out of a PDF's embedded text layer (no OCR involved),
 * confidence is reported as -1 to mean "not applicable / trust it as-is", since that
 * text came straight from the document rather than from image recognition.
 */
public class OcrResult {

    public static final double NOT_APPLICABLE = -1.0;

    private final String text;
    private final double confidence;

    public OcrResult(String text, double confidence) {
        this.text = text;
        this.confidence = confidence;
    }

    public String getText() {
        return text;
    }

    public double getConfidence() {
        return confidence;
    }

    public boolean isConfidenceApplicable() {
        return confidence >= 0;
    }
}
