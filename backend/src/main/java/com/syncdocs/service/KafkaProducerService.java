package com.syncdocs.service;

import com.syncdocs.events.KafkaDocumentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, KafkaDocumentEvent> kafkaTemplate;

    public void send(String topic, KafkaDocumentEvent event) {
        CompletableFuture<SendResult<String, KafkaDocumentEvent>> future =
                kafkaTemplate.send(topic, event.getDocumentId(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send event {} to topic {}: {}",
                        event.getEventType(), topic, ex.getMessage());
            } else {
                log.debug("Sent event {} to topic {} (offset {})",
                        event.getEventType(), topic, result.getRecordMetadata().offset());
            }
        });
    }

    public void sendEditEvent(KafkaDocumentEvent event) {
        send("document.edit", event);
    }

    public void sendSaveEvent(KafkaDocumentEvent event) {
        send("document.save", event);
    }

    public void sendCreatedEvent(KafkaDocumentEvent event) {
        send("document.created", event);
    }

    public void sendDeletedEvent(KafkaDocumentEvent event) {
        send("document.deleted", event);
    }

    public void sendPresenceEvent(KafkaDocumentEvent event) {
        send("user.presence", event);
    }

    public void sendAuditEvent(KafkaDocumentEvent event) {
        send("audit.events", event);
    }
}
