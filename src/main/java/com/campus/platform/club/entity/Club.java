package com.campus.platform.club.entity;

import com.campus.platform.college.entity.College;
import com.campus.platform.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "clubs",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_club_name_per_college",
                columnNames = {"college_id", "name"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Club {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "club_id", updatable = false, nullable = false)
    private UUID clubId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "college_id", referencedColumnName = "college_id", nullable = false)
    private College college;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "club_category")
    private String clubCategory;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "managed_by", referencedColumnName = "user_id")
    private User managedBy;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
