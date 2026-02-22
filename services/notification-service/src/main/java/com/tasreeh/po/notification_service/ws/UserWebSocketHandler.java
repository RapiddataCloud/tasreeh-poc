package com.tasreeh.po.notification_service.ws;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserWebSocketHandler extends TextWebSocketHandler {

    private final SessionRegistry registry;
    private final EventBuffer eventBuffer;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("========================================================");
        log.info("[WS-HANDLER] New WebSocket connection attempt");
        log.info("[WS-HANDLER]   sessionId = {}", session.getId());
        log.info("[WS-HANDLER]   URI       = {}", session.getUri());
        log.info("========================================================");

        String token = extractQueryParam(session.getUri(), "token");

        if (token == null || token.isBlank()) {
            log.warn("[WS-HANDLER] ❌ Connection rejected: missing token");
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        log.info("[WS-HANDLER] Token received, length = {}", token.length());

        // Parse JWT payload (no cryptographic verification — just extract claims)
        String userId = null;
        String username = null;
        List<String> roles = List.of();

        try {
            String[] parts = token.split("\\.");
            if (parts.length >= 2) {
                String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
                userId = extractJsonString(payloadJson, "sub");
                username = extractJsonString(payloadJson, "preferred_username");
                roles = extractRealmRoles(payloadJson);
                log.info("[WS-HANDLER] JWT parsed successfully");
                log.info("[WS-HANDLER]   userId   = {}", userId);
                log.info("[WS-HANDLER]   username = {}", username);
                log.info("[WS-HANDLER]   roles    = {}", roles);
            }
        } catch (Exception e) {
            log.error("[WS-HANDLER] ❌ Failed to parse JWT", e);
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        if (userId == null || userId.isBlank()) {
            log.warn("[WS-HANDLER] ❌ Connection rejected: could not extract userId from token");
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        // Register session by userId
        session.getAttributes().put("userId", userId);
        session.getAttributes().put("username", username);
        session.getAttributes().put("isManager", roles.contains("manager"));
        registry.add(userId, session);
        log.info("[WS-HANDLER] Registered session for userId={}", userId);

        // Register by role for broadcasts
        for (String role : roles) {
            registry.addByRole(role, session);
            log.info("[WS-HANDLER] Registered session for role={}", role);
        }

        log.info("========================================================");
        log.info("[WS-HANDLER] ✅ WebSocket CONNECTED");
        log.info("[WS-HANDLER]   userId   = {}", userId);
        log.info("[WS-HANDLER]   username = {}", username);
        log.info("[WS-HANDLER]   roles    = {}", roles);
        log.info("[WS-HANDLER]   session  = {}", session.getId());
        log.info("========================================================");

        // State recovery: replay missed events if lastEventTime is provided
        String lastEventTime = extractQueryParam(session.getUri(), "lastEventTime");
        if (lastEventTime != null && !lastEventTime.isBlank()) {
            try {
                Instant since = Instant.parse(lastEventTime);
                boolean isManager = roles.contains("manager");
                List<String> missedEvents = eventBuffer.getEventsSince(since, userId, isManager);
                log.info("[WS-HANDLER] Replaying {} missed events since {}", missedEvents.size(), lastEventTime);
                for (String payload : missedEvents) {
                    session.sendMessage(new TextMessage(payload));
                }
            } catch (Exception e) {
                log.warn("[WS-HANDLER] Failed to replay events: {}", e.getMessage());
            }
        }
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Handle recovery request: {"type":"recover","since":"2026-02-20T..."}
        String payload = message.getPayload();
        if (payload.contains("\"recover\"")) {
            try {
                String since = extractJsonString(payload, "since");
                if (since != null) {
                    String userId = (String) session.getAttributes().get("userId");
                    Boolean isManager = (Boolean) session.getAttributes().getOrDefault("isManager", false);
                    List<String> missed = eventBuffer.getEventsSince(Instant.parse(since), userId, isManager);
                    log.info("[WS-HANDLER] Recovery request: replaying {} events since {}", missed.size(), since);
                    for (String evt : missed) {
                        session.sendMessage(new TextMessage(evt));
                    }
                }
            } catch (Exception e) {
                log.warn("[WS-HANDLER] Failed to handle recovery request: {}", e.getMessage());
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = (String) session.getAttributes().get("userId");
        if (userId != null) {
            registry.remove(userId, session);
            log.info("[WS-HANDLER] WebSocket DISCONNECTED userId={}, sessionId={}, status={}",
                    userId, session.getId(), status);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("[WS-HANDLER] Transport error for session {}: {}",
                session.getId(), exception.getMessage());
    }

    private String extractQueryParam(URI uri, String paramName) {
        if (uri == null || uri.getQuery() == null) return null;
        for (String kv : uri.getQuery().split("&")) {
            String[] parts = kv.split("=", 2);
            if (parts.length == 2 && parts[0].equals(paramName)) return parts[1];
        }
        return null;
    }

    private String extractJsonString(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIdx = json.indexOf(searchKey);
        if (keyIdx < 0) return null;

        int colonIdx = json.indexOf(":", keyIdx + searchKey.length());
        if (colonIdx < 0) return null;

        int startQuote = json.indexOf("\"", colonIdx + 1);
        if (startQuote < 0) return null;

        int endQuote = json.indexOf("\"", startQuote + 1);
        if (endQuote < 0) return null;

        return json.substring(startQuote + 1, endQuote);
    }

    private List<String> extractRealmRoles(String json) {
        try {
            int raIdx = json.indexOf("\"realm_access\"");
            if (raIdx < 0) return List.of();

            int rolesIdx = json.indexOf("\"roles\"", raIdx);
            if (rolesIdx < 0) return List.of();

            int arrayStart = json.indexOf("[", rolesIdx);
            int arrayEnd = json.indexOf("]", arrayStart);
            if (arrayStart < 0 || arrayEnd < 0) return List.of();

            String rolesArray = json.substring(arrayStart + 1, arrayEnd);
            return java.util.Arrays.stream(rolesArray.split(","))
                    .map(s -> s.trim().replace("\"", ""))
                    .filter(s -> !s.isBlank())
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }
}
