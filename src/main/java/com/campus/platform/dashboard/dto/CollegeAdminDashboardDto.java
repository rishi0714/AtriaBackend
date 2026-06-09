package com.campus.platform.dashboard.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollegeAdminDashboardDto {

    private String collegeName;
    private long totalStudents;
    private long totalClubAdmins;
    private long totalClubs;
    private long totalEvents;
    private long totalRegistrations;
    private long totalAttendance;
    private List<EventSummaryDto> recentEvents;
}