package com.campus.platform.attendance.entity;

import com.campus.platform.registration.entity.Registration;
import com.campus.platform.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "attendance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "attendance_id", updatable = false, nullable = false)
    private UUID attendanceId;

    /**
     * UNIQUE on registration_id enforces one attendance record per registration.
     * Duplicate QR scans are prevented at this constraint level.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registration_id", referencedColumnName = "registration_id",
            nullable = false, unique = true)
    private Registration registration;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scanned_by", referencedColumnName = "user_id", nullable = false)
    private User scannedBy;

    @Column(name = "scanned_at", nullable = false)
    private LocalDateTime scannedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
