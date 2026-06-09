package com.campus.platform.registration.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrationResponseDto {

    private UUID registrationId;
    private UUID userId;
    private String userFullName;
    private String userEmail;          // ← add
    private String userPhone;          // ← add
    private String registrationNumber; // ← add (student roll number)
    private UUID eventId;
    private String eventTitle;
    private String eventVenue;
    private String clubName;
    private LocalDateTime eventDate;
    private String qrCode;
    private boolean attendanceMarked;
    private LocalDateTime registeredAt;
    private boolean isCancelled;
}