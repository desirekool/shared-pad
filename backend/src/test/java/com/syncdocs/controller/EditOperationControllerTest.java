package com.syncdocs.controller;

import com.syncdocs.events.DocumentOperation;
import com.syncdocs.events.DocumentOperation.OperationType;
import com.syncdocs.service.KafkaProducerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.GenericMessage;

import java.security.Principal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EditOperationControllerTest {

    @Mock private KafkaProducerService kafkaProducerService;
    @InjectMocks private EditOperationController controller;

    @Captor private ArgumentCaptor<com.syncdocs.events.KafkaDocumentEvent> eventCaptor;

    @Test
    void handleEdit_ShouldSendEditEvent() {
        DocumentOperation op = new DocumentOperation();
        op.setType(OperationType.INSERT);
        op.setPosition(5);
        op.setText("hello");
        op.setVersion(1L);

        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create();
        headers.setUser(() -> "testuser");
        headers.setSessionId("sess1");

        controller.handleEdit("doc123", op, headers);

        verify(kafkaProducerService).sendEditEvent(eventCaptor.capture());
        var event = eventCaptor.getValue();
        assertEquals("EDIT", event.getEventType());
        assertEquals("doc123", event.getDocumentId());
        assertEquals("testuser", event.getUserId());
        assertNotNull(event.getTimestamp());
        assertNotNull(event.getPayload());
    }

    @Test
    void handleEdit_ShouldHandleUnknownUser() {
        DocumentOperation op = new DocumentOperation();
        op.setType(OperationType.DELETE);
        op.setPosition(0);
        op.setVersion(2L);

        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create();
        headers.setSessionId("sess2");

        controller.handleEdit("doc456", op, headers);

        verify(kafkaProducerService).sendEditEvent(eventCaptor.capture());
        var event = eventCaptor.getValue();
        assertEquals("unknown", event.getUserId());
    }

    @Test
    void handleEdit_WithNegativePosition_ShouldStillSend() {
        DocumentOperation op = new DocumentOperation();
        op.setType(OperationType.INSERT);
        op.setPosition(-1);
        op.setText("neg");
        op.setVersion(1L);

        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create();
        headers.setUser(() -> "testuser");
        headers.setSessionId("sess3");

        controller.handleEdit("doc789", op, headers);

        verify(kafkaProducerService).sendEditEvent(eventCaptor.capture());
        var event = eventCaptor.getValue();
        assertEquals(-1, event.getPayload().get("position"));
    }

    @Test
    void handleEdit_WithNullText_ShouldSend() {
        DocumentOperation op = new DocumentOperation();
        op.setType(OperationType.INSERT);
        op.setPosition(0);
        op.setText(null);
        op.setVersion(1L);

        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create();
        headers.setUser(() -> "testuser");
        headers.setSessionId("sess4");

        controller.handleEdit("doc789", op, headers);

        verify(kafkaProducerService).sendEditEvent(eventCaptor.capture());
        var event = eventCaptor.getValue();
        assertEquals("testuser", event.getUserId());
    }

}
