package com.sme.be_sme.shared.gateway.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.shared.gateway.annotation.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class FacadeRegistry {

    private final Map<String, OperationFacade> map = new HashMap<>();
    private final ObjectMapper objectMapper;

    public FacadeRegistry(List<OperationFacadeProvider> facades, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        for (OperationFacadeProvider facade : facades) {
            registerFacadeMethods(facade);
        }
    }

    private void registerFacadeMethods(OperationFacadeProvider facade) {
        Method[] methods = facade.getClass().getMethods();
        for (Method method : methods) {
            OperationType ann = resolveOperationType(method);
            if (ann == null) {
                continue;
            }
            String op = ann.value();
            if (map.containsKey(op)) {
                throw new IllegalStateException("Duplicate operationType: " + op);
            }
            map.put(op, new MethodOperationFacade(facade, method, objectMapper));
            log.info("Registered opType [{}] -> {}.{}", op, facade.getClass().getSimpleName(), method.getName());
        }
    }

    private OperationType resolveOperationType(Method method) {
        OperationType direct = method.getAnnotation(OperationType.class);
        if (direct != null) {
            return direct;
        }
        Class<?> declaring = method.getDeclaringClass();
        for (Class<?> iface : declaring.getInterfaces()) {
            try {
                Method ifaceMethod = iface.getMethod(method.getName(), method.getParameterTypes());
                OperationType ann = ifaceMethod.getAnnotation(OperationType.class);
                if (ann != null) {
                    return ann;
                }
            } catch (NoSuchMethodException ignored) {
                // skip
            }
        }
        return null;
    }

    public OperationFacade get(String operationType) {
        OperationFacade f = map.get(operationType);
        if (f == null) {
            throw new IllegalArgumentException("Unsupported operationType: " + operationType);
        }
        return f;
    }

    private static class MethodOperationFacade implements OperationFacade {
        private final Object target;
        private final Method method;
        private final ObjectMapper objectMapper;

        private MethodOperationFacade(Object target, Method method, ObjectMapper objectMapper) {
            this.target = target;
            this.method = method;
            this.objectMapper = objectMapper;
        }

        @Override
        public Object execute(String tenantId, String requestId, JsonNode payload) {
            Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length != 1) {
                throw new IllegalStateException("Operation method must have 1 param: request");
            }
            Object arg = payload;
            if (!JsonNode.class.equals(paramTypes[0])) {
                arg = objectMapper.convertValue(payload, paramTypes[0]);
            }
            BizContextHolder.set(BizContext.of(tenantId, requestId, payload));
            try {
                return method.invoke(target, arg);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Cannot access operation method: " + method.getName(), e);
            } catch (InvocationTargetException e) {
                Throwable targetEx = e.getTargetException();
                if (targetEx instanceof RuntimeException) {
                    throw (RuntimeException) targetEx;
                }
                throw new IllegalStateException("Operation method failed: " + method.getName(), targetEx);
            } finally {
                BizContextHolder.clear();
            }
        }
    }
}
