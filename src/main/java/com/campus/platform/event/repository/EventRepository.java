package com.campus.platform.event.repository;

import com.campus.platform.common.enums.EventStatus;
import com.campus.platform.event.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    @EntityGraph(attributePaths = {"club", "college"})
    Page<Event> findAllByCollege_CollegeIdAndStatus(UUID collegeId, EventStatus status, Pageable pageable);

    // EventRepository.java
    @EntityGraph(attributePaths = {"club", "college"})
    List<Event> findAllByClub_ClubId(UUID clubId);

    @EntityGraph(attributePaths = {"club", "college"})
    List<Event> findAllByStatusAndRegistrationDeadlineBefore(EventStatus status, LocalDateTime dateTime);

    @EntityGraph(attributePaths = {"club", "college"})
    List<Event> findAllByStatusAndEventDateBefore(EventStatus status, LocalDateTime dateTime);

    @EntityGraph(attributePaths = {"club", "college"})
    Optional<Event> findByEventIdAndCollege_CollegeId(UUID eventId, UUID collegeId);

    @EntityGraph(attributePaths = {"club", "college"})
    List<Event> findAllByCollege_CollegeId(UUID collegeId);

    long countByCollege_CollegeId(UUID collegeId);

    @EntityGraph(attributePaths = {"club", "college"})
    @Query("SELECT e FROM Event e WHERE e.college.collegeId = :collegeId OR e.isOpenToAll = true")
    List<Event> findVisibleEventsForCollege(@Param("collegeId") UUID collegeId);

    @EntityGraph(attributePaths = {"club", "college"})
    List<Event> findAllByIsOpenToAllTrue();

    @EntityGraph(attributePaths = {"club", "college"})
    @Query("""
    SELECT e FROM Event e
    WHERE (e.college.collegeId = :collegeId OR e.isOpenToAll = true)
    AND e.status = :status
    AND e.club.isActive = true
    """)
    Page<Event> findVisibleEventsForCollege(
            @Param("collegeId") UUID collegeId,
            @Param("status") EventStatus status,
            Pageable pageable);

    @EntityGraph(attributePaths = {"club", "college"})
    @Query("""
    SELECT e FROM Event e
    WHERE e.isOpenToAll = true
    AND e.status = :status
    AND e.club.isActive = true
    """)
    Page<Event> findAllByIsOpenToAllTrueAndStatus(
            @Param("status") EventStatus status,
            Pageable pageable);

    @EntityGraph(attributePaths = {"club", "college"})
    @Query("""
    SELECT e FROM Event e
    WHERE e.college.collegeId = :collegeId
      AND e.status = 'PUBLISHED'
      AND e.club.isActive = true
      AND (LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
    """)
    Page<Event> searchByKeyword(@Param("collegeId") UUID collegeId,
                                @Param("keyword") String keyword,
                                Pageable pageable);
}
