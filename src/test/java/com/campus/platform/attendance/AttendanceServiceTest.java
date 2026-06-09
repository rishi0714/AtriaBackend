package com.campus.platform.attendance;

import com.campus.platform.attendance.dto.AttendanceDto;
import com.campus.platform.attendance.dto.AttendanceResponseDto;
import com.campus.platform.attendance.entity.Attendance;
import com.campus.platform.attendance.mapper.AttendanceMapper;
import com.campus.platform.attendance.repository.AttendanceRepository;
import com.campus.platform.attendance.service.AttendanceService;
import com.campus.platform.college.entity.College;
import com.campus.platform.common.enums.UserRole;
import com.campus.platform.event.entity.Event;
import com.campus.platform.registration.entity.Registration;
import com.campus.platform.registration.service.RegistrationService;
import com.campus.platform.user.entity.User;
import com.campus.platform.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AttendanceService")
class AttendanceServiceTest {

    @Mock AttendanceRepository attendanceRepository;
    @Mock AttendanceMapper attendanceMapper;
    @Mock RegistrationService registrationService;
    @Mock UserService userService;

    @InjectMocks AttendanceService attendanceService;

    private UUID collegeId, eventId, scannerUserId;
    private College college;
    private Event event;
    private User scanner;
    private Registration registration;
    private AttendanceDto dto;

    @BeforeEach
    void setUp() {
        collegeId     = UUID.randomUUID();
        eventId       = UUID.randomUUID();
        scannerUserId = UUID.randomUUID();

        college = College.builder().collegeId(collegeId).name("Test College").build();

        event = Event.builder()
                .eventId(eventId)
                .college(college)
                .title("Tech Fest")
                .build();

        scanner = User.builder()
                .userId(scannerUserId)
                .email("admin@sreenidhi.edu.in")
                .fullName("Club Admin")
                .role(UserRole.CLUB_ADMIN)
                .college(college)
                .build();

        User student = User.builder()
                .userId(UUID.randomUUID())
                .email("student@sreenidhi.edu.in")
                .fullName("Test Student")
                .college(college)
                .build();

        registration = Registration.builder()
                .registrationId(UUID.randomUUID())
                .user(student)
                .event(event)
                .qrCode("valid-qr-token")
                .registeredAt(LocalDateTime.now().minusDays(1))
                .isCancelled(false)
                .build();

        dto = AttendanceDto.builder()
                .qrCode("valid-qr-token")
                .eventId(eventId)
                .scannedById(scannerUserId)
                .build();
    }

    @Nested
    @DisplayName("scanQrCode()")
    class ScanQrCode {

        @Test
        @DisplayName("marks attendance on valid, first-time QR scan")
        void success() {
            when(registrationService.findByQrCodeOrThrow("valid-qr-token")).thenReturn(registration);
            when(attendanceRepository.existsByRegistration_RegistrationId(
                    registration.getRegistrationId())).thenReturn(false);
            when(userService.findUserOrThrow(scannerUserId)).thenReturn(scanner);

            Attendance saved = Attendance.builder()
                    .attendanceId(UUID.randomUUID())
                    .registration(registration)
                    .scannedBy(scanner)
                    .scannedAt(LocalDateTime.now())
                    .build();

            when(attendanceRepository.save(any())).thenReturn(saved);
            when(attendanceMapper.toResponseDto(saved)).thenReturn(new AttendanceResponseDto());

            AttendanceResponseDto result = attendanceService.scanQrCode(dto, collegeId);

            assertThat(result).isNotNull();
            verify(attendanceRepository).save(any(Attendance.class));
        }

        @Test
        @DisplayName("throws 409 on duplicate scan")
        void duplicateScan() {
            when(registrationService.findByQrCodeOrThrow("valid-qr-token")).thenReturn(registration);
            when(attendanceRepository.existsByRegistration_RegistrationId(
                    registration.getRegistrationId())).thenReturn(true); // already scanned

            assertThatThrownBy(() -> attendanceService.scanQrCode(dto, collegeId))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("already marked");

            verify(attendanceRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws 403 when QR belongs to a different college tenant")
        void crossTenantQr() {
            UUID otherCollegeId = UUID.randomUUID();

            assertThatThrownBy(() -> attendanceService.scanQrCode(dto, otherCollegeId))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(e -> {
                        ResponseStatusException rse = (ResponseStatusException) e;
                        assertThat(rse.getStatusCode().value()).isEqualTo(403);
                    });
        }

        @Test
        @DisplayName("throws 400 when QR is for a different event")
        void wrongEvent() {
            UUID differentEventId = UUID.randomUUID();
            dto.setEventId(differentEventId); // mismatch

            when(registrationService.findByQrCodeOrThrow("valid-qr-token")).thenReturn(registration);

            assertThatThrownBy(() -> attendanceService.scanQrCode(dto, collegeId))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("different event");
        }

        @Test
        @DisplayName("throws 400 when registration is cancelled")
        void cancelledRegistration() {
            registration.setCancelled(true);
            when(registrationService.findByQrCodeOrThrow("valid-qr-token")).thenReturn(registration);

            assertThatThrownBy(() -> attendanceService.scanQrCode(dto, collegeId))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("cancelled");
        }
    }
}
