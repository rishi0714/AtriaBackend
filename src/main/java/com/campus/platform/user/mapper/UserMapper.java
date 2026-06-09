package com.campus.platform.user.mapper;

import com.campus.platform.college.entity.College;
import com.campus.platform.user.dto.UserDto;
import com.campus.platform.user.dto.UserResponseDto;
import com.campus.platform.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toEntity(UserDto dto, College college) {
        return User.builder()
                .googleSub(dto.getGoogleSub())
                .email(dto.getEmail().toLowerCase())
                .fullName(dto.getFullName())
                .pictureUrl(dto.getPictureUrl())
                .role(dto.getRole())
                .college(college)
                .build();
    }

    public UserResponseDto toResponseDto(User user) {
        return UserResponseDto.builder()
                .userId(user.getUserId())
                .collegeId(user.getCollege() != null ? user.getCollege().getCollegeId() : null)
                .collegeName(user.getCollege() != null ? user.getCollege().getName() : null)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .pictureUrl(user.getPictureUrl())
                .role(user.getRole())
                .year(user.getYear())
                .stream(user.getStream())
                .phoneNumber(user.getPhoneNumber())
                .registrationNumber(user.getRegistrationNumber())
                .profileComplete(user.isProfileComplete())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
