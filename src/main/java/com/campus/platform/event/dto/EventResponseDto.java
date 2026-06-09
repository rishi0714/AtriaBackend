package com.campus.platform.event.dto;

import com.campus.platform.common.enums.EventStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventResponseDto {
    private UUID eventId;
    private UUID clubId;
    private String club;
    private UUID collegeId;
    private String collegeName;
    private String title;
    private String description;
    private String venue;
    private LocalDateTime date;
    private LocalDateTime registrationDeadline;
    private int maxCapacity;
    private int registeredCount;
    private EventStatus status;
    private String image;
    private String category;
    private boolean isOpenToAll;
    private String rejectionReason; // ← added; null unless status == REJECTED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}