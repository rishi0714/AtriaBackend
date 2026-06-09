package com.campus.platform.club.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ClubAnalyticsDto {
    private String clubId;
    private String name;
    private List<EventInfo> eventsList;       // ← same package, no import needed
    private Map<String, StatsDetail> stats;   // ← same package, no import needed
}