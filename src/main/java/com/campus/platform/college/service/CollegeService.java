package com.campus.platform.college.service;

import com.campus.platform.college.dto.CollegeDomainDto;
import com.campus.platform.college.dto.CollegeDomainResponseDto;
import com.campus.platform.college.dto.CollegeDto;
import com.campus.platform.college.dto.CollegeResponseDto;
import com.campus.platform.college.entity.College;
import com.campus.platform.college.entity.CollegeDomain;
import com.campus.platform.college.mapper.CollegeMapper;
import com.campus.platform.college.repository.CollegeDomainRepository;
import com.campus.platform.college.repository.CollegeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CollegeService {

    private final CollegeRepository collegeRepository;
    private final CollegeMapper collegeMapper;
    private final CollegeDomainRepository collegeDomainRepository;

    @Transactional
    public CollegeResponseDto createCollege(CollegeDto dto) {
        College college = collegeMapper.toEntity(dto);
        College saved = collegeRepository.save(college);

        if (dto.getDomain() != null && !dto.getDomain().isBlank()) {
            if (collegeDomainRepository.existsByDomain(dto.getDomain().toLowerCase())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Domain '" + dto.getDomain() + "' is already assigned to another college.");
            }
            CollegeDomain cd = CollegeDomain.builder()
                    .college(saved)
                    .domain(dto.getDomain().toLowerCase())
                    .primary(true)
                    .build();
            collegeDomainRepository.save(cd);
        }

        return collegeMapper.toResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public List<CollegeResponseDto> getAllColleges() {
        return collegeRepository.findAll()
                .stream()
                .map(collegeMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CollegeResponseDto getCollegeById(UUID collegeId) {
        return collegeMapper.toResponseDto(findCollegeOrThrow(collegeId));
    }

    @Transactional
    public CollegeResponseDto updateCollege(UUID collegeId, CollegeDto dto) {
        College college = findCollegeOrThrow(collegeId);
        collegeMapper.updateEntityFromDto(dto, college);
        return collegeMapper.toResponseDto(collegeRepository.save(college));
    }

    @Transactional
    public CollegeResponseDto setActiveStatus(UUID collegeId, boolean isActive) {
        College college = findCollegeOrThrow(collegeId);
        college.setActive(isActive);
        return collegeMapper.toResponseDto(collegeRepository.save(college));
    }

    // ── Domain management ───────────────────────────────────────────────────────

    @Transactional
    public CollegeDomainResponseDto addDomain(UUID collegeId, CollegeDomainDto dto) {
        College college = findCollegeOrThrow(collegeId);

        if (collegeDomainRepository.existsByDomain(dto.getDomain().toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Domain '" + dto.getDomain() + "' is already assigned to another college.");
        }

        if (dto.isPrimary()) {
            collegeDomainRepository
                    .findByCollege_CollegeIdAndPrimaryTrue(collegeId)
                    .ifPresent(existing -> {
                        existing.setPrimary(false);
                        collegeDomainRepository.save(existing);
                    });
        }

        CollegeDomain cd = CollegeDomain.builder()
                .college(college)
                .domain(dto.getDomain().toLowerCase())
                .primary(dto.isPrimary())
                .build();

        return collegeMapper.toDomainResponseDto(collegeDomainRepository.save(cd));
    }

    @Transactional
    public void removeDomain(UUID collegeId, UUID domainId) {
        CollegeDomain cd = collegeDomainRepository.findById(domainId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Domain not found."));

        if (!cd.getCollege().getCollegeId().equals(collegeId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Domain does not belong to this college.");
        }

        collegeDomainRepository.delete(cd);
    }

    @Transactional
    public CollegeDomainResponseDto setPrimaryDomain(UUID collegeId, UUID domainId) {
        CollegeDomain cd = collegeDomainRepository.findById(domainId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Domain not found."));

        if (!cd.getCollege().getCollegeId().equals(collegeId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Domain does not belong to this college.");
        }

        // Demote current primary
        collegeDomainRepository
                .findByCollege_CollegeIdAndPrimaryTrue(collegeId)
                .ifPresent(existing -> {
                    existing.setPrimary(false);
                    collegeDomainRepository.save(existing);
                });

        cd.setPrimary(true);
        return collegeMapper.toDomainResponseDto(collegeDomainRepository.save(cd));
    }

    @Transactional(readOnly = true)
    public List<CollegeDomainResponseDto> getDomainsForCollege(UUID collegeId) {
        findCollegeOrThrow(collegeId); // ensure college exists
        return collegeDomainRepository.findAllByCollege_CollegeId(collegeId)
                .stream()
                .map(collegeMapper::toDomainResponseDto)
                .collect(Collectors.toList());
    }

    // ── Internal helpers ────────────────────────────────────────────────────────

    public College findCollegeOrThrow(UUID collegeId) {
        return collegeRepository.findById(collegeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "College not found with id: " + collegeId));
    }

    public College resolveByDomain(String domain) {
        return collegeDomainRepository
                .findByDomainAndCollege_IsActiveTrueWithCollege(domain.toLowerCase())
                .map(CollegeDomain::getCollege)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "No active college found for domain: " + domain));
    }
}