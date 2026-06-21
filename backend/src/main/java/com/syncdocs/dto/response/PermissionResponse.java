package com.syncdocs.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class PermissionResponse {

    private Long id;
    private Long documentId;
    private String username;
    private String permissionLevel;
}
