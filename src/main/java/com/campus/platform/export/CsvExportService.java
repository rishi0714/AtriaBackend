package com.campus.platform.export;

import com.campus.platform.attendance.entity.Attendance;
import com.campus.platform.attendance.repository.AttendanceRepository;
import com.campus.platform.event.entity.Event;
import com.campus.platform.event.service.EventService;
import com.campus.platform.registration.entity.Registration;
import com.campus.platform.registration.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Generates in-memory CSV byte arrays for download endpoints.
 * No temp files — everything is streamed into a ByteArrayOutputStream.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CsvExportService {

    private final RegistrationRepository registrationRepository;
    private final AttendanceRepository attendanceRepository;
    private final EventService eventService;

    // ── Participant list export ──────────────────────────────────────────────────

    /**
     * Exports all active (non-cancelled) registrations for an event as CSV.
     * Columns: Registration ID, Student Name, Email, Registered At, Attended
     */
    public byte[] exportParticipants(UUID eventId, UUID collegeId) {
        // Validates tenant scope
        eventService.findEventInTenantOrThrow(eventId, collegeId);

        List<Registration> registrations =
                registrationRepository.findAllByEvent_EventIdAndIsCancelledFalse(eventId);

        Set<UUID> attendedRegistrationIds = attendanceRepository.findAllByEventId(eventId)
                .stream()
                .map(a -> a.getRegistration().getRegistrationId())
                .collect(Collectors.toSet());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(out)) {
            writer.println("Registration ID,Student Name,Email,Registered At,Attended");
            for (Registration r : registrations) {
                writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                        r.getRegistrationId(),
                        escapeCsv(r.getUser().getFullName()),
                        r.getUser().getEmail(),
                        r.getRegisteredAt(),
                        attendedRegistrationIds.contains(r.getRegistrationId()) ? "Yes" : "No"
                );
            }
        }
        return out.toByteArray();
    }

    // ── Attendance report export ─────────────────────────────────────────────────

    /**
     * Exports attendance records for an event as CSV.
     * Columns: Attendance ID, Student Name, Email, Scanned By, Scanned At
     */
    public byte[] exportAttendance(UUID eventId, UUID collegeId) {
        eventService.findEventInTenantOrThrow(eventId, collegeId);

        List<Attendance> records = attendanceRepository.findAllByEventId(eventId);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(out)) {
            writer.println("Attendance ID,Student Name,Email,Scanned By,Scanned At");
            for (Attendance a : records) {
                writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                        a.getAttendanceId(),
                        escapeCsv(a.getRegistration().getUser().getFullName()),
                        a.getRegistration().getUser().getEmail(),
                        escapeCsv(a.getScannedBy().getFullName()),
                        a.getScannedAt()
                );
            }
        }
        return out.toByteArray();
    }

    // ── Helper ───────────────────────────────────────────────────────────────────

    /** Escapes double-quotes inside a CSV cell value. */
    private String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }
}
