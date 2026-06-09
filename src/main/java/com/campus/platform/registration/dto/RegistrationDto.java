package com.campus.platform.registration.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrationDto {

    @NotNull(message = "Event ID is required")
    private UUID eventId;
}
