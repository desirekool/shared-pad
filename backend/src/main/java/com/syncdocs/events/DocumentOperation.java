package com.syncdocs.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentOperation {

    private String documentId;
    private String userId;
    private OperationType type;
    private Integer position;
    private String text;
    private Integer length;
    private long version;
    private Long timestamp;

    public enum OperationType {
        INSERT,
        DELETE,
        REPLACE
    }
}
