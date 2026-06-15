package com.campus.platform.college.controller;

import com.campus.platform.college.dto.*;
import com.campus.platform.college.service.CollegeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/platform/colleges")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PLATFORM_OWNER')")
public class CollegeController {

    private final CollegeService collegeService;

    @PostMapping("/setup")
    public ResponseEntity<CollegeSetupResponseDto> setupCollege(
            @Valid @RequestBody CollegeSetupDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(collegeService.setupCollege(dto));
    }

    @PostMapping
    public ResponseEntity<CollegeResponseDto> createCollege(
            @Valid @RequestBody CollegeDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(collegeService.createCollege(dto));
    }

    @GetMapping
    public ResponseEntity<List<CollegeResponseDto>> getAllColleges() {
        return ResponseEntity.ok(collegeService.getAllColleges());
    }

    @GetMapping("/{collegeId}")
    public ResponseEntity<CollegeResponseDto> getCollegeById(
            @PathVariable UUID collegeId) {
        return ResponseEntity.ok(collegeService.getCollegeById(collegeId));
    }

    @PutMapping("/{collegeId}")
    public ResponseEntity<CollegeResponseDto> updateCollege(
            @PathVariable UUID collegeId,
            @Valid @RequestBody CollegeDto dto) {
        return ResponseEntity.ok(collegeService.updateCollege(collegeId, dto));
    }

    @PatchMapping("/{collegeId}/status")
    public ResponseEntity<CollegeResponseDto> setActiveStatus(
            @PathVariable UUID collegeId,
            @RequestParam boolean isActive) {
        return ResponseEntity.ok(collegeService.setActiveStatus(collegeId, isActive));
    }

    // ── Domain sub-resource ─────────────────────────────────────────────────────

    @GetMapping("/{collegeId}/domains")
    public ResponseEntity<List<CollegeDomainResponseDto>> getDomains(
            @PathVariable UUID collegeId) {
        return ResponseEntity.ok(collegeService.getDomainsForCollege(collegeId));
    }

    @PostMapping("/{collegeId}/domains")
    public ResponseEntity<CollegeDomainResponseDto> addDomain(
            @PathVariable UUID collegeId,
            @Valid @RequestBody CollegeDomainDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(collegeService.addDomain(collegeId, dto));
    }

    @DeleteMapping("/{collegeId}/domains/{domainId}")
    public ResponseEntity<Void> removeDomain(
            @PathVariable UUID collegeId,
            @PathVariable UUID domainId) {
        collegeService.removeDomain(collegeId, domainId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{collegeId}/domains/{domainId}/primary")
    public ResponseEntity<CollegeDomainResponseDto> setPrimaryDomain(
            @PathVariable UUID collegeId,
            @PathVariable UUID domainId) {
        return ResponseEntity.ok(collegeService.setPrimaryDomain(collegeId, domainId));
    }
}