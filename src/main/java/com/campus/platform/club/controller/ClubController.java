package com.campus.platform.club.controller;

import com.campus.platform.club.dto.ClubAnalyticsDto;
import com.campus.platform.club.dto.ClubDto;
import com.campus.platform.club.dto.ClubResponseDto;
import com.campus.platform.club.service.AnalyticsService;
import com.campus.platform.club.service.ClubService;
import com.campus.platform.common.util.SecurityContextUtil;
import com.campus.platform.security.jwt.JwtAuthenticatedPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ClubController {

    private final ClubService clubService;
    private final AnalyticsService analyticsService;

    @GetMapping("/api/student/clubs")
    @PreAuthorize("hasAnyRole('STUDENT', 'CLUB_ADMIN')")
    public ResponseEntity<List<ClubResponseDto>> getClubsForStudent() {
        UUID collegeId = SecurityContextUtil.currentCollegeId();
        return ResponseEntity.ok(clubService.getClubsByCollege(collegeId)); // active only
    }


    // COLLEGE_ADMIN: full CRUD over clubs in their college
    @PostMapping("/api/college/clubs")
    @PreAuthorize("hasRole('COLLEGE_ADMIN')")
    public ResponseEntity<ClubResponseDto> createClub(
            @Valid @RequestBody ClubDto dto) {
        UUID collegeId = SecurityContextUtil.currentCollegeId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(clubService.createClub(collegeId, dto));
    }

    @GetMapping("/api/college/clubs")
    @PreAuthorize("hasAnyRole('COLLEGE_ADMIN', 'CLUB_ADMIN')")
    public ResponseEntity<List<ClubResponseDto>> getClubsByCollege() {
        UUID collegeId = SecurityContextUtil.currentCollegeId();
        return ResponseEntity.ok(clubService.getClubsByCollege(collegeId));
    }

    @GetMapping("/api/college/clubs/{clubId}")
    @PreAuthorize("hasAnyRole('COLLEGE_ADMIN', 'CLUB_ADMIN')")
    public ResponseEntity<ClubResponseDto> getClubById(
            @PathVariable UUID clubId) {
        UUID collegeId = SecurityContextUtil.currentCollegeId();
        return ResponseEntity.ok(clubService.getClubById(clubId, collegeId));
    }

    @PutMapping("/api/college/clubs/{clubId}")
    @PreAuthorize("hasAnyRole('COLLEGE_ADMIN', 'CLUB_ADMIN')")
    public ResponseEntity<ClubResponseDto> updateClub(
            @PathVariable UUID clubId,
            @Valid @RequestBody ClubDto dto) {
        UUID collegeId = SecurityContextUtil.currentCollegeId();
        return ResponseEntity.ok(clubService.updateClub(clubId, collegeId, dto));
    }

    @GetMapping("/api/club/analytics")
    @PreAuthorize("hasRole('CLUB_ADMIN')")
    public ResponseEntity<ClubAnalyticsDto> getClubAnalytics() {

        UUID userId = SecurityContextUtil.currentUserId();

        return ResponseEntity.ok(
                analyticsService.getClubAnalytics(userId)
        );
    }

    // Only COLLEGE_ADMIN can assign who runs a club
    @PatchMapping("/api/college/clubs/{clubId}/admin")
    @PreAuthorize("hasRole('COLLEGE_ADMIN')")
    public ResponseEntity<ClubResponseDto> assignClubAdmin(
            @PathVariable UUID clubId,
            @RequestParam String email) {
        UUID collegeId = SecurityContextUtil.currentCollegeId();
        return ResponseEntity.ok(clubService.assignClubAdmin(clubId, email, collegeId));
    }


    @DeleteMapping("/api/college/clubs/{clubId}")
    @PreAuthorize("hasRole('COLLEGE_ADMIN')")
    public ResponseEntity<Void> deleteClub(@PathVariable UUID clubId) { // ← remove @RequestParam UUID collegeId
        UUID collegeId = SecurityContextUtil.currentCollegeId();        // ← read from context
        clubService.deleteClub(clubId, collegeId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/college/clubs/{clubId}/deactivate")
    @PreAuthorize("hasRole('COLLEGE_ADMIN')")
    public ResponseEntity<ClubResponseDto> deactivateClub(@PathVariable UUID clubId) {
        UUID collegeId = SecurityContextUtil.currentCollegeId();
        return ResponseEntity.ok(clubService.deactivateClub(clubId, collegeId));
    }

    @PatchMapping("/api/college/clubs/{clubId}/activate")
    @PreAuthorize("hasRole('COLLEGE_ADMIN')")
    public ResponseEntity<ClubResponseDto> activateClub(@PathVariable UUID clubId) {
        UUID collegeId = SecurityContextUtil.currentCollegeId();
        return ResponseEntity.ok(clubService.activateClub(clubId, collegeId));
    }

    @GetMapping("/api/platform/colleges/{collegeId}/clubs/count")
    @PreAuthorize("hasRole('PLATFORM_OWNER')")
    public ResponseEntity<Long> getClubCountByCollege(
            @PathVariable UUID collegeId) {

        return ResponseEntity.ok(
                clubService.getClubCountByCollege(collegeId)
        );
    }

    @GetMapping("/api/platform/colleges/{collegeId}/clubs")
    @PreAuthorize("hasRole('PLATFORM_OWNER')")
    public ResponseEntity<List<ClubResponseDto>> getClubsByCollegeForPlatform(
            @PathVariable UUID collegeId) {

        return ResponseEntity.ok(
                clubService.getClubsByCollege(collegeId)
        );
    }
}
