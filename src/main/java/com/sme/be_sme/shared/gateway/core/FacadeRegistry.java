package com.sme.be_sme.shared.gateway.core;

import com.sme.be_sme.shared.gateway.annotation.OperationHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class FacadeRegistry {

    private final Map<String, OperationFacade> map = new HashMap<>();

    public FacadeRegistry(List<OperationFacade> facades) {
        for (OperationFacade facade : facades) {
            OperationHandler ann = facade.getClass().getAnnotation(OperationHandler.class);
            if (ann == null) {
                throw new IllegalStateException("Missing @OperationHandler on " + facade.getClass().getName());
            }
            String op = ann.value();
            if (map.containsKey(op)) {
                throw new IllegalStateException("Duplicate operationType: " + op);
            }
            map.put(op, facade);
            log.info("Registered opType [{}] -> {}", op, facade.getClass().getSimpleName());
        }
    }

    public OperationFacade get(String operationType) {
        OperationFacade f = map.get(operationType);
        if (f == null) {
            throw new IllegalArgumentException("Unsupported operationType: " + operationType);
        }
        return f;
    }
}
