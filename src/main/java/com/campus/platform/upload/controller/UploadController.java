package com.campus.platform.upload.controller;

import com.campus.platform.upload.service.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    @PostMapping("/college-logo")
    @PreAuthorize("hasRole('PLATFORM_OWNER')")
    public ResponseEntity<Map<String, String>> uploadCollegeLogo(
            @RequestParam("file") MultipartFile file) {
        String url = uploadService.upload(file, "college-logos");
        return ResponseEntity.ok(Map.of("url", url));
    }

    @PostMapping("/club-logo")
    @PreAuthorize("hasAnyRole('PLATFORM_OWNER', 'COLLEGE_ADMIN')")
    public ResponseEntity<Map<String, String>> uploadClubLogo(
            @RequestParam("file") MultipartFile file) {
        String url = uploadService.upload(file, "club-logos");
        return ResponseEntity.ok(Map.of("url", url));
    }

    @GetMapping("/debug-auth")
    public ResponseEntity<Map<String, Object>> debugAuth() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(Map.of(
                "principal", auth.getPrincipal().toString(),
                "authorities", auth.getAuthorities().toString(),
                "class", auth.getClass().getSimpleName()
        ));
    }

    @PostMapping("/event-poster")
    @PreAuthorize("hasRole('CLUB_ADMIN')")
    public ResponseEntity<Map<String, String>> uploadEventPoster(
            @RequestParam("file") MultipartFile file) {
        String url = uploadService.upload(file, "event-posters");
        return ResponseEntity.ok(Map.of("url", url));
    }
}