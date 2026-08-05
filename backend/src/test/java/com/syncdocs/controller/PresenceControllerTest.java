package com.syncdocs.controller;

import com.syncdocs.service.KafkaProducerService;
import com.syncdocs.service.PresenceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PresenceControllerTest {

    @Mock private PresenceService presenceService;
    @Mock private KafkaProducerService kafkaProducerService;
    @InjectMocks private PresenceController controller;

    @Captor private ArgumentCaptor<com.syncdocs.events.KafkaDocumentEvent> eventCaptor;

    @Test
    void join_ShouldTrackAndBroadcast() {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create();
        headers.setUser(() -> "alice");

        controller.join("doc1", Map.of("username", "Alice"), headers);

        verify(presenceService).userJoined("doc1", "alice", "Alice");
        verify(kafkaProducerService).sendPresenceEvent(eventCaptor.capture());
        assertEquals("USER_JOINED", eventCaptor.getValue().getEventType());
    }

    @Test
    void leave_ShouldUntrackAndBroadcast() {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create();
        headers.setUser(() -> "bob");

        controller.leave("doc1", headers);

        verify(presenceService).userLeft("doc1", "bob");
        verify(kafkaProducerService).sendPresenceEvent(eventCaptor.capture());
        assertEquals("USER_LEFT", eventCaptor.getValue().getEventType());
    }

    @Test
    void updateCursor_ShouldUpdateAndBroadcast() {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create();
        headers.setUser(() -> "alice");

        Map<String, Object> payload = Map.of(
                "cursor", Map.of("line", 3, "column", 10)
        );

        controller.updateCursor("doc1", payload, headers);

        verify(presenceService).updateCursor(eq("doc1"), eq("alice"), any());
        verify(kafkaProducerService).sendPresenceEvent(eventCaptor.capture());
        assertEquals("CURSOR_UPDATE", eventCaptor.getValue().getEventType());
    }

    @Test
    void typing_ShouldSetTypingAndBroadcast() {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create();
        headers.setUser(() -> "alice");

        controller.typing("doc1", Map.of("typing", true), headers);

        verify(presenceService).setTyping("doc1", "alice", true);
        verify(kafkaProducerService).sendPresenceEvent(eventCaptor.capture());
        assertEquals("TYPING", eventCaptor.getValue().getEventType());
    }
}
