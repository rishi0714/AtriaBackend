package com.campus.platform.event.controller;

import com.campus.platform.common.enums.EventStatus;
import com.campus.platform.common.util.SecurityContextUtil;
import com.campus.platform.event.dto.EventDto;
import com.campus.platform.event.dto.EventRejectDto;
import com.campus.platform.event.dto.EventResponseDto;
import com.campus.platform.event.service.EventService;
import com.campus.platform.registration.dto.RegistrationResponseDto;
import com.campus.platform.registration.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final RegistrationService registrationService;

    // ── Student endpoints ────────────────────────────────────────────────────────

    @GetMapping("/api/student/events")
    @PreAuthorize("hasAnyRole('STUDENT', 'CLUB_ADMIN')")
    public ResponseEntity<Page<EventResponseDto>> getPublishedEvents(
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        if (keyword != null && !keyword.isBlank()) {
            UUID collegeId = SecurityContextUtil.currentCollegeId();
            return ResponseEntity.ok(eventService.searchEvents(collegeId, keyword, pageable));
        }
        return ResponseEntity.ok(eventService.getVisibleEvents(pageable)); // ← only change
    }

    @GetMapping("/api/platform/colleges/{collegeId}/events/count")
    @PreAuthorize("hasRole('PLATFORM_OWNER')")
    public ResponseEntity<Long> getEventCountByCollege(
            @PathVariable UUID collegeId) {

        return ResponseEntity.ok(
                eventService.getEventCountByCollege(collegeId)
        );
    }

    @GetMapping("/api/student/events/{eventId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<EventResponseDto> getEventById(@PathVariable UUID eventId) {
        UUID collegeId = SecurityContextUtil.currentCollegeId();
        return ResponseEntity.ok(eventService.getEventById(eventId, collegeId));
    }

    // ── Club Admin endpoints ─────────────────────────────────────────────────────

    @PostMapping("/api/club/events")
    @PreAuthorize("hasRole('CLUB_ADMIN')")
    public ResponseEntity<EventResponseDto> createEvent(@Valid @RequestBody EventDto dto) {
        UUID collegeId = SecurityContextUtil.currentCollegeId();
        UUID userId = SecurityContextUtil.currentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventService.createEventForManager(userId, collegeId, dto));
    }

    @GetMapping("/api/club/events")
    @PreAuthorize("hasRole('CLUB_ADMIN')")
    public ResponseEntity<List<EventResponseDto>> getMyClubEvents() {
        UUID userId = SecurityContextUtil.currentUserId();
        UUID collegeId = SecurityContextUtil.currentCollegeId();
        return ResponseEntity.ok(eventService.getEventsByManager(userId, collegeId));
    }

    @PutMapping("/api/club/events/{eventId}")
    @PreAuthorize("hasRole('CLUB_ADMIN')")
    public ResponseEntity<EventResponseDto> updateEvent(
            @PathVariable UUID eventId,
            @Valid @RequestBody EventDto dto) {
        UUID collegeId = SecurityContextUtil.currentCollegeId();
        return ResponseEntity.ok(eventService.updateEvent(eventId, collegeId, dto));
    }

    @PatchMapping("/api/club/events/{eventId}/status")
    @PreAuthorize("hasRole('CLUB_ADMIN')")
    public ResponseEntity<EventResponseDto> updateEventStatus(
            @PathVariable UUID eventId,
            @RequestParam EventStatus status) {
        UUID collegeId = SecurityContextUtil.currentCollegeId();
        return ResponseEntity.ok(eventService.updateStatus(eventId, collegeId, status));
    }

    @PatchMapping("/api/club/events/{eventId}/resubmit")
    @PreAuthorize("hasRole('CLUB_ADMIN')")
    public ResponseEntity<EventResponseDto> resubmitEvent(@PathVariable UUID eventId) {
        UUID collegeId = SecurityContextUtil.currentCollegeId();
        return ResponseEntity.ok(eventService.resubmitEvent(eventId, collegeId));
    }

    @DeleteMapping("/api/club/events/{eventId}")
    @PreAuthorize("hasRole('CLUB_ADMIN')")
    public ResponseEntity<Void> deleteEvent(@PathVariable UUID eventId) {
        UUID collegeId = SecurityContextUtil.currentCollegeId();
        eventService.deleteEvent(eventId, collegeId);
        return ResponseEntity.noContent().build();
    }

    // ── College Admin endpoints ──────────────────────────────────────────────────

    @GetMapping("/api/college/events")
    @PreAuthorize("hasRole('COLLEGE_ADMIN')")
    public ResponseEntity<List<EventResponseDto>> getEventsForCollege() {
        return ResponseEntity.ok(
                eventService.getEventsByCollege(SecurityContextUtil.currentCollegeId()));
    }

    @PatchMapping("/api/college/events/{eventId}/approve")
    @PreAuthorize("hasRole('COLLEGE_ADMIN')")
    public ResponseEntity<EventResponseDto> approveEvent(@PathVariable UUID eventId) {
        UUID collegeId = SecurityContextUtil.currentCollegeId();
        return ResponseEntity.ok(eventService.approveEvent(eventId, collegeId));
    }

    @PatchMapping("/api/college/events/{eventId}/reject")
    @PreAuthorize("hasRole('COLLEGE_ADMIN')")
    public ResponseEntity<EventResponseDto> rejectEvent(
            @PathVariable UUID eventId,
            @Valid @RequestBody EventRejectDto dto) {
        UUID collegeId = SecurityContextUtil.currentCollegeId();
        return ResponseEntity.ok(eventService.rejectEvent(eventId, collegeId, dto.getReason()));
    }


}