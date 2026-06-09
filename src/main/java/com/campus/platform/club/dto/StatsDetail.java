package com.campus.platform.club.dto;

import lombok.Data;
import java.util.List;

@Data
public class StatsDetail {
    private List<KpiData> kpis;
    private List<CategoryData> categories;
    private List<YearData> years;
}