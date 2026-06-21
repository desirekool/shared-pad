package com.syncdocs.service;

import com.syncdocs.events.UserPresence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PresenceServiceTest {

    private PresenceService presenceService;

    @BeforeEach
    void setUp() {
        presenceService = new PresenceService();
    }

    @Test
    void userJoined_ShouldAddUserToActiveUsers() {
        presenceService.userJoined("doc-1", "alice", "Alice");

        List<UserPresence> users = presenceService.getActiveUsers("doc-1");
        assertEquals(1, users.size());
        assertEquals("Alice", users.get(0).getUsername());
    }

    @Test
    void userLeft_ShouldRemoveUser() {
        presenceService.userJoined("doc-1", "alice", "Alice");
        presenceService.userLeft("doc-1", "alice");

        List<UserPresence> users = presenceService.getActiveUsers("doc-1");
        assertTrue(users.isEmpty());
    }

    @Test
    void getActiveCount_ShouldReturnCorrectNumber() {
        presenceService.userJoined("doc-1", "alice", "Alice");
        presenceService.userJoined("doc-1", "bob", "Bob");

        assertEquals(2, presenceService.getActiveCount("doc-1"));
    }

    @Test
    void updateCursor_ShouldUpdateUserCursor() {
        presenceService.userJoined("doc-1", "alice", "Alice");
        UserPresence.CursorPosition cursor = UserPresence.CursorPosition.builder()
                .line(10).column(5).build();
        presenceService.updateCursor("doc-1", "alice", cursor);

        List<UserPresence> users = presenceService.getActiveUsers("doc-1");
        assertNotNull(users.get(0).getCursor());
        assertEquals(10, users.get(0).getCursor().getLine());
    }

    @Test
    void setTyping_ShouldUpdateTypingStatus() {
        presenceService.userJoined("doc-1", "alice", "Alice");
        presenceService.setTyping("doc-1", "alice", true);

        List<UserPresence> users = presenceService.getActiveUsers("doc-1");
        assertTrue(users.get(0).isTyping());
    }
}
