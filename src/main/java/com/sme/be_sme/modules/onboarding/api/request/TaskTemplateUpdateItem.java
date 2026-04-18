package com.sme.be_sme.modules.onboarding.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskTemplateUpdateItem extends TaskTemplateCreateItem {
    private String taskTemplateId;
}
