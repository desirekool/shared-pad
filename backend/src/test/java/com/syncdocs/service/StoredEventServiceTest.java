package com.syncdocs.service;

import tools.jackson.databind.ObjectMapper;
import com.syncdocs.events.KafkaDocumentEvent;
import com.syncdocs.model.StoredEvent;
import com.syncdocs.repository.StoredEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoredEventServiceTest {

    @Mock private StoredEventRepository storedEventRepository;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks private StoredEventService storedEventService;

    @Captor private ArgumentCaptor<StoredEvent> eventCaptor;

    @Test
    void storeEvent_ShouldSaveWithSerializedPayload() throws Exception {
        KafkaDocumentEvent kafkaEvent = KafkaDocumentEvent.builder()
                .eventType("EDIT")
                .documentId("1")
                .userId("alice")
                .sessionId("sess-1")
                .timestamp(1000L)
                .payload(Map.of("type", "INSERT", "position", 0, "text", "Hello", "length", 0, "version", 1))
                .build();

        when(objectMapper.writeValueAsString(kafkaEvent.getPayload())).thenReturn("{\"type\":\"INSERT\"}");

        storedEventService.storeEvent(kafkaEvent);

        verify(storedEventRepository).save(eventCaptor.capture());
        StoredEvent saved = eventCaptor.getValue();
        assertEquals("EDIT", saved.getEventType());
        assertEquals(1L, saved.getDocumentId());
        assertEquals("alice", saved.getUserId());
        assertEquals("sess-1", saved.getSessionId());
        assertEquals("{\"type\":\"INSERT\"}", saved.getPayload());
    }

    @Test
    void storeEvent_ShouldHandleNullPayload() {
        KafkaDocumentEvent kafkaEvent = KafkaDocumentEvent.builder()
                .eventType("SAVE")
                .documentId("2")
                .userId("bob")
                .build();

        storedEventService.storeEvent(kafkaEvent);

        verify(storedEventRepository).save(eventCaptor.capture());
        assertNull(eventCaptor.getValue().getPayload());
    }

    @Test
    void getEventsSince_ShouldReturnFilteredEvents() {
        StoredEvent ev2 = StoredEvent.builder().id(11L).eventType("EDIT").build();
        when(storedEventRepository.findByDocumentIdAndIdGreaterThanOrderByIdAsc(1L, 10L))
                .thenReturn(List.of(ev2));

        List<StoredEvent> result = storedEventService.getEventsSince(1L, 10L);

        assertEquals(1, result.size());
        assertEquals(11L, result.get(0).getId());
    }

    @Test
    void getAllEvents_ShouldReturnAllForDocument() {
        StoredEvent ev = StoredEvent.builder().id(1L).eventType("EDIT").build();
        when(storedEventRepository.findByDocumentIdOrderByIdAsc(1L))
                .thenReturn(List.of(ev));

        List<StoredEvent> result = storedEventService.getAllEvents(1L);

        assertEquals(1, result.size());
    }
}
