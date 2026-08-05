package com.syncdocs.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LocalShareRequest {

    @NotBlank
    @Size(max = 255)
    private String localDocId;

    @NotBlank
    @Size(max = 255)
    private String title;

    @NotBlank
    @Size(max = 100)
    private String sharedWith;

    private String filePath;
}
