package com.campus.platform.college.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CollegeDomainResponseDto {

    private UUID id;
    private String domain;
    private boolean primary;
    private LocalDateTime createdAt;
}