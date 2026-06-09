package com.campus.platform.export;

import com.campus.platform.common.util.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/club/export")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLUB_ADMIN')")
public class CsvExportController {

    private final CsvExportService csvExportService;

    @GetMapping("/events/{eventId}/participants")
    public ResponseEntity<byte[]> downloadParticipants(@PathVariable UUID eventId) {
        UUID collegeId = SecurityContextUtil.currentCollegeId();
        byte[] csv = csvExportService.exportParticipants(eventId, collegeId);
        return csvResponse(csv, "participants-" + eventId + ".csv");
    }

    @GetMapping("/events/{eventId}/attendance")
    public ResponseEntity<byte[]> downloadAttendance(@PathVariable UUID eventId) {
        UUID collegeId = SecurityContextUtil.currentCollegeId();
        byte[] csv = csvExportService.exportAttendance(eventId, collegeId);
        return csvResponse(csv, "attendance-" + eventId + ".csv");
    }

    // ── Helper ───────────────────────────────────────────────────────────────────

    private ResponseEntity<byte[]> csvResponse(byte[] data, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(filename).build());
        headers.setContentLength(data.length);
        return ResponseEntity.ok().headers(headers).body(data);
    }
}
