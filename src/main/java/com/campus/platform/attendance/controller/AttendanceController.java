package com.campus.platform.attendance.controller;

import com.campus.platform.attendance.dto.AttendanceDto;
import com.campus.platform.attendance.dto.AttendanceResponseDto;
import com.campus.platform.attendance.service.AttendanceService;
import com.campus.platform.common.util.SecurityContextUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/club/attendance")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLUB_ADMIN')")
public class AttendanceController {

    private final AttendanceService attendanceService;

    /** QR scan endpoint — primary attendance flow */
    @PostMapping("/scan")
    public ResponseEntity<AttendanceResponseDto> scanQrCode(
            @Valid @RequestBody AttendanceDto dto) {
        UUID scannedById = SecurityContextUtil.currentUserId();
        UUID collegeId = SecurityContextUtil.currentCollegeId(); // ← from JWT
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attendanceService.scanQrCode(dto, collegeId, scannedById));
    }

    /** Manual attendance — fallback when camera scan fails */
    @PostMapping("/manual")
    public ResponseEntity<AttendanceResponseDto> markManually(
            @RequestParam String email,
            @RequestParam UUID eventId) {
        UUID scannedById = SecurityContextUtil.currentUserId();
        UUID collegeId = SecurityContextUtil.currentCollegeId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attendanceService.markManually(email, eventId, scannedById, collegeId));
    }

    /** Get full attendance report for an event */
    @GetMapping("/events/{eventId}")
    public ResponseEntity<List<AttendanceResponseDto>> getAttendanceForEvent(
            @PathVariable UUID eventId) {
        UUID collegeId = SecurityContextUtil.currentCollegeId(); // ← tenant check
        return ResponseEntity.ok(attendanceService.getAttendanceForEvent(eventId, collegeId));
    }

    /** Real-time count — polled by the scanning UI */
    @GetMapping("/events/{eventId}/count")
    public ResponseEntity<Long> getAttendanceCount(
            @PathVariable UUID eventId) {
        UUID collegeId = SecurityContextUtil.currentCollegeId(); // ← tenant check
        return ResponseEntity.ok(attendanceService.getAttendanceCount(eventId, collegeId));
    }
}