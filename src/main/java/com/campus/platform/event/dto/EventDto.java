package com.campus.platform.event.dto;

import com.campus.platform.common.enums.EventStatus;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventDto {

    private UUID clubId;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotBlank(message = "Venue is required")
    private String venue;

    @NotNull(message = "Event date is required")
    @Future(message = "Event date must be in the future")
    private LocalDateTime eventDate;

    @NotNull(message = "Registration deadline is required")
    private LocalDateTime registrationDeadline;

    @Min(value = 1, message = "Max capacity must be at least 1")
    private int maxCapacity;

    private boolean openToAll = false;

    // ← status removed entirely — always set server-side

    private String posterUrl;
    private String category;
}