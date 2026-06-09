package com.campus.platform.dashboard.dto;

import com.campus.platform.registration.dto.RegistrationResponseDto;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentDashboardDto {

    private int totalRegistrations;
    private int totalAttended;
    private List<RegistrationResponseDto> upcomingEvents;
    private List<RegistrationResponseDto> pastEvents;
}
