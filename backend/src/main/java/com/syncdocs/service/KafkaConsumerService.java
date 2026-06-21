package com.syncdocs.service;

import com.syncdocs.events.KafkaDocumentEvent;
import com.syncdocs.events.OperationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ConflictResolutionService conflictResolutionService;
    private final EditHistoryService editHistoryService;

    @KafkaListener(topics = "document.edit", groupId = "syncdocs-group")
    public void handleEditEvent(KafkaDocumentEvent event) {
        String docId = event.getDocumentId();
        String userId = event.getUserId();
        Map<String, Object> payload = event.getPayload();

        long operationVersion = payload.get("version") instanceof Number
                ? ((Number) payload.get("version")).longValue() : 0;
        String type = (String) payload.getOrDefault("type", "INSERT");
        int position = payload.get("position") instanceof Number
                ? ((Number) payload.get("position")).intValue() : 0;
        String text = (String) payload.getOrDefault("text", "");
        int length = payload.get("length") instanceof Number
                ? ((Number) payload.get("length")).intValue() : 0;

        boolean accepted = conflictResolutionService.validateEdit(docId, operationVersion);

        if (accepted) {
            editHistoryService.recordEdit(
                    Long.parseLong(docId), userId, type, position, text, length, operationVersion);

            log.debug("Edit accepted: doc={} user={} version={}", docId, userId, operationVersion);
            broadcast(event, "/topic/document." + docId);
        } else {
            log.warn("Edit rejected (stale): doc={} user={} version={}", docId, userId, operationVersion);
            OperationResult rejection = OperationResult.builder()
                    .documentId(docId)
                    .userId(userId)
                    .accepted(false)
                    .reason("Stale version. Re-fetch document and retry.")
                    .build();
            messagingTemplate.convertAndSendToUser(userId, "/queue/errors", rejection);
        }
    }

    @KafkaListener(topics = "document.save", groupId = "syncdocs-group")
    public void handleSaveEvent(KafkaDocumentEvent event) {
        log.info("Save event: doc={} user={}", event.getDocumentId(), event.getUserId());
        broadcast(event, "/topic/document." + event.getDocumentId());
    }

    @KafkaListener(topics = "document.created", groupId = "syncdocs-group")
    public void handleCreatedEvent(KafkaDocumentEvent event) {
        log.info("Created event: doc={} user={}", event.getDocumentId(), event.getUserId());
        broadcast(event, "/topic/documents");
    }

    @KafkaListener(topics = "document.deleted", groupId = "syncdocs-group")
    public void handleDeletedEvent(KafkaDocumentEvent event) {
        log.info("Deleted event: doc={} user={}", event.getDocumentId(), event.getUserId());
        broadcast(event, "/topic/documents");
    }

    @KafkaListener(topics = "user.presence", groupId = "syncdocs-group")
    public void handlePresenceEvent(KafkaDocumentEvent event) {
        String docId = event.getDocumentId();
        if (docId != null) {
            broadcast(event, "/topic/document." + docId + ".presence");
        }
    }

    private void broadcast(KafkaDocumentEvent event, String destination) {
        try {
            messagingTemplate.convertAndSend(destination, event);
        } catch (Exception e) {
            log.error("Failed to broadcast event to {}: {}", destination, e.getMessage());
        }
    }
}
