package com.syncdocs.service;

import com.syncdocs.events.KafkaDocumentEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaConsumerServiceTest {

    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private ConflictResolutionService conflictResolutionService;
    @Mock private EditHistoryService editHistoryService;
    @Mock private StoredEventService storedEventService;

    @InjectMocks private KafkaConsumerService kafkaConsumerService;

    @Test
    void handleEditEvent_ShouldPersistEvent() {
        KafkaDocumentEvent event = KafkaDocumentEvent.builder()
                .eventType("EDIT")
                .documentId("1")
                .userId("alice")
                .timestamp(System.currentTimeMillis())
                .payload(Map.of("type", "INSERT", "position", 0, "text", "Hi", "length", 0, "version", 1))
                .build();

        when(conflictResolutionService.validateEdit("1", 1L)).thenReturn(false);

        kafkaConsumerService.handleEditEvent(event);

        verify(storedEventService).storeEvent(event);
    }

    @Test
    void handleSaveEvent_ShouldPersistEvent() {
        KafkaDocumentEvent event = KafkaDocumentEvent.builder().eventType("SAVE").documentId("1").userId("alice").build();

        kafkaConsumerService.handleSaveEvent(event);

        verify(storedEventService).storeEvent(event);
    }

    @Test
    void handleCreatedEvent_ShouldPersistEvent() {
        KafkaDocumentEvent event = KafkaDocumentEvent.builder().eventType("CREATED").documentId("1").userId("alice").build();

        kafkaConsumerService.handleCreatedEvent(event);

        verify(storedEventService).storeEvent(event);
    }

    @Test
    void handleDeletedEvent_ShouldPersistEvent() {
        KafkaDocumentEvent event = KafkaDocumentEvent.builder().eventType("DELETED").documentId("1").userId("alice").build();

        kafkaConsumerService.handleDeletedEvent(event);

        verify(storedEventService).storeEvent(event);
    }

    @Test
    void handleEditEvent_ShouldBroadcastWhenAccepted() {
        KafkaDocumentEvent event = KafkaDocumentEvent.builder()
                .eventType("EDIT")
                .documentId("1")
                .userId("alice")
                .timestamp(System.currentTimeMillis())
                .payload(Map.of(
                        "type", "INSERT",
                        "position", 0,
                        "text", "Hello",
                        "length", 0,
                        "version", 1
                ))
                .build();

        when(conflictResolutionService.validateEdit("1", 1L)).thenReturn(true);

        kafkaConsumerService.handleEditEvent(event);

        verify(messagingTemplate).convertAndSend(eq("/topic/document.1"), eq(event));
    }

    @Test
    void handleEditEvent_ShouldNotBroadcastWhenRejected() {
        KafkaDocumentEvent event = KafkaDocumentEvent.builder()
                .eventType("EDIT")
                .documentId("1")
                .userId("alice")
                .payload(Map.of("version", 0))
                .build();

        when(conflictResolutionService.validateEdit("1", 0L)).thenReturn(false);

        kafkaConsumerService.handleEditEvent(event);

        verify(messagingTemplate, never()).convertAndSend(eq("/topic/document.1"), eq(event));
    }

    @Test
    void handlePresenceEvent_ShouldBroadcastToPresenceTopic() {
        KafkaDocumentEvent event = KafkaDocumentEvent.builder()
                .eventType("USER_JOINED")
                .documentId("1")
                .userId("alice")
                .build();

        kafkaConsumerService.handlePresenceEvent(event);

        verify(messagingTemplate).convertAndSend(eq("/topic/document.1.presence"), eq(event));
    }

    @Test
    void handleSaveEvent_ShouldBroadcast() {
        KafkaDocumentEvent event = KafkaDocumentEvent.builder()
                .eventType("SAVE")
                .documentId("2")
                .userId("bob")
                .build();

        kafkaConsumerService.handleSaveEvent(event);

        verify(messagingTemplate).convertAndSend(eq("/topic/document.2"), eq(event));
    }

    @Test
    void handleCreatedEvent_ShouldBroadcastToDocumentsTopic() {
        KafkaDocumentEvent event = KafkaDocumentEvent.builder()
                .eventType("CREATED")
                .documentId("3")
                .userId("alice")
                .build();

        kafkaConsumerService.handleCreatedEvent(event);

        verify(messagingTemplate).convertAndSend(eq("/topic/documents"), eq(event));
    }

    @Test
    void handleDeletedEvent_ShouldBroadcastToDocumentsTopic() {
        KafkaDocumentEvent event = KafkaDocumentEvent.builder()
                .eventType("DELETED")
                .documentId("4")
                .userId("bob")
                .build();

        kafkaConsumerService.handleDeletedEvent(event);

        verify(messagingTemplate).convertAndSend(eq("/topic/documents"), eq(event));
    }
}
