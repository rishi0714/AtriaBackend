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
import com.campus.platform.college.dto.CollegeSetupDto;
import com.campus.platform.college.dto.CollegeSetupResponseDto;
import com.campus.platform.common.enums.UserRole;
import com.campus.platform.user.entity.User;
import com.campus.platform.user.mapper.UserMapper;
import com.campus.platform.user.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional
    public CollegeSetupResponseDto setupCollege(CollegeSetupDto dto) {

        // ── 1. Validate college name uniqueness ──────────────────────────────────
        if (collegeRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "College with name '" + dto.getName() + "' already exists.");
        }

        // ── 2. Validate domain uniqueness ────────────────────────────────────────
        String domain = dto.getDomain().toLowerCase();
        if (collegeDomainRepository.existsByDomain(domain)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Domain '" + domain + "' is already assigned to another college.");
        }

        // ── 3. Validate ALL admin emails match domain — fail fast before any write
        List<String> invalidEmails = dto.getAdmins().stream()       // ← was getAdminEmails()
                .map(a -> a.getEmail().toLowerCase())               // ← extract email from Admin
                .filter(email -> {
                    String emailDomain = email.substring(email.indexOf('@') + 1);
                    return !emailDomain.equalsIgnoreCase(domain);
                })
                .collect(Collectors.toList());

        if (!invalidEmails.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "These emails don't match college domain '@" + domain + "': " + invalidEmails);
        }

        // ── 4. Create and save college ───────────────────────────────────────────
        College college = College.builder()
                .name(dto.getName())
                .logoUrl(dto.getLogoUrl())
                .build();
        college = collegeRepository.save(college);

        // ── 5. Create primary domain ─────────────────────────────────────────────
        CollegeDomain collegeDomain = CollegeDomain.builder()
                .college(college)
                .domain(domain)
                .primary(true)
                .build();
        collegeDomainRepository.save(collegeDomain);

        // ── 6. Create or upgrade all admin users ─────────────────────────────────
        final College savedCollege = college;

        List<User> admins = dto.getAdmins().stream()                // ← was getAdminEmails()
                .map(adminDto -> {
                    String email = adminDto.getEmail().toLowerCase();
                    return userRepository.findByEmail(email)
                            .map(existing -> {
                                if (existing.getRole() == UserRole.COLLEGE_ADMIN
                                        && existing.getCollege() != null
                                        && !existing.getCollege().getCollegeId()
                                        .equals(savedCollege.getCollegeId())) {
                                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                                            "User '" + email +
                                                    "' is already a college admin of another college.");
                                }
                                existing.setRole(UserRole.COLLEGE_ADMIN);
                                existing.setCollege(savedCollege);
                                existing.setFullName(adminDto.getFullName()); // ← set name
                                existing.setProfileComplete(true);
                                return existing;
                            })
                            .orElseGet(() -> User.builder()
                                    .email(email)
                                    .fullName(adminDto.getFullName())          // ← set name
                                    .role(UserRole.COLLEGE_ADMIN)
                                    .college(savedCollege)
                                    .profileComplete(true)
                                    .build());
                })
                .collect(Collectors.toList());

        List<User> savedAdmins = userRepository.saveAll(admins);

        // ── 7. Build response ────────────────────────────────────────────────────
        return CollegeSetupResponseDto.builder()
                .collegeId(savedCollege.getCollegeId())
                .name(savedCollege.getName())
                .logoUrl(savedCollege.getLogoUrl())
                .isActive(savedCollege.isActive())
                .domain(domain)
                .admins(savedAdmins.stream()
                        .map(userMapper::toResponseDto)
                        .collect(Collectors.toList()))
                .createdAt(savedCollege.getCreatedAt())
                .build();
    }

    @Transactional
    public CollegeResponseDto createCollege(CollegeDto dto) {
        if (collegeRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "College with name '" + dto.getName() + "' already exists.");
        }
        College college = collegeMapper.toEntity(dto);
        return collegeMapper.toResponseDto(collegeRepository.save(college));
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