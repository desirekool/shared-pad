package com.syncdocs.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class DocumentUpdateRequest {

    @Size(max = 255)
    private String title;

    @Length(max = 10_000_000)
    private String content;
}
