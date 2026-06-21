package com.syncdocs.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPresence {

    private String documentId;
    private String userId;
    private String username;
    private String status;
    private CursorPosition cursor;
    private SelectionRange selection;
    private boolean typing;
    private Instant lastActivity;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CursorPosition {
        private int line;
        private int column;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SelectionRange {
        private int startLine;
        private int startColumn;
        private int endLine;
        private int endColumn;
    }
}
