package com.campus.platform.dashboard.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuperAdminDashboardDto {

    private int totalColleges;
    private int activeColleges;
    private int inactiveColleges;
    private long totalUsersAcrossAllColleges;
    private long totalEventsAcrossAllColleges;
    private List<CollegeSummaryDto> colleges;
}
