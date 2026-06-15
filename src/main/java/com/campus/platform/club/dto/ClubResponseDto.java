package com.campus.platform.club.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClubResponseDto {

    private UUID clubId;
    private UUID collegeId;
    private String collegeName;
    private String name;
    private String clubCategory;
    private String managedByEmail;  // ← was managedByUserId + managedByName
    private String description;
    private String logoUrl;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}