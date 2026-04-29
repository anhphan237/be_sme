package com.sme.be_sme.modules.content.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts plain text from editor JSON payloads by collecting all non-empty text nodes.
 */
public final class DocumentEditorTextExtractor {

    private DocumentEditorTextExtractor() {
    }

    public static String extractText(ObjectMapper objectMapper, String editorJson) {
        if (objectMapper == null || !StringUtils.hasText(editorJson)) {
            return "";
        }
        try {
            JsonNode root = objectMapper.readTree(editorJson);
            List<String> parts = new ArrayList<>();
            collectTextNodes(root, parts);
            return String.join("\n", parts);
        } catch (Exception e) {
            return "";
        }
    }

    private static void collectTextNodes(JsonNode node, List<String> out) {
        if (node == null) {
            return;
        }
        if (node.isTextual()) {
            String text = node.asText();
            if (StringUtils.hasText(text)) {
                out.add(text.trim());
            }
            return;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                collectTextNodes(item, out);
            }
            return;
        }
        if (node.isObject()) {
            JsonNode textNode = node.get("text");
            if (textNode != null && textNode.isTextual() && StringUtils.hasText(textNode.asText())) {
                out.add(textNode.asText().trim());
            }
            node.fields().forEachRemaining(entry -> collectTextNodes(entry.getValue(), out));
        }
    }
}
