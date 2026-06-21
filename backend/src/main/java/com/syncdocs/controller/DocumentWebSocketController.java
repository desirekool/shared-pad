package com.syncdocs.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@Slf4j
public class DocumentWebSocketController {

    @MessageMapping("/documents.join.{documentId}")
    public void joinDocument(@DestinationVariable String documentId,
                             @Payload Map<String, Object> payload,
                             SimpMessageHeaderAccessor headerAccessor) {
        String username = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "unknown";
        log.info("User {} joined document {}", username, documentId);
        headerAccessor.getSessionAttributes().put("documentId", documentId);
    }

    @MessageMapping("/documents.leave.{documentId}")
    public void leaveDocument(@DestinationVariable String documentId,
                              SimpMessageHeaderAccessor headerAccessor) {
        String username = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "unknown";
        log.info("User {} left document {}", username, documentId);
    }
}
