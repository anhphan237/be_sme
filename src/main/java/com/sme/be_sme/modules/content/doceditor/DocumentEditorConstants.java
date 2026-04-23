package com.sme.be_sme.modules.content.doceditor;

public final class DocumentEditorConstants {

    public static final String CONTENT_KIND_FILE = "FILE";
    public static final String CONTENT_KIND_EDITOR = "EDITOR";

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_ACTIVE = "ACTIVE";

    public static final String ACTION_DRAFT_SAVE = "DRAFT_SAVE";
    public static final String ACTION_DRAFT_CREATE = "DRAFT_CREATE";
    public static final String ACTION_PUBLISH = "PUBLISH";

    public static final String ACTION_COMMENT_ADD = "COMMENT_ADD";

    public static final String ACTION_COMMENT_REPLY = "COMMENT_REPLY";

    public static final String ACTION_LINK_ADD = "LINK_ADD";

    public static final String ACTION_LINK_REMOVE = "LINK_REMOVE";

    public static final String ACTION_ASSIGNMENT_ASSIGNED = "ASSIGNMENT_ASSIGNED";

    public static final String ACTION_ASSIGNMENT_REVOKED = "ASSIGNMENT_REVOKED";

    public static final String ACTION_ATTACHMENT_ADD = "ATTACHMENT_ADD";

    public static final String ACTION_ATTACHMENT_REMOVE = "ATTACHMENT_REMOVE";

    public static final String ACTION_ACCESS_RULE_ADD = "ACCESS_RULE_ADD";

    public static final String ACTION_ACCESS_RULE_REMOVE = "ACCESS_RULE_REMOVE";

    public static final String ACTION_COMMENT_DELETE = "COMMENT_DELETE";

    public static final String ACTION_COMMENT_UPDATE = "COMMENT_UPDATE";

    public static final String STATUS_READ = "READ";

    public static final String STATUS_DELETED = "DELETED";

    public static final String ASSIGNMENT_STATUS_ASSIGNED = "ASSIGNED";

    public static final String ASSIGNMENT_STATUS_REVOKED = "REVOKED";

    public static final String MEDIA_KIND_FILE = "FILE";

    public static final String MEDIA_KIND_VIDEO = "VIDEO";

    public static final String DEFAULT_LINK_TYPE = "RELATED";

    public static final int MAX_COMMENT_BODY_CHARS = 32000;

    /** Empty onboarding_id groups collaboration read receipts (not tied to an onboarding instance). */
    public static final String GENERAL_READ_ONBOARDING_ID = "";

    public static final String DEFAULT_EMPTY_JSON = "{}";

    /** In-app notification types (notifications.type) for document collaboration */
    public static final String NOTIFICATION_TYPE_DOCUMENT_ASSIGNED = "DOCUMENT_ASSIGNED";

    public static final String NOTIFICATION_TYPE_DOCUMENT_COMMENT = "DOCUMENT_COMMENT";

    public static final String NOTIFICATION_TYPE_DOCUMENT_COMMENT_REPLY = "DOCUMENT_COMMENT_REPLY";

    public static final String NOTIFICATION_REF_TYPE_DOCUMENT = "DOCUMENT";

    /** Max paths reported in version JSON compare metadata */
    public static final int VERSION_COMPARE_MAX_PATHS = 50;

    public static final int VERSION_COMPARE_MAX_DEPTH = 3;

    private DocumentEditorConstants() {
    }
}
