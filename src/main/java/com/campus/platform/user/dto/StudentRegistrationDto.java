package com.campus.platform.user.dto;

import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentRegistrationDto {

    private UUID id;
    private String status;
    private EventInfo event;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EventInfo {
        private String title;
        private String club;
        private String date;
        private String time;
        private String venue;
    }
}