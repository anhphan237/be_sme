package com.sme.be_sme.modules.content.doceditor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.util.StringUtils;

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
}
