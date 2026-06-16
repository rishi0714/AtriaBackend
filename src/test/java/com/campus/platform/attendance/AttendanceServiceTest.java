package com.campus.platform.attendance;

import com.campus.platform.attendance.dto.AttendanceDto;
import com.campus.platform.attendance.dto.AttendanceResponseDto;
import com.campus.platform.attendance.entity.Attendance;
import com.campus.platform.attendance.mapper.AttendanceMapper;
import com.campus.platform.attendance.repository.AttendanceRepository;
import com.campus.platform.attendance.service.AttendanceService;
import com.campus.platform.college.entity.College;
import com.campus.platform.common.enums.EventStatus;
import com.campus.platform.common.enums.UserRole;
import com.campus.platform.event.entity.Event;
import com.campus.platform.event.service.EventService;
import com.campus.platform.registration.entity.Registration;
import com.campus.platform.registration.repository.RegistrationRepository;
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

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private AttendanceMapper attendanceMapper;

    @Mock
    private RegistrationService registrationService;

    @Mock
    private UserService userService;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private EventService eventService;

    @InjectMocks
    private AttendanceService attendanceService;

    private UUID collegeId;
    private UUID eventId;
    private UUID scannerUserId;

    private College college;
    private Event event;
    private User scanner;
    private Registration registration;
    private AttendanceDto dto;

    @BeforeEach
    void setUp() {

        collegeId = UUID.randomUUID();
        eventId = UUID.randomUUID();
        scannerUserId = UUID.randomUUID();

        college = College.builder()
                .collegeId(collegeId)
                .name("Test College")
                .build();

        event = Event.builder()
                .eventId(eventId)
                .college(college)
                .title("Tech Fest")
                .status(EventStatus.PUBLISHED)
                .build();

        scanner = User.builder()
                .userId(scannerUserId)
                .email("admin@test.edu")
                .fullName("Club Admin")
                .role(UserRole.CLUB_ADMIN)
                .college(college)
                .build();

        User student = User.builder()
                .userId(UUID.randomUUID())
                .email("student@test.edu")
                .fullName("Student")
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
                .build();
    }

    @Nested
    @DisplayName("scanQrCode()")
    class ScanQrCode {

        @Test
        @DisplayName("marks attendance on valid first scan")
        void success() {

            when(registrationService.findByQrCodeOrThrow("valid-qr-token"))
                    .thenReturn(registration);

            when(attendanceRepository.existsByRegistration_RegistrationId(
                    registration.getRegistrationId()))
                    .thenReturn(false);

            when(userService.findUserOrThrow(scannerUserId))
                    .thenReturn(scanner);

            Attendance saved = Attendance.builder()
                    .attendanceId(UUID.randomUUID())
                    .registration(registration)
                    .scannedBy(scanner)
                    .scannedAt(LocalDateTime.now())
                    .build();

            when(attendanceRepository.save(any(Attendance.class)))
                    .thenReturn(saved);

            when(attendanceMapper.toResponseDto(saved))
                    .thenReturn(new AttendanceResponseDto());

            AttendanceResponseDto result =
                    attendanceService.scanQrCode(
                            dto,
                            collegeId,
                            scannerUserId);

            assertThat(result).isNotNull();

            verify(attendanceRepository)
                    .save(any(Attendance.class));
        }

        @Test
        @DisplayName("throws 409 when attendance already exists")
        void duplicateScan() {

            when(registrationService.findByQrCodeOrThrow("valid-qr-token"))
                    .thenReturn(registration);

            when(attendanceRepository.existsByRegistration_RegistrationId(
                    registration.getRegistrationId()))
                    .thenReturn(true);

            assertThatThrownBy(() ->
                    attendanceService.scanQrCode(
                            dto,
                            collegeId,
                            scannerUserId))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("already marked");

            verify(attendanceRepository, never())
                    .save(any());
        }

        @Test
        @DisplayName("throws 403 for cross tenant QR")
        void crossTenantQr() {

            UUID otherCollegeId = UUID.randomUUID();

            when(registrationService.findByQrCodeOrThrow("valid-qr-token"))
                    .thenReturn(registration);

            assertThatThrownBy(() ->
                    attendanceService.scanQrCode(
                            dto,
                            otherCollegeId,
                            scannerUserId))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse =
                                (ResponseStatusException) ex;

                        assertThat(rse.getStatusCode().value())
                                .isEqualTo(403);
                    });
        }

        @Test
        @DisplayName("throws 400 when QR belongs to another event")
        void wrongEvent() {

            dto.setEventId(UUID.randomUUID());

            when(registrationService.findByQrCodeOrThrow("valid-qr-token"))
                    .thenReturn(registration);

            assertThatThrownBy(() ->
                    attendanceService.scanQrCode(
                            dto,
                            collegeId,
                            scannerUserId))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("different event");
        }

        @Test
        @DisplayName("throws 400 when registration is cancelled")
        void cancelledRegistration() {

            registration.setCancelled(true);

            when(registrationService.findByQrCodeOrThrow("valid-qr-token"))
                    .thenReturn(registration);

            assertThatThrownBy(() ->
                    attendanceService.scanQrCode(
                            dto,
                            collegeId,
                            scannerUserId))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("cancelled");
        }

        @Test
        @DisplayName("throws 400 when event is pending approval")
        void pendingApprovalEvent() {

            event.setStatus(EventStatus.PENDING_APPROVAL);

            when(registrationService.findByQrCodeOrThrow("valid-qr-token"))
                    .thenReturn(registration);

            assertThatThrownBy(() ->
                    attendanceService.scanQrCode(
                            dto,
                            collegeId,
                            scannerUserId))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("not published");
        }

        @Test
        @DisplayName("throws 400 when event is rejected")
        void rejectedEvent() {

            event.setStatus(EventStatus.REJECTED);

            when(registrationService.findByQrCodeOrThrow("valid-qr-token"))
                    .thenReturn(registration);

            assertThatThrownBy(() ->
                    attendanceService.scanQrCode(
                            dto,
                            collegeId,
                            scannerUserId))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("not published");
        }
    }
}