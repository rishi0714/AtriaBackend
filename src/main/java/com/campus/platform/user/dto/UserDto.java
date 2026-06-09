package com.campus.platform.user.dto;

import com.campus.platform.common.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {

    @NotBlank(message = "Google subject (sub) is required")
    private String googleSub;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Full name is required")
    private String fullName;

    private String pictureUrl;

    @NotNull(message = "Role is required")
    private UserRole role;

    // Alias for frontend compatibility
    public String getName() {
        return fullName;
    }
}
