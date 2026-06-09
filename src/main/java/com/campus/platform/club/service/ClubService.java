package com.campus.platform.club.service;

import com.campus.platform.club.dto.ClubDto;
import com.campus.platform.club.dto.ClubResponseDto;
import com.campus.platform.club.entity.Club;
import com.campus.platform.club.mapper.ClubMapper;
import com.campus.platform.club.repository.ClubRepository;
import com.campus.platform.college.entity.College;
import com.campus.platform.college.service.CollegeService;
import com.campus.platform.common.enums.UserRole;
import com.campus.platform.user.entity.User;
import com.campus.platform.user.repository.UserRepository;
import com.campus.platform.user.service.UserService;
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
public class ClubService {

    private final ClubRepository clubRepository;
    private final ClubMapper clubMapper;
    private final CollegeService collegeService;
    private final UserService userService;
    private final UserRepository userRepository;

    @Transactional
    public ClubResponseDto createClub(UUID collegeId, ClubDto dto) {
        College college = collegeService.findCollegeOrThrow(collegeId);

        if (clubRepository.existsByCollege_CollegeIdAndName(collegeId, dto.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Club name '" + dto.getName() + "' already exists in this college.");
        }

        Club club = clubMapper.toEntity(dto, college);

        if (dto.getManagedByEmail() != null && !dto.getManagedByEmail().isBlank()) {
            User user = resolveAndPromoteClubAdmin(dto.getManagedByEmail(), collegeId);
            club.setManagedBy(user);
        }

        return clubMapper.toResponseDto(clubRepository.save(club));
    }

    @Transactional
    public ClubResponseDto assignClubAdmin(UUID clubId, String email, UUID collegeId) {
        Club club = findClubInTenantOrThrow(clubId, collegeId);

        // demote previous manager if they no longer manage any club after this
        User previous = club.getManagedBy();

        User newAdmin = resolveAndPromoteClubAdmin(email, collegeId);

        // don't demote if same user is being reassigned
        if (previous != null && !previous.getUserId().equals(newAdmin.getUserId())) {
            demoteIfUnassigned(previous);
        }

        club.setManagedBy(newAdmin);
        return clubMapper.toResponseDto(clubRepository.save(club));
    }

    @Transactional
    public void deleteClub(UUID clubId, UUID collegeId) {
        Club club = findClubInTenantOrThrow(clubId, collegeId);
        User previous = club.getManagedBy();
        clubRepository.delete(club);
        if (previous != null) {
            demoteIfUnassigned(previous);
        }
    }

    @Transactional(readOnly = true)
    public List<ClubResponseDto> getClubsByCollege(UUID collegeId) {
        return clubRepository.findAllByCollege_CollegeId(collegeId)
                .stream()
                .map(clubMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ClubResponseDto getClubById(UUID clubId, UUID collegeId) {
        return clubMapper.toResponseDto(findClubInTenantOrThrow(clubId, collegeId));
    }

    @Transactional
    public ClubResponseDto updateClub(UUID clubId, UUID collegeId, ClubDto dto) {
        Club club = findClubInTenantOrThrow(clubId, collegeId);
        boolean nameChanged = !club.getName().equals(dto.getName());
        if (nameChanged && clubRepository.existsByCollege_CollegeIdAndName(collegeId, dto.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Club name '" + dto.getName() + "' already exists in this college.");
        }
        clubMapper.updateEntityFromDto(dto, club);
        return clubMapper.toResponseDto(clubRepository.save(club));
    }

    @Transactional(readOnly = true)
    public Club findClubByManagerOrThrow(UUID userId) {
        return clubRepository.findByManagedBy_UserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No club found managed by this user."));
    }

    // ── Internal helpers ────────────────────────────────────────────────────────

    /**
     * Resolves a user by email, validates they belong to the college,
     * and auto-promotes them to CLUB_ADMIN if they are currently a STUDENT.
     */
    private User resolveAndPromoteClubAdmin(String email, UUID collegeId) {
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found with email: " + email));

        if (!user.belongsToCollege(collegeId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "User does not belong to your college.");
        }

        if (user.getRole() == UserRole.COLLEGE_ADMIN || user.getRole() == UserRole.PLATFORM_OWNER) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "User has a higher role and cannot be assigned as club admin.");
        }

        if (clubRepository.existsByManagedBy_UserId(user.getUserId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "User is already managing another club.");
        }

        // auto-promote STUDENT → CLUB_ADMIN
        if (user.getRole() == UserRole.STUDENT) {
            user.setRole(UserRole.CLUB_ADMIN);
            userRepository.save(user);
        }

        return user;
    }

    /**
     * Demotes a CLUB_ADMIN back to STUDENT if they are no longer managing any club.
     */
    private void demoteIfUnassigned(User user) {
        if (user.getRole() == UserRole.CLUB_ADMIN
                && !clubRepository.existsByManagedBy_UserId(user.getUserId())) {
            user.setRole(UserRole.STUDENT);
            userRepository.save(user);
        }
    }

    public Club findClubInTenantOrThrow(UUID clubId, UUID collegeId) {
        return clubRepository.findByClubIdAndCollege_CollegeId(clubId, collegeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Club not found or does not belong to this college."));
    }
}