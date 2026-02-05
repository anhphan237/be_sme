package com.sme.be_sme.modules.content.support;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * Extracts plain text from uploaded documents for RAG chunking.
 * Supports PDF (PDFBox), DOCX (POI); other types return empty string.
 */
public final class DocumentTextExtractor {

    private static final Logger log = LoggerFactory.getLogger(DocumentTextExtractor.class);

    private DocumentTextExtractor() {
    }

    /**
     * Extract plain text from the given bytes. Uses fileExtension (e.g. "pdf", "docx") to choose parser.
     * If unsupported or extraction fails, returns empty string.
     */
    public static String extractText(byte[] bytes, String fileExtension) {
        if (bytes == null || bytes.length == 0) return "";
        return extractText(new ByteArrayInputStream(bytes), fileExtension);
    }

    /**
     * Extract plain text from the given stream. Uses fileExtension (e.g. "pdf", "docx") to choose parser.
     * If unsupported or extraction fails, returns empty string.
     */
    public static String extractText(InputStream inputStream, String fileExtension) {
        if (inputStream == null) return "";
        String ext = fileExtension != null ? fileExtension.toLowerCase(Locale.ROOT).trim() : "";
        try {
            if (ext.equals("pdf")) {
                return extractFromPdf(inputStream);
            }
            if (ext.equals("docx")) {
                return extractFromDocx(inputStream);
            }
            log.debug("Unsupported file type for text extraction: {}", fileExtension);
            return "";
        } catch (Exception e) {
            log.warn("Text extraction failed for extension {}: {}", fileExtension, e.getMessage());
            return "";
        }
    }

    private static String extractFromPdf(InputStream inputStream) throws IOException {
        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private static String extractFromDocx(InputStream inputStream) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return extractor.getText();
        }
    }
}
