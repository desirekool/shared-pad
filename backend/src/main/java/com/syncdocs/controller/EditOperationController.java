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

import java.util.HashMap;
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

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", operation.getType() != null ? operation.getType().name() : "INSERT");
        payload.put("position", operation.getPosition() != null ? operation.getPosition() : 0);
        payload.put("text", operation.getText() != null ? operation.getText() : "");
        payload.put("length", operation.getLength() != null ? operation.getLength() : 0);
        payload.put("version", operation.getVersion());

        KafkaDocumentEvent event = KafkaDocumentEvent.builder()
                .eventType("EDIT")
                .documentId(documentId)
                .userId(username)
                .timestamp(System.currentTimeMillis())
                .payload(payload)
                .build();

        kafkaProducerService.sendEditEvent(event);
        log.info("Edit op received: type={} text={} pos={} len={} ver={} doc={} user={}",
                operation.getType(),
                operation.getText() != null ? "\"" + operation.getText() + "\"" : "\"\"",
                operation.getPosition(), operation.getLength(), operation.getVersion(),
                documentId, username);
    }
}
