package com.campus.platform.user.controller;

import com.campus.platform.attendance.repository.AttendanceRepository;
import com.campus.platform.club.service.ClubService;
import com.campus.platform.common.util.SecurityContextUtil;
import com.campus.platform.registration.dto.RegistrationResponseDto;
import com.campus.platform.registration.service.RegistrationService;
import com.campus.platform.user.dto.StudentRegistrationDto;
import com.campus.platform.user.dto.UserResponseDto;
import com.campus.platform.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.campus.platform.user.dto.CompleteStudentProfileDto;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentController {

    private final UserService userService;
    private final ClubService clubService;
    private final RegistrationService registrationService;
    private final AttendanceRepository attendanceRepository;
    private final Clock clock;  // ← inject clock

    @GetMapping("/profile")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<UserResponseDto> getMyProfile() {
        return ResponseEntity.ok(
                userService.getUserById(SecurityContextUtil.currentUserId()));
    }

    @GetMapping("/registrations")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<StudentRegistrationDto>> getMyRegistrations() {
        UUID userId = SecurityContextUtil.currentUserId();
        List<RegistrationResponseDto> raw = registrationService.getMyRegistrations(userId);

        LocalDateTime now = LocalDateTime.now(clock);  // ← fixed, extracted once

        List<StudentRegistrationDto> result = raw.stream().map(r -> {
            boolean isPast = r.getEventDate() != null &&
                    r.getEventDate().isBefore(now);

            return StudentRegistrationDto.builder()
                    .id(r.getRegistrationId())
                    .status(isPast ? "PAST" : "Active")
                    .event(StudentRegistrationDto.EventInfo.builder()
                            .title(r.getEventTitle())
                            .club(r.getClubName())
                            .date(r.getEventDate() != null ?
                                    r.getEventDate().toLocalDate().toString() : "")
                            .time(r.getEventDate() != null ?
                                    r.getEventDate().toLocalTime().toString() : "")
                            .venue(r.getEventVenue())
                            .build())
                    .build();
        }).toList();

        return ResponseEntity.ok(result);
    }

    @PostMapping("/profile/complete")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<UserResponseDto> completeProfile(
            @Valid @RequestBody CompleteStudentProfileDto dto) {
        return ResponseEntity.ok(
                userService.completeStudentProfile(
                        SecurityContextUtil.currentUserId(), dto));
    }
}