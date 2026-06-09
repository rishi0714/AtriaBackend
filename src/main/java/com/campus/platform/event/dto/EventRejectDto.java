package com.campus.platform.event.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventRejectDto {

    @NotBlank(message = "Rejection reason is required")
    private String reason;
}