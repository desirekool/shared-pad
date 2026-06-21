package com.syncdocs.controller;

import com.syncdocs.events.DocumentOperation;
import com.syncdocs.events.KafkaDocumentEvent;
import com.syncdocs.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class EditOperationController {

    private final KafkaProducerService kafkaProducerService;

    @MessageMapping("/document.{documentId}.edit")
    public void handleEdit(@DestinationVariable String documentId,
                           @Payload DocumentOperation operation,
                           SimpMessageHeaderAccessor headerAccessor) {
        String username = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "unknown";

        operation.setDocumentId(documentId);
        operation.setUserId(username);
        operation.setTimestamp(System.currentTimeMillis());

        KafkaDocumentEvent event = KafkaDocumentEvent.builder()
                .eventType("EDIT")
                .documentId(documentId)
                .userId(username)
                .timestamp(Instant.now())
                .payload(Map.of(
                        "type", operation.getType().name(),
                        "position", operation.getPosition(),
                        "text", operation.getText() != null ? operation.getText() : "",
                        "length", operation.getLength(),
                        "version", operation.getVersion()
                ))
                .build();

        kafkaProducerService.sendEditEvent(event);
        log.debug("Edit op: {} {} pos={} doc={} user={}",
                operation.getType(), operation.getText() != null ? "\"" + operation.getText() + "\"" : "",
                operation.getPosition(), documentId, username);
    }
}
