package com.sme.be_sme.modules.content.service;

import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentBlockEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class BlockOrderService {

    public List<DocumentBlockEntity> reorderSiblings(
            List<DocumentBlockEntity> siblings,
            String movingBlockId,
            String afterBlockId
    ) {
        List<DocumentBlockEntity> working = siblings == null ? new ArrayList<>() : new ArrayList<>(siblings);
        DocumentBlockEntity moving = null;
        for (DocumentBlockEntity row : working) {
            if (row != null && Objects.equals(movingBlockId, row.getDocumentBlockId())) {
                moving = row;
                break;
            }
        }
        if (moving != null) {
            working.remove(moving);
        }
        if (moving == null) {
            return working;
        }

        int insertIdx = 0;
        if (afterBlockId != null) {
            insertIdx = working.size();
            for (int i = 0; i < working.size(); i++) {
                if (Objects.equals(afterBlockId, working.get(i).getDocumentBlockId())) {
                    insertIdx = i + 1;
                    break;
                }
            }
        }
        working.add(Math.min(insertIdx, working.size()), moving);
        resequence(working);
        return working;
    }

    public void resequence(List<DocumentBlockEntity> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).setOrderKey(formatOrder(i + 1));
        }
    }

    public static String formatOrder(int ordinal) {
        return String.format("%06d", Math.max(1, ordinal));
    }
}
