package com.campus.platform.registration.controller;

import com.campus.platform.common.util.SecurityContextUtil;
import com.campus.platform.registration.dto.RegistrationResponseDto;
import com.campus.platform.registration.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    /** Student: register for an event */
    @PostMapping("/student/events/{eventId}/register")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<RegistrationResponseDto> register(@PathVariable UUID eventId) {
        UUID userId = SecurityContextUtil.currentUserId();
        UUID collegeId = SecurityContextUtil.currentCollegeId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registrationService.registerForEvent(userId, eventId));
    }

    /** Student: view own registrations + QR codes */
    @GetMapping("/student/registrations/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<RegistrationResponseDto>> getMyRegistrations() {
        return ResponseEntity.ok(
                registrationService.getMyRegistrations(SecurityContextUtil.currentUserId()));
    }


    /** Student: cancel a registration */
    @DeleteMapping("/student/registrations/{registrationId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Void> cancelRegistration(@PathVariable UUID registrationId) {
        registrationService.cancelRegistration(registrationId, SecurityContextUtil.currentUserId());
        return ResponseEntity.noContent().build();
    }

    /** Club Admin: get all registered participants for an event */
    @GetMapping("/club/events/{eventId}/participants")
    @PreAuthorize("hasRole('CLUB_ADMIN')")
    public ResponseEntity<List<RegistrationResponseDto>> getParticipants(@PathVariable UUID eventId) {
        return ResponseEntity.ok(registrationService.getParticipantsForEvent(eventId));
    }


    @DeleteMapping("/student/events/{eventId}/register")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Void> cancelByEventId(@PathVariable UUID eventId) {
        registrationService.cancelByEventId(eventId, SecurityContextUtil.currentUserId());
        return ResponseEntity.noContent().build();
    }
}
