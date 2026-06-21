package com.syncdocs.controller;

import com.syncdocs.events.KafkaDocumentEvent;
import com.syncdocs.events.UserPresence;
import com.syncdocs.service.KafkaProducerService;
import com.syncdocs.service.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class PresenceController {

    private final PresenceService presenceService;
    private final KafkaProducerService kafkaProducerService;

    @MessageMapping("/documents.join.{documentId}")
    public void join(@DestinationVariable String documentId,
                     @Payload Map<String, Object> payload,
                     SimpMessageHeaderAccessor headerAccessor) {
        String userId = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "anonymous";
        String username = payload.containsKey("username") ? (String) payload.get("username") : userId;

        presenceService.userJoined(documentId, userId, username);

        KafkaDocumentEvent event = KafkaDocumentEvent.builder()
                .eventType("USER_JOINED")
                .documentId(documentId)
                .userId(userId)
                .timestamp(Instant.now())
                .payload(Map.of("username", username, "status", "ONLINE"))
                .build();

        kafkaProducerService.sendPresenceEvent(event);
    }

    @MessageMapping("/documents.leave.{documentId}")
    public void leave(@DestinationVariable String documentId,
                      SimpMessageHeaderAccessor headerAccessor) {
        String userId = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "anonymous";

        presenceService.userLeft(documentId, userId);

        KafkaDocumentEvent event = KafkaDocumentEvent.builder()
                .eventType("USER_LEFT")
                .documentId(documentId)
                .userId(userId)
                .timestamp(Instant.now())
                .payload(Map.of("status", "OFFLINE"))
                .build();

        kafkaProducerService.sendPresenceEvent(event);
    }

    @MessageMapping("/documents.{documentId}.cursor")
    public void updateCursor(@DestinationVariable String documentId,
                             @Payload Map<String, Object> payload,
                             SimpMessageHeaderAccessor headerAccessor) {
        String userId = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "anonymous";

        if (payload.containsKey("cursor")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> cursorData = (Map<String, Object>) payload.get("cursor");
            UserPresence.CursorPosition cursor = UserPresence.CursorPosition.builder()
                    .line((int) cursorData.getOrDefault("line", 0))
                    .column((int) cursorData.getOrDefault("column", 0))
                    .build();
            presenceService.updateCursor(documentId, userId, cursor);
        }

        if (payload.containsKey("selection")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> selData = (Map<String, Object>) payload.get("selection");
            UserPresence.SelectionRange sel = UserPresence.SelectionRange.builder()
                    .startLine((int) selData.getOrDefault("startLine", 0))
                    .startColumn((int) selData.getOrDefault("startColumn", 0))
                    .endLine((int) selData.getOrDefault("endLine", 0))
                    .endColumn((int) selData.getOrDefault("endColumn", 0))
                    .build();
            presenceService.updateSelection(documentId, userId, sel);
        }

        KafkaDocumentEvent event = KafkaDocumentEvent.builder()
                .eventType("CURSOR_UPDATE")
                .documentId(documentId)
                .userId(userId)
                .timestamp(Instant.now())
                .payload(payload)
                .build();

        kafkaProducerService.sendPresenceEvent(event);
    }

    @MessageMapping("/documents.{documentId}.typing")
    public void typing(@DestinationVariable String documentId,
                       @Payload Map<String, Object> payload,
                       SimpMessageHeaderAccessor headerAccessor) {
        String userId = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "anonymous";
        boolean typing = payload.containsKey("typing") && (boolean) payload.get("typing");

        presenceService.setTyping(documentId, userId, typing);

        KafkaDocumentEvent event = KafkaDocumentEvent.builder()
                .eventType("TYPING")
                .documentId(documentId)
                .userId(userId)
                .timestamp(Instant.now())
                .payload(Map.of("typing", typing))
                .build();

        kafkaProducerService.sendPresenceEvent(event);
    }
}
