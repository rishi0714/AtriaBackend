package com.campus.platform.event.entity;

import com.campus.platform.club.entity.Club;
import com.campus.platform.college.entity.College;
import com.campus.platform.common.enums.EventStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "event_id", updatable = false, nullable = false)
    private UUID eventId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "club_id", referencedColumnName = "club_id", nullable = false)
    private Club club;

    /**
     * Denormalized tenant anchor — avoids joins on the most queried table.
     * Must always match club.college.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "college_id", referencedColumnName = "college_id", nullable = false)
    private College college;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "venue", nullable = false)
    private String venue;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Column(name = "registration_deadline", nullable = false)
    private LocalDateTime registrationDeadline;

    @Column(name = "max_capacity", nullable = false)
    private int maxCapacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private EventStatus status = EventStatus.PENDING_APPROVAL; // ← changed

    @Column(name = "poster_url")
    private String posterUrl;

    @Column(name = "category")
    private String category;

    @Column(name = "is_open_to_all", nullable = false)
    @Builder.Default
    private boolean isOpenToAll = false;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason; // ← added, nullable

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}