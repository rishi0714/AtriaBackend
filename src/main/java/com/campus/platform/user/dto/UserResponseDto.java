package com.campus.platform.user.dto;

import com.campus.platform.common.enums.UserRole;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDto {

    private UUID userId;
    private UUID collegeId;
    private String collegeName;
    private String email;
    private String fullName;
    private String pictureUrl;
    private UserRole role;
    private Short   year;
    private String phoneNumber;
    private String registrationNumber;
    private String  stream;
    private boolean profileComplete;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
