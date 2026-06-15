package com.campus.platform.security.auth.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseDto {

    private String token;
    private String userId;
    private String email;
    private String role;
    private String collegeId;
}