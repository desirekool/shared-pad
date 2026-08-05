package com.syncdocs.service;

import com.syncdocs.service.LocalShareService.LocalShare;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LocalShareServiceTest {

    private LocalShareService service;

    @BeforeEach
    void setUp() {
        service = new LocalShareService();
    }

    @Test
    void share_ShouldCreateShare() {
        service.share("doc1", "Test", "alice", "bob", "/path/doc1.txt");

        List<LocalShare> bobShares = service.getSharedWith("bob");
        assertEquals(1, bobShares.size());
        assertEquals("doc1", bobShares.get(0).getLocalDocId());
        assertEquals("alice", bobShares.get(0).getOwner());
        assertEquals("Test", bobShares.get(0).getTitle());
    }

    @Test
    void getSharedWith_ShouldReturnEmptyForUnknownUser() {
        List<LocalShare> shares = service.getSharedWith("nobody");
        assertTrue(shares.isEmpty());
    }

    @Test
    void removeShare_ShouldRemoveSpecificShare() {
        service.share("doc1", "A", "alice", "bob", null);
        service.share("doc2", "B", "alice", "bob", null);

        service.removeShare("doc1");

        List<LocalShare> bobShares = service.getSharedWith("bob");
        assertEquals(1, bobShares.size());
        assertEquals("doc2", bobShares.get(0).getLocalDocId());
    }

    @Test
    void cleanup_ShouldClearAllShares() {
        service.share("doc1", "A", "alice", "bob", null);
        service.cleanup();

        assertTrue(service.getSharedWith("bob").isEmpty());
    }

    @Test
    void cleanExpired_ShouldRemoveSharesOlderThan24h() {
        service.share("doc1", "Old", "alice", "bob", null);

        // Manually set the created time to be > 24h old via reflection
        try {
            java.lang.reflect.Field sharesField = LocalShareService.class.getDeclaredField("shares");
            sharesField.setAccessible(true);
            java.util.Map<String, java.util.List<LocalShare>> shares =
                    (java.util.Map<String, java.util.List<LocalShare>>) sharesField.get(service);
            java.lang.reflect.Field createdAtField = LocalShare.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            LocalShare share = shares.get("bob").get(0);
            createdAtField.set(share, java.time.Instant.now().minusSeconds(90000));

            service.cleanExpired();
            assertTrue(service.getSharedWith("bob").isEmpty());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
