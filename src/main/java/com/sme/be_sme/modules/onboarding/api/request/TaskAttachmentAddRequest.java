package com.sme.be_sme.modules.onboarding.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskAttachmentAddRequest {
    private String taskId;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private Long fileSizeBytes;
}
