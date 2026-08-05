package com.syncdocs.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "stored_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoredEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "document_id")
    private Long documentId;

    @Column(name = "user_id", length = 50)
    private String userId;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
