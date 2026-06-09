package com.campus.platform.dashboard.dto;

import com.campus.platform.common.enums.EventStatus;
import lombok.*;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClubAdminDashboardDto {

    private int totalEvents;
    private Map<EventStatus, Long> eventsByStatus;  // e.g. {PUBLISHED: 3, DRAFT: 1}
    private long totalRegistrations;
    private long totalAttendance;
    private double overallAttendanceRate;           // attended / registered * 100
    private Map<UUID, EventSummaryDto> perEventSummary;
}
