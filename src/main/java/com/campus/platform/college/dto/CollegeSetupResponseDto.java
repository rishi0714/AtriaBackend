package com.campus.platform.college.dto;

import com.campus.platform.user.dto.UserResponseDto;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CollegeSetupResponseDto {

    private UUID collegeId;
    private String name;
    private String logoUrl;
    private boolean isActive;
    private String domain;
    private List<UserResponseDto> admins;
    private LocalDateTime createdAt;
}