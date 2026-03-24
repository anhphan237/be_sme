package com.sme.be_sme.modules.notification.config;

import com.sme.be_sme.shared.security.JwtPrincipal;
import com.sme.be_sme.shared.security.JwtService;
import com.sme.be_sme.modules.notification.service.NotificationPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for /ws/notifications. Client connects with ?token=JWT.
 * On connect, session is registered by companyId + userId from JWT.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private final JwtService jwtService;
    private final NotificationPushService notificationPushService;

    private final Map<WebSocketSession, String> sessionKeys = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token = extractToken(session);
        if (!StringUtils.hasText(token)) {
            log.warn("NotificationWebSocket: no token, closing");
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        try {
            JwtPrincipal principal = jwtService.verify(token);
            String key = principal.getTenantId() + ":" + principal.getUserId();
            sessionKeys.put(session, key);
            notificationPushService.registerSession(principal.getTenantId(), principal.getUserId(), session);
            log.debug("NotificationWebSocket: connected {} for {}", session.getId(), key);
        } catch (SecurityException e) {
            log.warn("NotificationWebSocket: invalid token: {}", e.getMessage());
            session.close(CloseStatus.POLICY_VIOLATION);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String key = sessionKeys.remove(session);
        if (key != null) {
            int colon = key.indexOf(':');
            if (colon > 0) {
                String companyId = key.substring(0, colon);
                String userId = key.substring(colon + 1);
                notificationPushService.unregisterSession(companyId, userId);
            }
        }
    }

    private String extractToken(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) return null;
        String query = uri.getQuery();
        if (!StringUtils.hasText(query)) return null;
        for (String param : query.split("&")) {
            int eq = param.indexOf('=');
            if (eq > 0 && "token".equals(param.substring(0, eq).trim())) {
                return param.substring(eq + 1).trim();
            }
        }
        return null;
    }
}
