package com.campus.platform.registration;

import com.campus.platform.attendance.repository.AttendanceRepository;
import com.campus.platform.college.entity.College;
import com.campus.platform.common.enums.EventStatus;
import com.campus.platform.common.enums.UserRole;
import com.campus.platform.common.service.EmailService;
import com.campus.platform.event.entity.Event;
import com.campus.platform.event.service.EventService;
import com.campus.platform.registration.dto.RegistrationResponseDto;
import com.campus.platform.registration.entity.Registration;
import com.campus.platform.registration.mapper.RegistrationMapper;
import com.campus.platform.registration.repository.RegistrationRepository;
import com.campus.platform.registration.service.RegistrationService;
import com.campus.platform.user.entity.User;
import com.campus.platform.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegistrationService")
class RegistrationServiceTest {

    @Mock RegistrationRepository registrationRepository;
    @Mock RegistrationMapper registrationMapper;
    @Mock UserService userService;
    @Mock EventService eventService;
    @Mock AttendanceRepository attendanceRepository;
    @Mock EmailService emailService;

    // ← real fixed clock instead of @Mock
    private final Clock clock = Clock.fixed(Instant.now(), ZoneOffset.UTC);

    private RegistrationService registrationService;

    private UUID userId;
    private UUID eventId;

    private User student;
    private Event event;
    private College college;

    @BeforeEach
    void setUp() {
        // ← manually construct so the real clock is injected
        registrationService = new RegistrationService(
                registrationRepository,
                registrationMapper,
                userService,
                eventService,
                attendanceRepository,
                emailService,
                clock
        );

        userId  = UUID.randomUUID();
        eventId = UUID.randomUUID();

        college = College.builder()
                .collegeId(UUID.randomUUID())
                .name("Test College")
                .build();

        student = User.builder()
                .userId(userId)
                .email("student@test.edu")
                .fullName("Test Student")
                .role(UserRole.STUDENT)
                .college(college)
                .build();

        event = Event.builder()
                .eventId(eventId)
                .college(college)
                .title("Hackathon")
                .status(EventStatus.PUBLISHED)
                .eventDate(LocalDateTime.now(clock).plusDays(10))         // ← fixed
                .registrationDeadline(LocalDateTime.now(clock).plusDays(5)) // ← fixed
                .maxCapacity(50)
                .isOpenToAll(false)
                .build();
    }

    @Nested
    @DisplayName("registerForEvent()")
    class RegisterForEvent {

        @Test
        @DisplayName("registers successfully")
        void success() {

            when(userService.findUserOrThrow(userId))
                    .thenReturn(student);

            when(eventService.findEventOrThrow(eventId))
                    .thenReturn(event);

            when(registrationRepository
                    .existsByUser_UserIdAndEvent_EventIdAndIsCancelledFalse(
                            userId, eventId))
                    .thenReturn(false);

            when(registrationRepository
                    .countByEvent_EventIdAndIsCancelledFalse(eventId))
                    .thenReturn(10L);

            Registration saved = Registration.builder()
                    .registrationId(UUID.randomUUID())
                    .user(student)
                    .event(event)
                    .qrCode(UUID.randomUUID().toString())
                    .registeredAt(LocalDateTime.now(clock))  // ← fixed
                    .isCancelled(false)
                    .build();

            when(registrationRepository.save(any()))
                    .thenReturn(saved);

            when(registrationMapper.toResponseDto(saved))
                    .thenReturn(new RegistrationResponseDto());

            RegistrationResponseDto result =
                    registrationService.registerForEvent(userId, eventId);

            assertThat(result).isNotNull();

            ArgumentCaptor<Registration> captor =
                    ArgumentCaptor.forClass(Registration.class);

            verify(registrationRepository).save(captor.capture());

            assertThat(captor.getValue().getQrCode()).isNotBlank();

            verify(emailService).sendRegistrationEmail(any(UUID.class));
        }

        @Test
        @DisplayName("throws 409 when already registered")
        void duplicateRegistration() {

            when(userService.findUserOrThrow(userId))
                    .thenReturn(student);

            when(eventService.findEventOrThrow(eventId))
                    .thenReturn(event);

            when(registrationRepository
                    .existsByUser_UserIdAndEvent_EventIdAndIsCancelledFalse(
                            userId, eventId))
                    .thenReturn(true);

            assertThatThrownBy(() ->
                    registrationService.registerForEvent(userId, eventId))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("already registered");

            verify(registrationRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws 409 when event capacity reached")
        void fullCapacity() {

            when(userService.findUserOrThrow(userId))
                    .thenReturn(student);

            when(eventService.findEventOrThrow(eventId))
                    .thenReturn(event);

            when(registrationRepository
                    .existsByUser_UserIdAndEvent_EventIdAndIsCancelledFalse(
                            userId, eventId))
                    .thenReturn(false);

            when(registrationRepository
                    .countByEvent_EventIdAndIsCancelledFalse(eventId))
                    .thenReturn(50L);

            assertThatThrownBy(() ->
                    registrationService.registerForEvent(userId, eventId))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("maximum capacity");
        }

        @Test
        @DisplayName("throws 400 when event not published")
        void eventNotPublished() {

            event.setStatus(EventStatus.REJECTED);

            when(userService.findUserOrThrow(userId))
                    .thenReturn(student);

            when(eventService.findEventOrThrow(eventId))
                    .thenReturn(event);

            assertThatThrownBy(() ->
                    registrationService.registerForEvent(userId, eventId))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("not open for registration");
        }

        @Test
        @DisplayName("throws 400 when deadline passed")
        void deadlinePassed() {

            event.setRegistrationDeadline(
                    LocalDateTime.now(clock).minusHours(1));  // ← fixed

            when(userService.findUserOrThrow(userId))
                    .thenReturn(student);

            when(eventService.findEventOrThrow(eventId))
                    .thenReturn(event);

            assertThatThrownBy(() ->
                    registrationService.registerForEvent(userId, eventId))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("deadline has passed");
        }
    }
}