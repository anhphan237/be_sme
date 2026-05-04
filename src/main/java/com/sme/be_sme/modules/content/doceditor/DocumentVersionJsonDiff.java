package com.sme.be_sme.modules.content.doceditor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Lightweight JSON metadata for comparing two published version payloads (FE hints only).
 */
public final class DocumentVersionJsonDiff {

    private DocumentVersionJsonDiff() {
    }

    public static boolean equalDeep(ObjectMapper om, String rawA, String rawB) {
        return parseObjectOrEmpty(om, rawA).equals(parseObjectOrEmpty(om, rawB));
    }

    public static ObjectNode buildSummary(ObjectMapper om, String rawA, String rawB) {
        JsonNode a = parseObjectOrEmpty(om, rawA);
        JsonNode b = parseObjectOrEmpty(om, rawB);
        ObjectNode out = om.createObjectNode();
        out.put("equalDeep", a.equals(b));
        if (!a.isObject() || !b.isObject()) {
            out.put("note", "non_object_treated_as_empty_object");
            ArrayNode paths = om.createArrayNode();
            if (!a.equals(b)) {
                paths.add("");
            }
            out.set("changedPaths", paths);
            return out;
        }
        TreeSet<String> keysA = new TreeSet<>();
        a.fieldNames().forEachRemaining(keysA::add);
        TreeSet<String> keysB = new TreeSet<>();
        b.fieldNames().forEachRemaining(keysB::add);

        ArrayNode added = om.createArrayNode();
        for (String k : keysB) {
            if (!keysA.contains(k)) {
                added.add(k);
            }
        }
        ArrayNode removed = om.createArrayNode();
        for (String k : keysA) {
            if (!keysB.contains(k)) {
                removed.add(k);
            }
        }
        out.set("topLevelKeysAdded", added);
        out.set("topLevelKeysRemoved", removed);

        ArrayNode changedPaths = om.createArrayNode();
        for (String k : keysA) {
            if (keysB.contains(k) && !a.get(k).equals(b.get(k))) {
                collectChangedPaths(changedPaths, k, a.get(k), b.get(k), 1);
                if (changedPaths.size() >= DocumentEditorConstants.VERSION_COMPARE_MAX_PATHS) {
                    break;
                }
            }
        }
        out.set("changedPaths", changedPaths);
        return out;
    }

    /**
     * Detailed compare result for FE visual diff.
     * Output items: {type,path,fromValue,toValue}
     * - type: ADD | REMOVE | UPDATE
     * - path: JSON-like path with root '$' and array indexes, e.g. $.blocks[1].text
     */
    public static ArrayNode buildChanges(ObjectMapper om, String rawA, String rawB) {
        JsonNode a = parseNodeOrEmptyObject(om, rawA);
        JsonNode b = parseNodeOrEmptyObject(om, rawB);
        ArrayNode out = om.createArrayNode();
        collectChanges(out, "$", a, b, 1);
        return out;
    }

