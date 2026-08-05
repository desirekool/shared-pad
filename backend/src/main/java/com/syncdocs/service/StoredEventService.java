package com.syncdocs.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.syncdocs.events.KafkaDocumentEvent;
import com.syncdocs.model.StoredEvent;
import com.syncdocs.repository.StoredEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoredEventService {

    private final StoredEventRepository storedEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void storeEvent(KafkaDocumentEvent event) {
        try {
            String payloadJson = event.getPayload() != null
                    ? objectMapper.writeValueAsString(event.getPayload())
                    : null;
            Long docId = event.getDocumentId() != null ? Long.valueOf(event.getDocumentId()) : null;

            StoredEvent stored = StoredEvent.builder()
                    .eventType(event.getEventType())
                    .documentId(docId)
                    .userId(event.getUserId())
                    .sessionId(event.getSessionId())
                    .payload(payloadJson)
                    .build();

            storedEventRepository.save(stored);
        } catch (JacksonException e) {
            log.error("Failed to serialize event payload: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<StoredEvent> getEventsSince(Long documentId, Long afterEventId) {
        return storedEventRepository.findByDocumentIdAndIdGreaterThanOrderByIdAsc(documentId, afterEventId);
    }

    @Transactional(readOnly = true)
    public List<StoredEvent> getAllEvents(Long documentId) {
        return storedEventRepository.findByDocumentIdOrderByIdAsc(documentId);
    }
}
