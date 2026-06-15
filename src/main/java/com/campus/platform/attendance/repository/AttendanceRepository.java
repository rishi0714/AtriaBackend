package com.campus.platform.attendance.repository;

import com.campus.platform.attendance.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {

    boolean existsByRegistration_RegistrationId(UUID registrationId);

    Optional<Attendance> findByRegistration_RegistrationId(UUID registrationId);

    @Query("""
            SELECT a FROM Attendance a
            WHERE a.registration.event.eventId = :eventId
            """)
    List<Attendance> findAllByEventId(@Param("eventId") UUID eventId);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.registration.event.eventId = :eventId")
    long countByEventId(@Param("eventId") UUID eventId);

    long countByRegistration_User_UserId(UUID userId);

    // AttendanceRepository.java
    @Query("SELECT r.event.eventId, COUNT(a) FROM Attendance a " +
            "JOIN a.registration r " +
            "WHERE r.event.eventId IN :eventIds " +
            "GROUP BY r.event.eventId")
    List<Object[]> countByEventIds(@Param("eventIds") List<UUID> eventIds);

    @Query("SELECT a.registration.registrationId FROM Attendance a WHERE a.registration.user.userId = :userId")
    Set<UUID> findRegistrationIdsByUser(@Param("userId") UUID userId);

    // Should already exist — if not, add it
    @Query("""
    SELECT a.registration.registrationId
    FROM Attendance a
    WHERE a.registration.event.eventId = :eventId
    """)
    Set<UUID> findRegistrationIdsByEvent(@Param("eventId") UUID eventId);
}
