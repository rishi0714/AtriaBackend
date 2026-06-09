package com.campus.platform.dashboard.controller;

import com.campus.platform.club.service.ClubService;
import com.campus.platform.common.util.SecurityContextUtil;
import com.campus.platform.dashboard.dto.ClubAdminDashboardDto;
import com.campus.platform.dashboard.dto.CollegeAdminDashboardDto;
import com.campus.platform.dashboard.dto.StudentDashboardDto;
import com.campus.platform.dashboard.dto.SuperAdminDashboardDto;
import com.campus.platform.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final ClubService clubService;

    @GetMapping("/student/dashboard")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, Object>> studentDashboard() {  // ✅ change return type
        UUID userId = SecurityContextUtil.currentUserId();
        UUID collegeId = SecurityContextUtil.currentCollegeId();

        StudentDashboardDto dto = dashboardService.getStudentDashboard(userId);
        long clubsJoined = clubService.getClubsByCollege(collegeId).size(); // ✅ add this

        return ResponseEntity.ok(Map.of(
                "clubsJoined", clubsJoined,                          // ✅ frontend expects this
                "totalRegistrations", dto.getTotalRegistrations(),
                "totalAttended", dto.getTotalAttended()
        ));
    }

    @GetMapping("/student/attendance")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, Object>> studentAttendance() {
        UUID userId = SecurityContextUtil.currentUserId();
        StudentDashboardDto dto = dashboardService.getStudentDashboard(userId);

        long total = dto.getTotalRegistrations();
        long attended = dto.getTotalAttended();

        String rate = total > 0
                ? Math.round((attended * 100.0) / total) + "%"
                : "0%";

        return ResponseEntity.ok(Map.of("rate", rate));
    }

    @GetMapping("/college/dashboard")
    @PreAuthorize("hasRole('COLLEGE_ADMIN')")
    public ResponseEntity<CollegeAdminDashboardDto> collegeAdminDashboard() {
        return ResponseEntity.ok(
                dashboardService.getCollegeAdminDashboard(
                        SecurityContextUtil.currentCollegeId()));
    }

    @GetMapping("/club/dashboard")
    @PreAuthorize("hasRole('CLUB_ADMIN')")
    public ResponseEntity<ClubAdminDashboardDto> clubAdminDashboard() {
        return ResponseEntity.ok(
                dashboardService.getClubAdminDashboard(SecurityContextUtil.currentCollegeId()));
    }

    @GetMapping("/platform/dashboard")
    @PreAuthorize("hasRole('PLATFORM_OWNER')")
    public ResponseEntity<SuperAdminDashboardDto> superAdminDashboard() {
        return ResponseEntity.ok(dashboardService.getSuperAdminDashboard());
    }
}
