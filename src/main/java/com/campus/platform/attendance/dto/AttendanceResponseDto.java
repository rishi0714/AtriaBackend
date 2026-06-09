package com.campus.platform.attendance.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceResponseDto {

    private UUID attendanceId;
    private UUID registrationId;
    private UUID userId;
    private String userFullName;
    private UUID eventId;
    private String eventTitle;
    private UUID scannedById;
    private String scannedByName;
    private LocalDateTime scannedAt;
}
