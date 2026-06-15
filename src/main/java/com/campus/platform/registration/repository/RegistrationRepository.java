package com.campus.platform.registration.repository;

import com.campus.platform.registration.entity.Registration;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, UUID> {
    Optional<Registration> findByRegistrationIdAndUser_UserId(UUID registrationId, UUID userId);

    Optional<Registration> findByQrCode(String qrCode);

    boolean existsByUser_UserIdAndEvent_EventIdAndIsCancelledFalse(UUID userId, UUID eventId);

    long countByEvent_EventIdAndIsCancelledFalse(UUID eventId);

    Optional<Registration> findByUser_UserIdAndEvent_EventId(UUID userId, UUID eventId);

    @EntityGraph(attributePaths = {"user", "event", "event.club", "event.college"})
    List<Registration> findAllByEvent_EventIdAndIsCancelledFalse(UUID eventId);

    @EntityGraph(attributePaths = {"user", "event", "event.club", "event.college"})
    List<Registration> findAllByUser_UserIdAndIsCancelledFalse(UUID userId);

    long countByEvent_College_CollegeId(UUID collegeId);

    @EntityGraph(attributePaths = {"user"})
    List<Registration> findAllByEvent_EventId(UUID eventId); // ← all including cancelled

    @EntityGraph(attributePaths = {"user"})
    List<Registration> findAllByUser_UserId(UUID userId);    // ← all registrations for a user including cancelled

    // RegistrationRepository.java
    @Query("SELECT r.event.eventId, COUNT(r) FROM Registration r " +
            "WHERE r.event.eventId IN :eventIds AND r.isCancelled = false " +
            "GROUP BY r.event.eventId")
    List<Object[]> countByEventIds(@Param("eventIds") List<UUID> eventIds);

    Optional<Registration> findByUser_EmailAndEvent_EventIdAndIsCancelledFalse(
            String email, UUID eventId);

    @Query("SELECT r FROM Registration r " +
            "JOIN FETCH r.user " +
            "JOIN FETCH r.event e " +
            "JOIN FETCH e.club " +
            "WHERE r.registrationId = :id")
    Optional<Registration> findByIdWithDetails(@Param("id") UUID id);




    // RegistrationRepository.java — add this alongside countByEventIds
    @Query("SELECT r FROM Registration r " +
            "JOIN FETCH r.user " +
            "WHERE r.event.eventId IN :eventIds AND r.isCancelled = false")
    List<Registration> findAllByEvent_EventIdInAndIsCancelledFalse(@Param("eventIds") List<UUID> eventIds);

    // Add this alongside your existing queries
    @Query("""
    SELECT r FROM Registration r
    JOIN FETCH r.user
    JOIN FETCH r.event e
    JOIN FETCH e.club
    JOIN FETCH e.college
    WHERE r.event.eventId = :eventId
    AND r.isCancelled = false
""")
    List<Registration> findParticipantsWithDetails(@Param("eventId") UUID eventId);
}