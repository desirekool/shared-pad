package com.syncdocs.service;

import com.syncdocs.events.UserPresence;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class PresenceService {

    private final ConcurrentHashMap<String, Map<String, UserPresence>> documentUsers = new ConcurrentHashMap<>();

    public void userJoined(String documentId, String userId, String username) {
        UserPresence presence = UserPresence.builder()
                .documentId(documentId)
                .userId(userId)
                .username(username)
                .status("ONLINE")
                .lastActivity(Instant.now())
                .build();

        documentUsers
                .computeIfAbsent(documentId, k -> new ConcurrentHashMap<>())
                .put(userId, presence);

        log.debug("User {} joined document {} ({} active)", username, documentId, getActiveCount(documentId));
    }

    public void userLeft(String documentId, String userId) {
        Map<String, UserPresence> users = documentUsers.get(documentId);
        if (users != null) {
            users.remove(userId);
            if (users.isEmpty()) {
                documentUsers.remove(documentId);
            }
        }
        log.debug("User {} left document {}", userId, documentId);
    }

    public void updateCursor(String documentId, String userId, UserPresence.CursorPosition cursor) {
        UserPresence presence = getUser(documentId, userId);
        if (presence != null) {
            presence.setCursor(cursor);
            presence.setLastActivity(Instant.now());
        }
    }

    public void updateSelection(String documentId, String userId, UserPresence.SelectionRange selection) {
        UserPresence presence = getUser(documentId, userId);
        if (presence != null) {
            presence.setSelection(selection);
            presence.setLastActivity(Instant.now());
        }
    }

    public void setTyping(String documentId, String userId, boolean typing) {
        UserPresence presence = getUser(documentId, userId);
        if (presence != null) {
            presence.setTyping(typing);
            presence.setLastActivity(Instant.now());
        }
    }

    public List<UserPresence> getActiveUsers(String documentId) {
        Map<String, UserPresence> users = documentUsers.get(documentId);
        if (users == null) return List.of();

        Instant threshold = Instant.now().minusSeconds(30);
        return users.values().stream()
                .filter(u -> u.getLastActivity().isAfter(threshold))
                .toList();
    }

    public int getActiveCount(String documentId) {
        return getActiveUsers(documentId).size();
    }

    private UserPresence getUser(String documentId, String userId) {
        Map<String, UserPresence> users = documentUsers.get(documentId);
        return users != null ? users.get(userId) : null;
    }

    @PreDestroy
    public void cleanup() {
        documentUsers.clear();
    }
}
