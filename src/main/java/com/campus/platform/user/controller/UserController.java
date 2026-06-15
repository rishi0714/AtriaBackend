package com.campus.platform.user.controller;

import com.campus.platform.common.util.SecurityContextUtil;
import com.campus.platform.user.dto.AssignCollegeAdminDto;
import com.campus.platform.user.dto.CompleteGuestProfileDto;
import com.campus.platform.user.dto.UserResponseDto;
import com.campus.platform.user.service.UserService;
import jakarta.validation.Valid;
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
public class UserController {

    private final UserService userService;

    @PostMapping("/me/complete-guest-profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponseDto> completeGuestProfile(
            @Valid @RequestBody CompleteGuestProfileDto dto) {
        return ResponseEntity.ok(
                userService.completeGuestProfile(
                        SecurityContextUtil.currentUserId(), dto));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponseDto> getMyProfile() {
        return ResponseEntity.ok(userService.getUserById(SecurityContextUtil.currentUserId()));
    }

    @GetMapping("/platform/colleges/{collegeId}/admins")
    @PreAuthorize("hasRole('PLATFORM_OWNER')")
    public ResponseEntity<List<UserResponseDto>> getCollegeAdmins(@PathVariable UUID collegeId) {
        return ResponseEntity.ok(userService.getCollegeAdmins(collegeId));
    }

    @PostMapping("/platform/colleges/assign-admin")
    @PreAuthorize("hasRole('PLATFORM_OWNER')")
    public ResponseEntity<UserResponseDto> assignCollegeAdmin(
            @Valid @RequestBody AssignCollegeAdminDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.assignCollegeAdmin(dto));
    }

    @DeleteMapping("/platform/colleges/{collegeId}/admin")
    @PreAuthorize("hasRole('PLATFORM_OWNER')")
    public ResponseEntity<Void> removeCollegeAdmin(
            @PathVariable UUID collegeId,
            @RequestParam String email) {
        userService.removeCollegeAdmin(collegeId, email);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/platform/users/count")
    @PreAuthorize("hasRole('PLATFORM_OWNER')")
    public ResponseEntity<Long> getTotalUserCount() {
        return ResponseEntity.ok(userService.getTotalUserCount());
    }

    @GetMapping("/platform/colleges/users/count")
    @PreAuthorize("hasRole('PLATFORM_OWNER')")
    public ResponseEntity<Long> getUserCountByCollegeName(@RequestParam String collegeName) {
        return ResponseEntity.ok(userService.getUserCountByCollegeName(collegeName));
    }

    @GetMapping("/platform/colleges/{collegeId}/users/count")
    @PreAuthorize("hasRole('PLATFORM_OWNER')")
    public ResponseEntity<Long> getUserCountByCollege(
            @PathVariable UUID collegeId) {

        return ResponseEntity.ok(
                userService.getUserCountByCollege(collegeId)
        );
    }
    @GetMapping("/college/admins")
    @PreAuthorize("hasRole('COLLEGE_ADMIN')")
    public ResponseEntity<List<UserResponseDto>> getAdminsByCollege() {
        UUID collegeId = SecurityContextUtil.currentCollegeId();
        List<UserResponseDto> admins = userService.getAdminsByCollege(collegeId);
        return ResponseEntity.ok(admins);
    }
}