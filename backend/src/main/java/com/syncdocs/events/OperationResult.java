package com.syncdocs.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationResult {

    private String documentId;
    private String userId;
    private boolean accepted;
    private long currentVersion;
    private String reason;
    private DocumentOperation originalOperation;
}
