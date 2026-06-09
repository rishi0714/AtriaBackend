package com.campus.platform.college.mapper;

import com.campus.platform.college.dto.CollegeDomainResponseDto;
import com.campus.platform.college.dto.CollegeDto;
import com.campus.platform.college.dto.CollegeResponseDto;
import com.campus.platform.college.entity.College;
import com.campus.platform.college.entity.CollegeDomain;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CollegeMapper {

    public College toEntity(CollegeDto dto) {
        return College.builder()
                .name(dto.getName())
                .logoUrl(dto.getLogoUrl())
                .isActive(true)
                .build();
        // domain is NOT set here — saved separately as CollegeDomain in CollegeService.createCollege()
    }

    public CollegeResponseDto toResponseDto(College college) {

        List<String> domains = college.getDomains().stream()
                .map(CollegeDomain::getDomain)
                .collect(Collectors.toList());

        String primaryDomain = college.getDomains().stream()
                .filter(CollegeDomain::isPrimary)
                .map(CollegeDomain::getDomain)
                .findFirst()
                .orElse(domains.isEmpty() ? null : domains.get(0));

        return CollegeResponseDto.builder()
                .collegeId(college.getCollegeId())
                .name(college.getName())
                .logoUrl(college.getLogoUrl())
                .isActive(college.isActive())
                .domains(domains)
                .primaryDomain(primaryDomain)
                .createdAt(college.getCreatedAt())
                .updatedAt(college.getUpdatedAt())
                .build();
    }

    public void updateEntityFromDto(CollegeDto dto, College college) {
        college.setName(dto.getName());
        college.setLogoUrl(dto.getLogoUrl());
        // domain is managed via college_domains — not updated here
    }

    public CollegeDomainResponseDto toDomainResponseDto(CollegeDomain cd) {
        return CollegeDomainResponseDto.builder()
                .id(cd.getId())
                .domain(cd.getDomain())
                .primary(cd.isPrimary())
                .createdAt(cd.getCreatedAt())
                .build();
    }
}