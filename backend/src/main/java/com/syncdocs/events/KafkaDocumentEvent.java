package com.syncdocs.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KafkaDocumentEvent {

    private String eventType;
    private String documentId;
    private String userId;
    private String sessionId;
    private Instant timestamp;
    private Map<String, Object> payload;
}
