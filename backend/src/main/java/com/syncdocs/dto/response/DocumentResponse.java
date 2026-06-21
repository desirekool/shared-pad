package com.syncdocs.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
public class DocumentResponse {

    private Long id;
    private String title;
    private String owner;
    private String mimeType;
    private Long size;
    private Long version;
    private Instant createdAt;
    private Instant updatedAt;
    private String content;
    private String originalFilename;
}
