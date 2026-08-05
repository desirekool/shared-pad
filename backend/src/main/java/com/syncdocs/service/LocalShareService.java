package com.syncdocs.service;

import jakarta.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class LocalShareService {

    @Data
    @AllArgsConstructor
    public static class LocalShare {
        private String localDocId;
        private String title;
        private String owner;
        private String sharedWith;
        private String filePath;
        private Instant createdAt;
    }

    private final Map<String, List<LocalShare>> shares = new ConcurrentHashMap<>();

    public void share(String localDocId, String title, String owner, String sharedWith, String filePath) {
        shares.computeIfAbsent(sharedWith, k -> new ArrayList<>())
                .add(new LocalShare(localDocId, title, owner, sharedWith, filePath, Instant.now()));
        log.info("Local doc {} shared by {} with {}", localDocId, owner, sharedWith);
    }

    public List<LocalShare> getSharedWith(String username) {
        return shares.getOrDefault(username, List.of());
    }

    public void removeShare(String localDocId) {
        shares.values().forEach(list -> list.removeIf(s -> s.getLocalDocId().equals(localDocId)));
    }

    @Scheduled(fixedRate = 3600000)
    public void cleanExpired() {
        Instant cutoff = Instant.now().minusSeconds(86400);
        shares.values().forEach(list -> list.removeIf(s -> s.getCreatedAt().isBefore(cutoff)));
        shares.entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    @PreDestroy
    public void cleanup() {
        shares.clear();
    }
}
