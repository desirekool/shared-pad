package com.syncdocs.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
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

    @Min(0)
    private long fileSize;

    private String originalLastModified;
}
