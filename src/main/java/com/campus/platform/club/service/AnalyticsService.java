package com.campus.platform.club.service;

import com.campus.platform.attendance.repository.AttendanceRepository;
import com.campus.platform.club.dto.*;
import com.campus.platform.club.entity.Club;
import com.campus.platform.club.repository.ClubRepository;
import com.campus.platform.event.entity.Event;
import com.campus.platform.event.repository.EventRepository;
import com.campus.platform.registration.entity.Registration;
import com.campus.platform.registration.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ClubRepository clubRepository;
    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final AttendanceRepository attendanceRepository;

    private static final List<String> COLORS = List.of(
            "bg-indigo-500", "bg-pink-500", "bg-amber-500",
            "bg-emerald-500", "bg-violet-500"
    );

    public ClubAnalyticsDto getClubAnalytics(UUID userId) {
        Club club = clubRepository.findByManagedBy_UserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Club not found"));

        List<Event> events = eventRepository.findAllByClub_ClubId(club.getClubId());

        List<UUID> eventIds = events.stream()
                .map(Event::getEventId)
                .collect(Collectors.toList());

        // ── Bulk fetch 1: all registrations for all events in one query ──────────
        // Grouped by eventId so each buildEventStats call can do a map lookup
        // instead of hitting the DB again.
        Map<UUID, List<Registration>> regsByEvent = eventIds.isEmpty()
                ? Map.of()
                : registrationRepository.findAllByEvent_EventIdInAndIsCancelledFalse(eventIds)
                .stream()
                .collect(Collectors.groupingBy(r -> r.getEvent().getEventId()));

        // ── Bulk fetch 2: attendance counts for all events in one GROUP BY query ─
        // Same pattern as DashboardService.fetchBulkCounts().
        Map<UUID, Long> attCounts = eventIds.isEmpty()
                ? Map.of()
                : attendanceRepository.countByEventIds(eventIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (Long)  row[1]
                ));

        // Build the event selector list shown in the UI dropdown
        List<EventInfo> eventsList = events.stream()
                .map(e -> {
                    EventInfo info = new EventInfo();
                    info.setId(e.getEventId().toString());
                    info.setName(e.getTitle());
                    return info;
                }).toList();

        // Build stats map: "all" entry first, then one entry per event.
        // All DB data is already in memory — no further queries inside these methods.
        Map<String, StatsDetail> stats = new LinkedHashMap<>();
        stats.put("all", buildOverallStats(events, regsByEvent, attCounts));
        for (Event event : events) {
            List<Registration> regs = regsByEvent.getOrDefault(event.getEventId(), List.of());
            long attended          = attCounts.getOrDefault(event.getEventId(), 0L);
            stats.put(event.getEventId().toString(), buildEventStats(event, regs, attended));
        }

        ClubAnalyticsDto dto = new ClubAnalyticsDto();
        dto.setClubId(club.getClubId().toString());
        dto.setName(club.getName());
        dto.setEventsList(eventsList);
        dto.setStats(stats);
        return dto;
    }

    /**
     * Aggregates registration and attendance data across ALL events for the club.
     *
     * Previously this called findAllByEvent + countByEventId inside a loop,
     * producing 2*N queries. Now it receives pre-fetched maps and does pure
     * in-memory aggregation — zero extra DB round-trips.
     */
    private StatsDetail buildOverallStats(List<Event> events,
                                          Map<UUID, List<Registration>> regsByEvent,
                                          Map<UUID, Long> attCounts) {
        long totalRegistrations = 0;
        long totalAttended      = 0;

        Map<String, Integer> categoryCount = new LinkedHashMap<>();
        Map<String, Integer> yearCount     = new LinkedHashMap<>();

        for (Event event : events) {
            List<Registration> registrations = regsByEvent.getOrDefault(event.getEventId(), List.of());
            long attended                    = attCounts.getOrDefault(event.getEventId(), 0L);

            totalRegistrations += registrations.size();
            totalAttended      += attended;

            // Tally stream/year breakdowns from the already-loaded registration rows
            for (Registration reg : registrations) {
                String stream = reg.getUser().getStream();
                String year = (reg.getUser().getYear() != null && reg.getUser().getYear() > 0)
                        ? "Year " + reg.getUser().getYear()
                        : "Unknown";
                categoryCount.merge(stream != null ? stream : "Other", 1, Integer::sum);
                yearCount.merge(year, 1, Integer::sum); // ← keep only this one
            }
        }

        double attendanceRate = totalRegistrations > 0
                ? (totalAttended * 100.0 / totalRegistrations) : 0;

        List<KpiData> kpis = List.of(
                kpi("Total Registrations", String.valueOf(totalRegistrations), "up", "Across all events"),
                kpi("Average Attendance", String.format("%.1f%%", attendanceRate), "up", "Verified via QR code")
        );

        return buildStatsDetail(kpis, categoryCount, yearCount, totalRegistrations);
    }

    /**
     * Builds stats for a single event.
     *
     * Previously fetched registrations and attendance from the DB here.
     * Now receives pre-fetched data so this is pure computation — zero DB calls.
     */
    private StatsDetail buildEventStats(Event event,
                                        List<Registration> registrations,
                                        long attended) {
        long regs  = registrations.size();
        double rate = regs > 0 ? (attended * 100.0 / regs) : 0;

        Map<String, Integer> categoryCount = new LinkedHashMap<>();
        Map<String, Integer> yearCount     = new LinkedHashMap<>();

        for (Registration reg : registrations) {
            String stream = reg.getUser().getStream();
            String year = (reg.getUser().getYear() != null && reg.getUser().getYear() > 0)
                    ? "Year " + reg.getUser().getYear()
                    : "Unknown";
            categoryCount.merge(stream != null ? stream : "Other", 1, Integer::sum);
            yearCount.merge(year, 1, Integer::sum); // ← keep only this one
        }

        List<KpiData> kpis = List.of(
                kpi("Registrations", String.valueOf(regs), "up",
                        "Capacity: " + event.getMaxCapacity()),
                kpi("Attendance Rate", String.format("%.1f%%", rate),
                        rate >= 50 ? "up" : "down", "Verified via QR code")
        );

        return buildStatsDetail(kpis, categoryCount, yearCount, regs);
    }

    // ── Unchanged helpers ────────────────────────────────────────────────────────

    private StatsDetail buildStatsDetail(List<KpiData> kpis,
                                         Map<String, Integer> categoryCount,
                                         Map<String, Integer> yearCount,
                                         long total) {
        List<CategoryData> categories = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, Integer> entry : categoryCount.entrySet()) {
            CategoryData c = new CategoryData();
            c.setCategory(entry.getKey());
            c.setRegistrations(entry.getValue());
            c.setPercentage(total > 0 ? (int)(entry.getValue() * 100L / total) : 0);
            c.setColor(COLORS.get(i++ % COLORS.size()));
            categories.add(c);
        }

        List<YearData> years = new ArrayList<>();
        int j = 0;
        for (Map.Entry<String, Integer> entry : yearCount.entrySet()) {
            YearData y = new YearData();
            y.setYear(entry.getKey());
            y.setCount(entry.getValue());
            y.setPercentage(total > 0 ? (int)(entry.getValue() * 100L / total) : 0);
            y.setColor(COLORS.get(j++ % COLORS.size()));
            years.add(y);
        }

        StatsDetail detail = new StatsDetail();
        detail.setKpis(kpis);
        detail.setCategories(categories);
        detail.setYears(years);
        return detail;
    }

    private KpiData kpi(String title, String value, String trend, String description) {
        KpiData k = new KpiData();
        k.setTitle(title);
        k.setValue(value);
        k.setChange("");
        k.setTrend(trend);
        k.setDescription(description);
        return k;
    }
}