package com.syncdocs.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DocumentUpdateRequest {

    @Size(max = 255)
    private String title;

    private String content;
}