    /**
     * Block-aware compare for editor documents where one top-level block ~= one line in Git diff.
     */
    public static ArrayNode buildBlockChanges(ObjectMapper om, String rawA, String rawB) {
        JsonNode aRoot = parseNodeOrEmptyObject(om, rawA);
        JsonNode bRoot = parseNodeOrEmptyObject(om, rawB);
        List<BlockView> aBlocks = extractTopLevelBlocks(aRoot);
        List<BlockView> bBlocks = extractTopLevelBlocks(bRoot);
        ArrayNode out = om.createArrayNode();

        boolean[] usedA = new boolean[aBlocks.size()];
        boolean[] usedB = new boolean[bBlocks.size()];
        int[] matchAtoB = new int[aBlocks.size()];
        for (int i = 0; i < matchAtoB.length; i++) {
            matchAtoB[i] = -1;
        }
        int[] matchBtoA = new int[bBlocks.size()];
        for (int i = 0; i < matchBtoA.length; i++) {
            matchBtoA[i] = -1;
        }

        // pass 1: exact block JSON match
        for (int i = 0; i < aBlocks.size(); i++) {
            if (out.size() >= DocumentEditorConstants.VERSION_COMPARE_MAX_PATHS) break;
            BlockView a = aBlocks.get(i);
            for (int j = 0; j < bBlocks.size(); j++) {
                if (usedB[j]) continue;
                BlockView b = bBlocks.get(j);
                if (a.node.equals(b.node)) {
                    usedA[i] = true;
                    usedB[j] = true;
                    matchAtoB[i] = j;
                    matchBtoA[j] = i;
                    break;
                }
            }
        }

        // pass 2: best-effort by block type + normalized text
        for (int i = 0; i < aBlocks.size(); i++) {
            if (usedA[i]) continue;
            BlockView a = aBlocks.get(i);
            for (int j = 0; j < bBlocks.size(); j++) {
                if (usedB[j]) continue;
                BlockView b = bBlocks.get(j);
                if (a.blockType.equals(b.blockType)
                        && StringUtils.hasText(a.normalizedText)
                        && a.normalizedText.equals(b.normalizedText)) {
                    usedA[i] = true;
                    usedB[j] = true;
                    matchAtoB[i] = j;
                    matchBtoA[j] = i;
                    break;
                }
            }
        }

        // pass 3: fallback pair by same index and block type
        int min = Math.min(aBlocks.size(), bBlocks.size());
        for (int i = 0; i < min; i++) {
            if (usedA[i] || usedB[i]) continue;
            BlockView a = aBlocks.get(i);
            BlockView b = bBlocks.get(i);
            if (a.blockType.equals(b.blockType)) {
                usedA[i] = true;
                usedB[i] = true;
                matchAtoB[i] = i;
                matchBtoA[i] = i;
            }
        }

        // matched pairs -> MOVE / UPDATE
        for (int i = 0; i < aBlocks.size(); i++) {
            if (out.size() >= DocumentEditorConstants.VERSION_COMPARE_MAX_PATHS) break;
            int j = matchAtoB[i];
            if (j < 0) continue;
            BlockView a = aBlocks.get(i);
            BlockView b = bBlocks.get(j);
            if (i != j) {
                addBlockChange(out, "MOVE", i, j, b.blockType, "$.content[" + j + "]",
                        a.preview, b.preview, a.node, b.node);
            } else if (!a.node.equals(b.node)) {
                addBlockChange(out, "UPDATE", i, j, b.blockType, "$.content[" + j + "]",
                        a.preview, b.preview, a.node, b.node);
            }
        }

        // REMOVE blocks
        for (int i = 0; i < aBlocks.size() && out.size() < DocumentEditorConstants.VERSION_COMPARE_MAX_PATHS; i++) {
            if (matchAtoB[i] >= 0) continue;
            BlockView a = aBlocks.get(i);
            addBlockChange(out, "REMOVE", i, null, a.blockType, "$.content[" + i + "]",
                    a.preview, null, a.node, null);
        }

        // ADD blocks
        for (int j = 0; j < bBlocks.size() && out.size() < DocumentEditorConstants.VERSION_COMPARE_MAX_PATHS; j++) {
            if (matchBtoA[j] >= 0) continue;
            BlockView b = bBlocks.get(j);
            addBlockChange(out, "ADD", null, j, b.blockType, "$.content[" + j + "]",
                    null, b.preview, null, b.node);
        }

        return out;
    }

    private static void collectChangedPaths(ArrayNode out, String path,
                                            JsonNode na, JsonNode nb, int depth) {
        if (out.size() >= DocumentEditorConstants.VERSION_COMPARE_MAX_PATHS) {
            return;
        }
        if (na.equals(nb)) {
            return;
        }
        if (depth >= DocumentEditorConstants.VERSION_COMPARE_MAX_DEPTH) {
            out.add(path);
            return;
        }
        if (na.isObject() && nb.isObject()) {
            TreeSet<String> keys = new TreeSet<>();
            na.fieldNames().forEachRemaining(keys::add);
            nb.fieldNames().forEachRemaining(keys::add);
            for (String k : keys) {
                if (out.size() >= DocumentEditorConstants.VERSION_COMPARE_MAX_PATHS) {
                    return;
                }
                JsonNode ca = na.get(k);
                JsonNode cb = nb.get(k);
                if (ca == null || cb == null || !ca.equals(cb)) {
                    String child = path + "." + k;
                    if (ca != null && cb != null && ca.isObject() && cb.isObject()) {
                        collectChangedPaths(out, child, ca, cb, depth + 1);
                    } else {
                        out.add(child);
                    }
                }
            }
        } else {
            out.add(path);
        }
    }

    private static JsonNode parseObjectOrEmpty(ObjectMapper om, String raw) {
        if (!StringUtils.hasText(raw)) {
            return om.createObjectNode();
        }
        try {
            JsonNode n = om.readTree(raw);
            return n != null && n.isObject() ? n : om.createObjectNode();
        } catch (Exception e) {
            return om.createObjectNode();
        }
    }

    private static JsonNode parseNodeOrEmptyObject(ObjectMapper om, String raw) {
        if (!StringUtils.hasText(raw)) {
            return om.createObjectNode();
        }
        try {
            JsonNode n = om.readTree(raw);
            return n != null ? n : om.createObjectNode();
        } catch (Exception e) {
            return om.createObjectNode();
        }
    }

