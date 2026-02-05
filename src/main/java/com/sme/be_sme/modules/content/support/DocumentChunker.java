package com.sme.be_sme.modules.content.support;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Splits normalized document text into overlapping chunks for RAG.
 * Chunk size ~800 chars, overlap ~150 chars; drops blank or very short chunks (&lt; 50 chars).
 */
public final class DocumentChunker {

    private static final int CHUNK_SIZE = 800;
    private static final int OVERLAP = 150;
    private static final int MIN_CHUNK_LENGTH = 50;
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private DocumentChunker() {
    }

    /**
     * Chunk the given normalized text. Skips chunks that are blank or shorter than MIN_CHUNK_LENGTH.
     */
    public static List<String> chunk(String normalizedText) {
        List<String> result = new ArrayList<>();
        if (!StringUtils.hasText(normalizedText)) {
            return result;
        }
        int start = 0;
        int len = normalizedText.length();
        while (start < len) {
            int end = Math.min(start + CHUNK_SIZE, len);
            String chunk = normalizedText.substring(start, end).trim();
            if (chunk.length() >= MIN_CHUNK_LENGTH) {
                result.add(chunk);
            }
            start += (CHUNK_SIZE - OVERLAP);
            if (start >= len) break;
        }
        return result;
    }

    /**
     * Normalize extracted text: trim and collapse excessive whitespace.
     */
    public static String normalize(String rawText) {
        if (rawText == null) return "";
        String t = WHITESPACE.matcher(rawText.trim()).replaceAll(" ");
        return t.trim();
    }
}
