package com.syncdocs.service;

import com.syncdocs.events.KafkaDocumentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

    private final SimpMessagingTemplate messagingTemplate;

    @KafkaListener(topics = "document.edit", groupId = "syncdocs-group")
    public void handleEditEvent(KafkaDocumentEvent event) {
        log.debug("Edit event: doc={} user={}", event.getDocumentId(), event.getUserId());
        broadcast(event, "/topic/document." + event.getDocumentId());
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
