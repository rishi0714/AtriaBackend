package com.campus.platform.club.mapper;

import com.campus.platform.club.dto.ClubDto;
import com.campus.platform.club.dto.ClubResponseDto;
import com.campus.platform.club.entity.Club;
import com.campus.platform.college.entity.College;
import org.springframework.stereotype.Component;

@Component
public class ClubMapper {

    public Club toEntity(ClubDto dto, College college) {
        return Club.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .logoUrl(dto.getLogoUrl())
                .clubCategory(dto.getClubCategory())           // ← add this
                .college(college)
                .build();
    }

    public ClubResponseDto toResponseDto(Club club) {
        return ClubResponseDto.builder()
                .clubId(club.getClubId())
                .collegeId(club.getCollege().getCollegeId())
                .collegeName(club.getCollege().getName())
                .name(club.getName())
                .clubCategory(club.getClubCategory())
                .managedByEmail(club.getManagedBy() != null ?
                        club.getManagedBy().getEmail() : null)  // ← was userId + fullName
                .description(club.getDescription())
                .logoUrl(club.getLogoUrl())
                .createdAt(club.getCreatedAt())
                .updatedAt(club.getUpdatedAt())
                .build();
    }

    public void updateEntityFromDto(ClubDto dto, Club club) {
        club.setName(dto.getName());
        club.setDescription(dto.getDescription());
        club.setLogoUrl(dto.getLogoUrl());
        club.setClubCategory(dto.getClubCategory());           // ← add this
    }
}
