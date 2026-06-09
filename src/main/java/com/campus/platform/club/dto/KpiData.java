package com.campus.platform.club.dto;

import lombok.Data;

@Data
public class KpiData {
    private String title;
    private String value;
    private String change;
    private String trend; // "up" or "down"
    private String description;
}