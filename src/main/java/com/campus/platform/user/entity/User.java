package com.campus.platform.user.entity;

import com.campus.platform.college.entity.College;
import com.campus.platform.common.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "college_id", referencedColumnName = "college_id")
    private College college; // null for SUPER_ADMIN

    @Column(name = "google_sub", unique = true)
    private String googleSub;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "picture_url")
    private String pictureUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @Builder.Default
    private UserRole role = UserRole.STUDENT;

    @Column(name = "year")
    private Short year;

    @Column(name = "stream", length = 100)
    private String stream;

    @Column(name = "phone_number", length = 15)
    private String phoneNumber;

    @Column(name = "registration_number", length = 50)
    private String registrationNumber;

    @Column(name = "profile_complete", nullable = false)
    @Builder.Default
    private boolean profileComplete = false;

    // User.java — just add this one field
    @Column(name = "refresh_token", length = 512)
    private String refreshToken;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Helpers ──────────────────────────────────────────────
    public boolean belongsToCollege(UUID collegeId) {
        return this.college != null && this.college.getCollegeId().equals(collegeId);
    }


}
