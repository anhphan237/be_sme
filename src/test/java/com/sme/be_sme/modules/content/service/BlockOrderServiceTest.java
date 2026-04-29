package com.sme.be_sme.modules.content.service;

import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentBlockEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class BlockOrderServiceTest {

    private final BlockOrderService service = new BlockOrderService();

    @Test
    void shouldMoveBlockAfterTargetAndResequence() {
        List<DocumentBlockEntity> rows = new ArrayList<>();
        rows.add(block("a", "000001"));
        rows.add(block("b", "000002"));
        rows.add(block("c", "000003"));

        List<DocumentBlockEntity> reordered = service.reorderSiblings(rows, "a", "c");

        Assertions.assertEquals("b", reordered.get(0).getDocumentBlockId());
        Assertions.assertEquals("c", reordered.get(1).getDocumentBlockId());
        Assertions.assertEquals("a", reordered.get(2).getDocumentBlockId());
        Assertions.assertEquals("000001", reordered.get(0).getOrderKey());
        Assertions.assertEquals("000002", reordered.get(1).getOrderKey());
        Assertions.assertEquals("000003", reordered.get(2).getOrderKey());
    }

    private static DocumentBlockEntity block(String id, String orderKey) {
        DocumentBlockEntity row = new DocumentBlockEntity();
        row.setDocumentBlockId(id);
        row.setOrderKey(orderKey);
        return row;
    }
}
