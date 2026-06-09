package com.campus.platform.dashboard.dto;

import com.campus.platform.common.enums.EventStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventSummaryDto {

    private UUID eventId;
    private String title;
    private EventStatus status;
    private LocalDateTime eventDate;
    private int maxCapacity;
    private long registeredCount;
    private long attendedCount;
    private long noShowCount;
    private double attendancePercentage;  // attendedCount / registeredCount * 100
}
