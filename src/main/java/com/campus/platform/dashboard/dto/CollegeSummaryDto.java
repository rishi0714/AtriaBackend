package com.campus.platform.dashboard.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollegeSummaryDto {

    private UUID collegeId;
    private String name;
    private String primaryDomain;
    private boolean isActive;
    private long totalUsers;
    private long totalEvents;
    private long totalRegistrations;
}
