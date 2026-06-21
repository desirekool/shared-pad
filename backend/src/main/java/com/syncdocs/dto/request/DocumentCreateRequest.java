package com.syncdocs.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DocumentCreateRequest {

    @NotBlank
    @Size(max = 255)
    private String title;

    private String mimeType;

    private String content;
}
