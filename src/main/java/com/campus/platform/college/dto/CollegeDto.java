package com.campus.platform.college.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CollegeDto {

    @NotBlank(message = "College name is required")
    private String name;

    private String logoUrl;
}