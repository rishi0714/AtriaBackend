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
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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

        // Already logged in before — return existing session
        Optional<User> byGoogleSub = userRepository.findByGoogleSub(googleSub);
        if (byGoogleSub.isPresent()) return byGoogleSub.get();

        // Email was pre-assigned by platform admin (stub exists, no googleSub yet)
        Optional<User> byEmail = userRepository.findByEmail(email.toLowerCase());
        if (byEmail.isPresent()) {
            User existing = byEmail.get();
            existing.setGoogleSub(googleSub);   // bind Google identity on first login
            existing.setFullName(fullName);     // fill real name from Google
            existing.setPictureUrl(pictureUrl);
            // role + college already set — do NOT override
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

    @Transactional(readOnly = true)
    public List<UserResponseDto> getUsersByCollege(UUID collegeId) {
        return userRepository.findAllByCollege_CollegeId(collegeId)
                .stream()
                .map(userMapper::toResponseDto)
                .collect(Collectors.toList());
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

        userRepository.delete(admin);
    }

    @Transactional
    public UserResponseDto assignCollegeAdmin(AssignCollegeAdminDto dto) {
        // domain removed from lookup — college identified by name only
        College college = collegeRepository
                .findByNameIgnoreCase(dto.getCollegeName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No college found with name '" + dto.getCollegeName() + "'"));

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