package com.syncdocs.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PromoteRequest {

    @NotBlank
    private String content;

    @NotBlank
    private String originalFilename;

    private String originalPath;

    private String originalChecksum;

    private String mimeType;

    private long fileSize;

    private String originalLastModified;
}