    private static void collectChanges(ArrayNode out, String path, JsonNode a, JsonNode b, int depth) {
        if (out.size() >= DocumentEditorConstants.VERSION_COMPARE_MAX_PATHS) {
            return;
        }
        if (a == null && b == null) {
            return;
        }
        if (a == null) {
            addChange(out, "ADD", path, null, b);
            return;
        }
        if (b == null) {
            addChange(out, "REMOVE", path, a, null);
            return;
        }
        if (a.equals(b)) {
            return;
        }
        if (depth >= DocumentEditorConstants.VERSION_COMPARE_MAX_DEPTH) {
            addChange(out, "UPDATE", path, a, b);
            return;
        }
        if (a.isObject() && b.isObject()) {
            TreeSet<String> keys = new TreeSet<>();
            a.fieldNames().forEachRemaining(keys::add);
            b.fieldNames().forEachRemaining(keys::add);
            for (String key : keys) {
                if (out.size() >= DocumentEditorConstants.VERSION_COMPARE_MAX_PATHS) {
                    return;
                }
                JsonNode ca = a.get(key);
                JsonNode cb = b.get(key);
                collectChanges(out, path + "." + key, ca, cb, depth + 1);
            }
            return;
        }
        if (a.isArray() && b.isArray()) {
            int max = Math.max(a.size(), b.size());
            for (int i = 0; i < max; i++) {
                if (out.size() >= DocumentEditorConstants.VERSION_COMPARE_MAX_PATHS) {
                    return;
                }
                JsonNode ca = i < a.size() ? a.get(i) : null;
                JsonNode cb = i < b.size() ? b.get(i) : null;
                collectChanges(out, path + "[" + i + "]", ca, cb, depth + 1);
            }
            return;
        }
        addChange(out, "UPDATE", path, a, b);
    }

    private static void addChange(ArrayNode out, String type, String path, JsonNode fromValue, JsonNode toValue) {
        if (out.size() >= DocumentEditorConstants.VERSION_COMPARE_MAX_PATHS) {
            return;
        }
        ObjectNode row = out.addObject();
        row.put("type", type);
        row.put("path", path);
        row.set("fromValue", fromValue);
        row.set("toValue", toValue);
    }

    private static List<BlockView> extractTopLevelBlocks(JsonNode root) {
        List<BlockView> out = new ArrayList<>();
        if (root == null || !root.isObject()) {
            return out;
        }
        JsonNode content = root.get("content");
        if (content == null || !content.isArray()) {
            return out;
        }
        for (int i = 0; i < content.size(); i++) {
            JsonNode node = content.get(i);
            String blockType = node != null && node.isObject() && node.has("type")
                    ? node.path("type").asText("unknown")
                    : "unknown";
            String text = extractText(node);
            String normalizedText = normalizeText(text);
            String preview = buildPreview(text);
            out.add(new BlockView(i, node, blockType, normalizedText, preview));
        }
        return out;
    }

    private static String extractText(JsonNode node) {
        if (node == null || node.isNull()) {
            return "";
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode child : node) {
                String c = extractText(child);
                if (!c.isEmpty()) {
                    if (sb.length() > 0) sb.append(' ');
                    sb.append(c);
                }
            }
            return sb.toString();
        }
        if (node.isObject()) {
            if (node.has("text") && node.get("text").isTextual()) {
                return node.get("text").asText();
            }
            StringBuilder sb = new StringBuilder();
            node.fields().forEachRemaining(e -> {
                String c = extractText(e.getValue());
                if (!c.isEmpty()) {
                    if (sb.length() > 0) sb.append(' ');
                    sb.append(c);
                }
            });
            return sb.toString();
        }
        return node.asText("");
    }

    private static String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    private static String buildPreview(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String normalized = text.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= 120) {
            return normalized;
        }
        return normalized.substring(0, 117) + "...";
    }

    private static void addBlockChange(ArrayNode out,
                                       String type,
                                       Integer fromIndex,
                                       Integer toIndex,
                                       String blockType,
                                       String path,
                                       String fromPreview,
                                       String toPreview,
                                       JsonNode fromValue,
                                       JsonNode toValue) {
        if (out.size() >= DocumentEditorConstants.VERSION_COMPARE_MAX_PATHS) {
            return;
        }
        ObjectNode row = out.addObject();
        row.put("type", type);
        if (fromIndex != null) {
            row.put("fromBlockIndex", fromIndex);
        } else {
            row.putNull("fromBlockIndex");
        }
        if (toIndex != null) {
            row.put("toBlockIndex", toIndex);
        } else {
            row.putNull("toBlockIndex");
        }
        row.put("blockType", blockType != null ? blockType : "unknown");
        row.put("path", path);
        row.put("fromPreview", fromPreview != null ? fromPreview : "");
        row.put("toPreview", toPreview != null ? toPreview : "");
        row.set("fromValue", fromValue);
        row.set("toValue", toValue);
    }

    private static final class BlockView {
        private final int index;
        private final JsonNode node;
        private final String blockType;
        private final String normalizedText;
        private final String preview;

        private BlockView(int index, JsonNode node, String blockType, String normalizedText, String preview) {
            this.index = index;
            this.node = node;
            this.blockType = blockType;
            this.normalizedText = normalizedText;
            this.preview = preview;
        }
    }
}
