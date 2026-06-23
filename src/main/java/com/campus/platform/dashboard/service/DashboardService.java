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

import java.time.Clock;
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
    private final Clock clock;

    // ── Shared helper ────────────────────────────────────────────────────────────

    private BulkCounts fetchBulkCounts(List<UUID> eventIds) {
        if (eventIds.isEmpty()) {
            return new BulkCounts(Map.of(), Map.of());
        }

        Map<UUID, Long> regCounts = registrationRepository
                .countByEventIds(eventIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (Long)  row[1]
                ));

        Map<UUID, Long> attCounts = attendanceRepository
                .countByEventIds(eventIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (Long)  row[1]
                ));

        return new BulkCounts(regCounts, attCounts);
    }

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

    private record BulkCounts(Map<UUID, Long> reg, Map<UUID, Long> att) {}

    // ── Student Dashboard ────────────────────────────────────────────────────────

    public StudentDashboardDto getStudentDashboard(UUID userId) {
        List<Registration> active = registrationRepository
                .findAllByUser_UserIdAndIsCancelledFalse(userId);

        LocalDateTime now = LocalDateTime.now(clock);  // ← fixed

        List<RegistrationResponseDto> upcoming = active.stream()
                .filter(r -> r.getEvent().getEventDate().isAfter(now))
                .map(registrationMapper::toResponseDto)
                .collect(Collectors.toList());

        List<RegistrationResponseDto> past = active.stream()
                .filter(r -> r.getEvent().getEventDate().isBefore(now))
                .map(registrationMapper::toResponseDto)
                .collect(Collectors.toList());

        long attended = attendanceRepository.countByRegistration_User_UserId(userId);

        return StudentDashboardDto.builder()
                .totalRegistrations(active.size())
                .totalAttended((int) attended)
                .upcomingEvents(upcoming)
                .pastEvents(past)
                .build();
    }

    // ── Club Admin Dashboard ─────────────────────────────────────────────────────

    public ClubAdminDashboardDto getClubAdminDashboard(UUID collegeId) {
        UUID userId = SecurityContextUtil.currentUserId();

        Club club = clubRepository.findByManagedBy_UserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Club not found"));

        List<Event> allEvents = eventRepository.findAllByClub_ClubId(club.getClubId());

        List<UUID> eventIds = allEvents.stream()
                .map(Event::getEventId)
                .collect(Collectors.toList());

        BulkCounts counts = fetchBulkCounts(eventIds);

        Map<EventStatus, Long> byStatus = allEvents.stream()
                .collect(Collectors.groupingBy(Event::getStatus, Collectors.counting()));

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

    public CollegeAdminDashboardDto getCollegeAdminDashboard(UUID collegeId) {
        College college = collegeRepository.findById(collegeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "College not found"));

        long totalStudents   = userRepository.countByCollege_CollegeIdAndRole(collegeId, UserRole.STUDENT);
        long totalClubAdmins = userRepository.countByCollege_CollegeIdAndRole(collegeId, UserRole.CLUB_ADMIN);
        long totalClubs      = clubRepository.countByCollege_CollegeId(collegeId);

        List<Event> allEvents = eventRepository.findAllByCollege_CollegeId(collegeId);

        List<UUID> eventIds = allEvents.stream()
                .map(Event::getEventId)
                .collect(Collectors.toList());

        BulkCounts counts = fetchBulkCounts(eventIds);

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

    // ── Super Admin Dashboard ────────────────────────────────────────────────────

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