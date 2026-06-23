package com.campus.platform.attendance.service;

import com.campus.platform.attendance.dto.AttendanceDto;
import com.campus.platform.attendance.dto.AttendanceResponseDto;
import com.campus.platform.attendance.entity.Attendance;
import com.campus.platform.attendance.mapper.AttendanceMapper;
import com.campus.platform.attendance.repository.AttendanceRepository;
import com.campus.platform.event.service.EventService;
import com.campus.platform.registration.entity.Registration;
import com.campus.platform.registration.repository.RegistrationRepository;
import com.campus.platform.registration.service.RegistrationService;
import com.campus.platform.user.entity.User;
import com.campus.platform.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final AttendanceMapper attendanceMapper;
    private final RegistrationService registrationService;
    private final UserService userService;
    private final RegistrationRepository registrationRepository;
    private final EventService eventService; // ← add for tenant checks
    private final Clock clock;


    /**
     * Validates QR code and marks attendance.
     * Guards: cross-tenant QR, wrong event, cancelled registration, duplicate scan.
     */
    @Transactional
    public AttendanceResponseDto scanQrCode(AttendanceDto dto, UUID collegeId, UUID scannedById) {
        Registration registration = registrationService.findByQrCodeOrThrow(dto.getQrCode());

        // Cross-tenant guard
        UUID registrationCollegeId = registration.getEvent().getCollege().getCollegeId();
        if (!registrationCollegeId.equals(collegeId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "QR code does not belong to this college tenant.");
        }

        // Event mismatch guard
        if (!registration.getEvent().getEventId().equals(dto.getEventId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "QR code is valid but belongs to a different event.");
        }

        // Cancelled registration guard
        if (registration.isCancelled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This registration has been cancelled.");
        }

        // Duplicate scan guard
        if (attendanceRepository.existsByRegistration_RegistrationId(registration.getRegistrationId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Attendance already marked for this registration.");
        }

        // Event must be PUBLISHED — can't mark attendance for pending/rejected events
        if (registration.getEvent().getStatus().name().equals("PENDING_APPROVAL")
                || registration.getEvent().getStatus().name().equals("REJECTED")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot mark attendance for an event that is not published.");
        }

        User scanner = userService.findUserOrThrow(scannedById);

        Attendance attendance = Attendance.builder()
                .registration(registration)
                .scannedBy(scanner)
                .scannedAt(LocalDateTime.now(clock))  // ← was .now()
                .build();

        return attendanceMapper.toResponseDto(attendanceRepository.save(attendance));
    }

    /**
     * Manual attendance marking by registration ID — fallback when QR scanner fails.
     */
    @Transactional
    public AttendanceResponseDto markManually(String email, UUID eventId,
                                              UUID scannedById, UUID collegeId) {
        Registration registration = registrationRepository
                .findByUser_EmailAndEvent_EventIdAndIsCancelledFalse(email, eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No active registration found for email: " + email));

        // Cross-tenant guard
        UUID registrationCollegeId = registration.getEvent().getCollege().getCollegeId();
        if (!registrationCollegeId.equals(collegeId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Registration does not belong to this college.");
        }

        // Cancelled guard is now handled by the query (isCancelledFalse)
        // but keep it as a safety net
        if (registration.isCancelled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This registration has been cancelled.");
        }

        // Duplicate guard
        if (attendanceRepository.existsByRegistration_RegistrationId(registration.getRegistrationId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Attendance already marked for this registration.");
        }

        User scanner = userService.findUserOrThrow(scannedById);

        Attendance attendance = Attendance.builder()
                .registration(registration)
                .scannedBy(scanner)
                .scannedAt(LocalDateTime.now(clock))  // ← was .now()
                .build();

        return attendanceMapper.toResponseDto(attendanceRepository.save(attendance));
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponseDto> getAttendanceForEvent(UUID eventId, UUID collegeId) {
        // tenant check — club admin can only see attendance for events in their college
        eventService.findEventInTenantOrThrow(eventId, collegeId); // ← added
        return attendanceRepository.findAllByEventId(eventId)
                .stream()
                .map(attendanceMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getAttendanceCount(UUID eventId, UUID collegeId) {
        // tenant check — same guard
        eventService.findEventInTenantOrThrow(eventId, collegeId); // ← added
        return attendanceRepository.countByEventId(eventId);
    }
}