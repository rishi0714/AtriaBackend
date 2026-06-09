package com.campus.platform.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceDto {

    @NotBlank(message = "QR code is required")
    private String qrCode;

    @NotNull(message = "Event ID is required for cross-tenant validation")
    private UUID eventId;

}
