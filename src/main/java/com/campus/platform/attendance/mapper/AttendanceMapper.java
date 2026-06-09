package com.campus.platform.attendance.mapper;

import com.campus.platform.attendance.dto.AttendanceResponseDto;
import com.campus.platform.attendance.entity.Attendance;
import org.springframework.stereotype.Component;

@Component
public class AttendanceMapper {

    public AttendanceResponseDto toResponseDto(Attendance attendance) {
        return AttendanceResponseDto.builder()
                .attendanceId(attendance.getAttendanceId())
                .registrationId(attendance.getRegistration().getRegistrationId())
                .userId(attendance.getRegistration().getUser().getUserId())
                .userFullName(attendance.getRegistration().getUser().getFullName())
                .eventId(attendance.getRegistration().getEvent().getEventId())
                .eventTitle(attendance.getRegistration().getEvent().getTitle())
                .scannedById(attendance.getScannedBy().getUserId())
                .scannedByName(attendance.getScannedBy().getFullName())
                .scannedAt(attendance.getScannedAt())
                .build();
    }
}
