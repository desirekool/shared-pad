package com.syncdocs.service;

import com.syncdocs.events.KafkaDocumentEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaProducerServiceTest {

    @Mock private KafkaTemplate<String, KafkaDocumentEvent> kafkaTemplate;

    @InjectMocks private KafkaProducerService kafkaProducerService;

    @Test
    void send_ShouldPublishToCorrectTopic() {
        KafkaDocumentEvent event = KafkaDocumentEvent.builder()
                .eventType("EDIT")
                .documentId("1")
                .userId("alice")
                .timestamp(Instant.now())
                .payload(Map.of("type", "INSERT"))
                .build();

        kafkaProducerService.send("document.edit", event);

        verify(kafkaTemplate).send(eq("document.edit"), eq("1"), eq(event));
    }

    @Test
    void sendEditEvent_ShouldPublishToEditTopic() {
        KafkaDocumentEvent event = KafkaDocumentEvent.builder().build();

        kafkaProducerService.sendEditEvent(event);

        verify(kafkaTemplate).send(eq("document.edit"), any(), eq(event));
    }

    @Test
    void sendPresenceEvent_ShouldPublishToPresenceTopic() {
        KafkaDocumentEvent event = KafkaDocumentEvent.builder().build();

        kafkaProducerService.sendPresenceEvent(event);

        verify(kafkaTemplate).send(eq("user.presence"), any(), eq(event));
    }
}
