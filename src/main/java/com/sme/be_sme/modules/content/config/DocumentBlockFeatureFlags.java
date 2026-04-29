package com.sme.be_sme.modules.content.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DocumentBlockFeatureFlags {

    @Value("${features.document.blocks.write-enabled:true}")
    private boolean writeEnabled;

    @Value("${features.document.blocks.read-enabled:true}")
    private boolean readEnabled;

    public boolean isWriteEnabled() {
        return writeEnabled;
    }

    public boolean isReadEnabled() {
        return readEnabled;
    }
}
