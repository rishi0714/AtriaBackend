package com.campus.platform.dashboard.service;

import com.campus.platform.attendance.repository.AttendanceRepository;
import com.campus.platform.club.entity.Club;
import com.campus.platform.club.repository.ClubRepository;
import com.campus.platform.college.entity.College;
import com.campus.platform.college.entity.CollegeDomain;
import com.campus.platform.college.repository.CollegeRepository;
import com.campus.platform.common.enums.EventStatus;
import com.campus.platform.common.enums.UserRole;
import com.campus.platform.common.util.SecurityContextUtil;
import com.campus.platform.dashboard.dto.*;
import com.campus.platform.event.entity.Event;
import com.campus.platform.event.repository.EventRepository;
import com.campus.platform.registration.dto.RegistrationResponseDto;
import com.campus.platform.registration.entity.Registration;
import com.campus.platform.registration.mapper.RegistrationMapper;
import com.campus.platform.registration.repository.RegistrationRepository;
import com.campus.platform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final RegistrationRepository registrationRepository;
    private final AttendanceRepository attendanceRepository;
    private final EventRepository eventRepository;
    private final CollegeRepository collegeRepository;
    private final UserRepository userRepository;
    private final ClubRepository clubRepository;
    private final RegistrationMapper registrationMapper;

    // ── Shared helper ────────────────────────────────────────────────────────────

    /**
     * Given a list of event IDs, fetches registration counts and attendance counts
     * in exactly two GROUP BY queries and returns them as UUID-keyed maps.
     *
     * This replaces the N+1 pattern where countByEvent and countAttendance were
     * called individually inside a loop, producing 2*N extra round-trips.
     *
     * Returns empty maps (not null) when eventIds is empty so callers can safely
     * use getOrDefault without null checks.
     */
    private BulkCounts fetchBulkCounts(List<UUID> eventIds) {
        if (eventIds.isEmpty()) {
            return new BulkCounts(Map.of(), Map.of());
        }

        // One query: SELECT event_id, COUNT(*) FROM registrations
        //            WHERE event_id IN (...) AND is_cancelled = false
        //            GROUP BY event_id
        Map<UUID, Long> regCounts = registrationRepository
                .countByEventIds(eventIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],   // event_id
                        row -> (Long)  row[1]   // count
                ));

        // One query: SELECT r.event_id, COUNT(*) FROM attendance a
        //            JOIN registrations r ON r.id = a.registration_id
        //            WHERE r.event_id IN (...)
        //            GROUP BY r.event_id
        Map<UUID, Long> attCounts = attendanceRepository
                .countByEventIds(eventIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],   // event_id
                        row -> (Long)  row[1]   // count
                ));

        return new BulkCounts(regCounts, attCounts);
    }

    /**
     * Builds an EventSummaryDto from pre-fetched bulk count maps.
     * All arithmetic (noShow, attendancePercentage) lives here so it isn't
     * duplicated across dashboard methods.
     */
    private EventSummaryDto buildEventSummary(Event event, BulkCounts counts) {
        long registered = counts.reg().getOrDefault(event.getEventId(), 0L);
        long attended   = counts.att().getOrDefault(event.getEventId(), 0L);
        long noShow     = registered - attended;
        double pct      = registered > 0 ? (attended * 100.0 / registered) : 0.0;

        return EventSummaryDto.builder()
                .eventId(event.getEventId())
                .title(event.getTitle())
                .status(event.getStatus())
                .eventDate(event.getEventDate())
                .maxCapacity(event.getMaxCapacity())
                .registeredCount(registered)
                .attendedCount(attended)
                .noShowCount(noShow)
                .attendancePercentage(Math.round(pct * 10.0) / 10.0)
                .build();
    }

    /**
     * Simple value carrier for the two bulk count maps.
     * Using a record avoids a meaningless Pair/Tuple dependency.
     */
    private record BulkCounts(Map<UUID, Long> reg, Map<UUID, Long> att) {}

    // ── Student Dashboard ────────────────────────────────────────────────────────

    /**
     * Loads all active registrations for the student in one query, then splits
     * them in-memory into upcoming / past by comparing eventDate to now.
     *
     * Attendance count is a single aggregate query, not a per-registration check.
     *
     * Total DB round-trips: 2 (registrations + attendance count)
     */
    public StudentDashboardDto getStudentDashboard(UUID userId) {
        // Fetches registrations with their events eagerly (JOIN in repo query)
        List<Registration> active = registrationRepository
                .findAllByUser_UserIdAndIsCancelledFalse(userId);

        LocalDateTime now = LocalDateTime.now();

        // Split in-memory; no extra DB calls needed
        List<RegistrationResponseDto> upcoming = active.stream()
                .filter(r -> r.getEvent().getEventDate().isAfter(now))
                .map(registrationMapper::toResponseDto)
                .collect(Collectors.toList());

        List<RegistrationResponseDto> past = active.stream()
                .filter(r -> r.getEvent().getEventDate().isBefore(now))
                .map(registrationMapper::toResponseDto)
                .collect(Collectors.toList());

        // Single COUNT query — not a per-registration existence check
        long attended = attendanceRepository.countByRegistration_User_UserId(userId);

        return StudentDashboardDto.builder()
                .totalRegistrations(active.size())
                .totalAttended((int) attended)
                .upcomingEvents(upcoming)
                .pastEvents(past)
                .build();
    }

    // ── Club Admin Dashboard ─────────────────────────────────────────────────────

    /**
     * Fetches all events for the club and computes per-event registration and
     * attendance summaries.
     *
     * Previously this produced 2*N extra queries (one countRegistrations + one
     * countAttendance per event). Now it uses two GROUP BY queries to fetch all
     * counts at once, then does an O(1) map lookup per event.
     *
     * Total DB round-trips: 3
     *   1. findByManagedBy            → resolve club from JWT userId
     *   2. findAllByClub_ClubId       → load all events for the club
     *   3. countByEventIds (reg)      → registration counts for all events
     *   4. countByEventIds (att)      → attendance counts for all events
     *
     * Note: round-trips 3+4 are encapsulated in fetchBulkCounts().
     */
    public ClubAdminDashboardDto getClubAdminDashboard(UUID collegeId) {
        UUID userId = SecurityContextUtil.currentUserId();

        // 1. Resolve which club this admin manages
        Club club = clubRepository.findByManagedBy_UserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Club not found"));

        // 2. Load all events for this club
        List<Event> allEvents = eventRepository.findAllByClub_ClubId(club.getClubId());

        // 3+4. Two bulk GROUP BY queries instead of 2*N individual count queries
        List<UUID> eventIds = allEvents.stream()
                .map(Event::getEventId)
                .collect(Collectors.toList());

        BulkCounts counts = fetchBulkCounts(eventIds);

        // Group events by status for the summary card (in-memory, no extra query)
        Map<EventStatus, Long> byStatus = allEvents.stream()
                .collect(Collectors.groupingBy(Event::getStatus, Collectors.counting()));

        // Build per-event summaries using pre-fetched count maps (O(1) lookups)
        Map<UUID, EventSummaryDto> summaries = new LinkedHashMap<>();
        long totalReg = 0;
        long totalAtt = 0;

        for (Event event : allEvents) {
            EventSummaryDto summary = buildEventSummary(event, counts);
            summaries.put(event.getEventId(), summary);
            totalReg += summary.getRegisteredCount();
            totalAtt += summary.getAttendedCount();
        }

        double overallRate = totalReg > 0 ? (totalAtt * 100.0 / totalReg) : 0.0;

        return ClubAdminDashboardDto.builder()
                .totalEvents(allEvents.size())
                .eventsByStatus(byStatus)
                .totalRegistrations(totalReg)
                .totalAttendance(totalAtt)
                .overallAttendanceRate(Math.round(overallRate * 10.0) / 10.0)
                .perEventSummary(summaries)
                .build();
    }

    // ── College Admin Dashboard ──────────────────────────────────────────────────

    /**
     * Same N+1 fix applied at college scope. Previously produced 2*N queries
     * for N events across all clubs in the college. Now uses the same two bulk
     * GROUP BY queries regardless of event count.
     *
     * Total DB round-trips: 6
     *   1. findById (college)
     *   2. countByRole (students)
     *   3. countByRole (club admins)
     *   4. countByCollege (clubs)
     *   5. findAllByCollege_CollegeId (events)
     *   6. countByEventIds (reg)      ─┐ encapsulated
     *   7. countByEventIds (att)      ─┘ in fetchBulkCounts()
     */
    public CollegeAdminDashboardDto getCollegeAdminDashboard(UUID collegeId) {
        College college = collegeRepository.findById(collegeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "College not found"));

        // Scalar count queries — these are already fine (single round-trip each)
        long totalStudents   = userRepository.countByCollege_CollegeIdAndRole(collegeId, UserRole.STUDENT);
        long totalClubAdmins = userRepository.countByCollege_CollegeIdAndRole(collegeId, UserRole.CLUB_ADMIN);
        long totalClubs      = clubRepository.countByCollege_CollegeId(collegeId);

        // Load all events for this college
        List<Event> allEvents = eventRepository.findAllByCollege_CollegeId(collegeId);

        // Two bulk GROUP BY queries for all events in the college
        List<UUID> eventIds = allEvents.stream()
                .map(Event::getEventId)
                .collect(Collectors.toList());

        BulkCounts counts = fetchBulkCounts(eventIds);

        // Build event summaries and accumulate college-wide totals (no extra queries)
        long totalReg = 0;
        long totalAtt = 0;
        List<EventSummaryDto> recentEvents = new ArrayList<>();

        for (Event event : allEvents) {
            EventSummaryDto summary = buildEventSummary(event, counts);
            recentEvents.add(summary);
            totalReg += summary.getRegisteredCount();
            totalAtt += summary.getAttendedCount();
        }

        return CollegeAdminDashboardDto.builder()
                .collegeName(college.getName())
                .totalStudents(totalStudents)
                .totalClubAdmins(totalClubAdmins)
                .totalClubs(totalClubs)
                .totalEvents(allEvents.size())
                .totalRegistrations(totalReg)
                .totalAttendance(totalAtt)
                .recentEvents(recentEvents)
                .build();
    }
    public SuperAdminDashboardDto getSuperAdminDashboard() {
        List<College> colleges = collegeRepository.findAll();

        List<CollegeSummaryDto> summaries = colleges.stream().map(college -> {
            UUID cid = college.getCollegeId();

            long users = userRepository.countByCollege_CollegeId(cid);
            long evts  = eventRepository.countByCollege_CollegeId(cid);
            long regs  = registrationRepository.countByEvent_College_CollegeId(cid);

            String primaryDomain = college.getDomains().stream()
                    .filter(CollegeDomain::isPrimary)
                    .map(CollegeDomain::getDomain)
                    .findFirst()
                    .orElse(null);

            return CollegeSummaryDto.builder()
                    .collegeId(cid)
                    .name(college.getName())
                    .primaryDomain(primaryDomain)
                    .isActive(college.isActive())
                    .totalUsers(users)
                    .totalEvents(evts)
                    .totalRegistrations(regs)
                    .build();
        }).collect(Collectors.toList());

        long totalUsers = summaries.stream().mapToLong(CollegeSummaryDto::getTotalUsers).sum();
        long totalEvts  = summaries.stream().mapToLong(CollegeSummaryDto::getTotalEvents).sum();

        return SuperAdminDashboardDto.builder()
                .totalColleges(colleges.size())
                .activeColleges((int) colleges.stream().filter(College::isActive).count())
                .inactiveColleges((int) colleges.stream().filter(c -> !c.isActive()).count())
                .totalUsersAcrossAllColleges(totalUsers)
                .totalEventsAcrossAllColleges(totalEvts)
                .colleges(summaries)
                .build();
    }
}