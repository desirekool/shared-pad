package com.syncdocs.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "edit_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EditHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "operation_type", nullable = false, length = 20)
    private String operationType;

    @Column(nullable = false)
    private Integer position;

    @Column(columnDefinition = "TEXT")
    private String text;

    private Integer length;

    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
