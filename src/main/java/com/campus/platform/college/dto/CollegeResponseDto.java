package com.campus.platform.college.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CollegeResponseDto {

    private UUID collegeId;
    private String name;
    private String logoUrl;
    private boolean isActive;
    private List<String> domains;
    private String primaryDomain;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}