package com.sme.be_sme.modules.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages WebSocket sessions and pushes notifications to connected users.
 * Key: companyId + ":" + userId
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPushService {

    private final ObjectMapper objectMapper;

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void registerSession(String companyId, String userId, WebSocketSession session) {
        String key = key(companyId, userId);
        sessions.put(key, session);
        log.debug("NotificationPushService: registered session for {}", key);
    }

    public void unregisterSession(String companyId, String userId) {
        sessions.remove(key(companyId, userId));
    }

    public void unregisterSession(WebSocketSession session) {
        sessions.entrySet().removeIf(e -> e.getValue().equals(session));
    }

    /**
     * Push notification payload to user if they are connected.
     */
    public void pushToUser(String companyId, String userId, ObjectNode payload) {
        String key = key(companyId, userId);
        WebSocketSession session = sessions.get(key);
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(payload);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.warn("NotificationPushService: failed to push to {}: {}", key, e.getMessage());
            sessions.remove(key);
        }
    }

    private static String key(String companyId, String userId) {
        return (companyId != null ? companyId : "") + ":" + (userId != null ? userId : "");
    }
}
