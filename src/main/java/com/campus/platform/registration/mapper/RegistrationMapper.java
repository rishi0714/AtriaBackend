package com.campus.platform.registration.mapper;

import com.campus.platform.attendance.repository.AttendanceRepository;
import com.campus.platform.registration.dto.RegistrationResponseDto;
import com.campus.platform.registration.entity.Registration;
import org.springframework.stereotype.Component;

@Component
public class RegistrationMapper {

    public RegistrationResponseDto toResponseDto(Registration registration, boolean attendanceMarked) {
        return RegistrationResponseDto.builder()
                .registrationId(registration.getRegistrationId())
                .userId(registration.getUser().getUserId())
                .userFullName(registration.getUser().getFullName())
                .userEmail(registration.getUser().getEmail())
                .userPhone(registration.getUser().getPhoneNumber())
                .registrationNumber(registration.getUser().getRegistrationNumber())
                .eventId(registration.getEvent().getEventId())
                .eventTitle(registration.getEvent().getTitle())
                .eventVenue(registration.getEvent().getVenue())
                .clubName(registration.getEvent().getClub().getName())
                .eventDate(registration.getEvent().getEventDate())
                .qrCode(registration.getQrCode())
                .registeredAt(registration.getRegisteredAt())
                .isCancelled(registration.isCancelled())
                .attendanceMarked(attendanceMarked) // ← populated
                .build();
    }

    // existing single-arg overload — defaults to false, no N+1
    public RegistrationResponseDto toResponseDto(Registration registration) {
        return toResponseDto(registration, false);
    }
}
