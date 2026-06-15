package com.campus.platform.user.service;

import com.campus.platform.college.entity.College;
import com.campus.platform.college.repository.CollegeRepository;
import com.campus.platform.college.service.CollegeService;
import com.campus.platform.common.enums.UserRole;
import com.campus.platform.user.dto.*;
import com.campus.platform.user.entity.User;
import com.campus.platform.user.mapper.UserMapper;
import com.campus.platform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final CollegeService collegeService;
    private final CollegeRepository collegeRepository;

    @Transactional
    public User provisionUser(String googleSub, String email,
                              String fullName, String pictureUrl,
                              College college) {
        log.info("provisionUser called — email: {}, college: {}",
                email, college != null ? college.getCollegeId() : "NULL");

        // Already logged in before — update college if missing
        Optional<User> byGoogleSub = userRepository.findByGoogleSub(googleSub);
        if (byGoogleSub.isPresent()) {
            User existing = byGoogleSub.get();
            if (existing.getCollege() == null && college != null) {
                existing.setCollege(college);
                return userRepository.save(existing);
            }
            return existing;
        }

        // Email was pre-assigned by platform admin (stub exists, no googleSub yet)
        Optional<User> byEmail = userRepository.findByEmail(email.toLowerCase());
        if (byEmail.isPresent()) {
            User existing = byEmail.get();
            existing.setGoogleSub(googleSub);
            existing.setFullName(fullName);
            existing.setPictureUrl(pictureUrl);
            if (existing.getCollege() == null && college != null) {
                existing.setCollege(college);
            }
            return userRepository.save(existing);
        }

        // Completely new user — default STUDENT
        return userRepository.save(User.builder()
                .googleSub(googleSub)
                .email(email.toLowerCase())
                .fullName(fullName)
                .pictureUrl(pictureUrl)
                .role(UserRole.STUDENT)
                .college(college)
                .build());
    }

    @Transactional(readOnly = true)
    public long getUserCountByCollege(UUID collegeId) {
        return userRepository.countByCollege_CollegeId(collegeId);
    }

    @Transactional
    public User provisionPlatformOwner(String googleSub, String email,
                                       String fullName, String pictureUrl) {
        return userRepository.findByEmail(email.toLowerCase()).map(existing -> {
            existing.setGoogleSub(googleSub);
            existing.setRole(UserRole.PLATFORM_OWNER);
            existing.setCollege(null);
            existing.setFullName(fullName);
            existing.setPictureUrl(pictureUrl);
            existing.setProfileComplete(true);
            return userRepository.save(existing);
        }).orElseGet(() -> userRepository.save(User.builder()
                .googleSub(googleSub)
                .email(email.toLowerCase())
                .fullName(fullName)
                .pictureUrl(pictureUrl)
                .role(UserRole.PLATFORM_OWNER)
                .college(null)
                .profileComplete(true)
                .build()));
    }

    @Transactional(readOnly = true)
    public List<UserResponseDto> getCollegeAdmins(UUID collegeId) {
        return userRepository
                .findAllByCollege_CollegeIdAndRole(collegeId, UserRole.COLLEGE_ADMIN)
                .stream()
                .map(userMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponseDto promoteToCollegeAdmin(UUID userId) {
        User user = findUserOrThrow(userId);
        if (user.getCollege() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "User has no college — cannot promote to COLLEGE_ADMIN.");
        }
        user.setRole(UserRole.COLLEGE_ADMIN);
        return userMapper.toResponseDto(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponseDto getUserById(UUID userId) {
        return userMapper.toResponseDto(findUserOrThrow(userId));
    }

    @Transactional
    public UserResponseDto completeStudentProfile(UUID userId, CompleteStudentProfileDto dto) {
        User user = findUserOrThrow(userId);
        if (user.getRole() != UserRole.STUDENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only students complete this profile step.");
        }
        if (user.isProfileComplete()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Profile already completed.");
        }
        user.setYear(dto.getYear());
        user.setStream(dto.getStream());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setRegistrationNumber(dto.getRegistrationNumber());
        user.setProfileComplete(true);
        return userMapper.toResponseDto(userRepository.save(user));
    }

    @Transactional
    public UserResponseDto assignCollegeAdmin(AssignCollegeAdminDto dto) {
        College college = collegeRepository
                .findByNameIgnoreCase(dto.getCollegeName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No college found with name '" + dto.getCollegeName() + "'"));

        // Check if college already has an admin
        List<User> existingAdmins = userRepository
                .findAllByCollege_CollegeIdAndRole(college.getCollegeId(), UserRole.COLLEGE_ADMIN);
        if (!existingAdmins.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "College already has an admin. Remove the existing admin first.");
        }

        String email = dto.getEmail().toLowerCase();

        User user = userRepository.findByEmail(email)
                .map(existing -> {
                    existing.setRole(UserRole.COLLEGE_ADMIN);
                    existing.setCollege(college);
                    existing.setProfileComplete(true);
                    return existing;
                })
                .orElseGet(() -> User.builder()
                        .email(email)
                        .fullName("")
                        .role(UserRole.COLLEGE_ADMIN)
                        .college(college)
                        .profileComplete(true)
                        .build());

        return userMapper.toResponseDto(userRepository.save(user));
    }

    @Transactional
    public void removeCollegeAdmin(UUID collegeId, String email) {
        User admin = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No user found with email: " + email));

        if (admin.getRole() != UserRole.COLLEGE_ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "User is not a college admin.");
        }

        if (!admin.belongsToCollege(collegeId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "User is not an admin of this college.");
        }

        // Demote to STUDENT instead of deleting
        admin.setRole(UserRole.STUDENT);
        userRepository.save(admin);
    }

    @Transactional(readOnly = true)
    public long getTotalUserCount() {
        return userRepository.count();
    }

    @Transactional(readOnly = true)
    public long getUserCountByCollegeName(String collegeName) {
        College college = collegeRepository.findByNameIgnoreCase(collegeName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No college found with name: " + collegeName));
        return userRepository.countByCollege_CollegeId(college.getCollegeId());
    }

    @Transactional
    public UserResponseDto completeGuestProfile(UUID userId, CompleteGuestProfileDto dto) {
        User user = findUserOrThrow(userId);

        if (user.getRole() != UserRole.STUDENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only students can complete this step.");
        }

        if (user.getCollege() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "You are a college student — use the full profile completion endpoint.");
        }

        if (user.isProfileComplete()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Profile already completed.");
        }

        user.setPhoneNumber(dto.getPhoneNumber());
        user.setProfileComplete(true);
        return userMapper.toResponseDto(userRepository.save(user));
    }
    @Transactional(readOnly = true)
    public List<UserResponseDto> getAdminsByCollege(UUID collegeId) {
        return userRepository
                .findAllByCollege_CollegeIdAndRoleIn(
                        collegeId,
                        List.of(UserRole.COLLEGE_ADMIN, UserRole.CLUB_ADMIN))
                .stream()
                .map(userMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase());
    }

    // ── Internal helpers ────────────────────────────────────────────────────────

    public User findUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found with id: " + userId));
    }

    public User findByGoogleSubOrThrow(String googleSub) {
        return userRepository.findByGoogleSub(googleSub)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found for google_sub: " + googleSub));
    }
}