package com.sme.be_sme.modules.content.doceditor;

import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class DocumentDetailInclude {

    public final boolean links;
    public final boolean assignments;
    public final boolean attachments;
    public final boolean accessRules;
    public final boolean blocks;

    public DocumentDetailInclude(boolean links, boolean assignments, boolean attachments, boolean accessRules, boolean blocks) {
        this.links = links;
        this.assignments = assignments;
        this.attachments = attachments;
        this.accessRules = accessRules;
        this.blocks = blocks;
    }

    /**
     * Parses comma-separated tokens. Blank/null means include all (Phase 3 default per plan).
     */
    public static DocumentDetailInclude parse(String includeCsv) {
        if (!StringUtils.hasText(includeCsv)) {
            return new DocumentDetailInclude(true, true, true, false, false);
        }
        Set<String> tokens = Stream.of(includeCsv.split(","))
                .map(s -> s != null ? s.trim().toLowerCase(Locale.ROOT) : "")
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        return new DocumentDetailInclude(
                tokens.contains("links"),
                tokens.contains("assignments"),
                tokens.contains("attachments"),
                tokens.contains("accessrules"),
                tokens.contains("blocks"));
    }
}
