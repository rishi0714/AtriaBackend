package com.campus.platform.registration;

import com.campus.platform.college.entity.College;
import com.campus.platform.common.enums.EventStatus;
import com.campus.platform.common.enums.UserRole;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegistrationService")
class RegistrationServiceTest {

    @Mock RegistrationRepository registrationRepository;
    @Mock RegistrationMapper registrationMapper;
    @Mock UserService userService;
    @Mock EventService eventService;

    @InjectMocks RegistrationService registrationService;

    private UUID userId, eventId, collegeId;
    private User student;
    private Event event;
    private College college;

    @BeforeEach
    void setUp() {
        userId    = UUID.randomUUID();
        eventId   = UUID.randomUUID();
        collegeId = UUID.randomUUID();

        college = College.builder().collegeId(collegeId).name("Test College").build();

        student = User.builder()
                .userId(userId)
                .email("student@sreenidhi.edu.in")
                .fullName("Test Student")
                .role(UserRole.STUDENT)
                .college(college)
                .build();

        event = Event.builder()
                .eventId(eventId)
                .college(college)
                .title("Hackathon 2025")
                .status(EventStatus.PUBLISHED)
                .eventDate(LocalDateTime.now().plusDays(7))
                .registrationDeadline(LocalDateTime.now().plusDays(3))
                .maxCapacity(50)
                .build();
    }

    @Nested
    @DisplayName("registerForEvent()")
    class RegisterForEvent {

        @Test
        @DisplayName("saves registration and returns DTO on happy path")
        void success() {
            when(userService.findUserOrThrow(userId)).thenReturn(student);
            when(eventService.findEventInTenantOrThrow(eventId, collegeId)).thenReturn(event);
            when(registrationRepository.countActiveByEventId(eventId)).thenReturn(10L);
            when(registrationRepository.existsByUser_UserIdAndEvent_EventIdAndIsCancelledFalse(
                    userId, eventId)).thenReturn(false);

            Registration savedReg = Registration.builder()
                    .registrationId(UUID.randomUUID())
                    .user(student).event(event)
                    .qrCode(UUID.randomUUID().toString())
                    .registeredAt(LocalDateTime.now())
                    .build();

            when(registrationRepository.save(any())).thenReturn(savedReg);
            RegistrationResponseDto dto = new RegistrationResponseDto();
            when(registrationMapper.toResponseDto(savedReg)).thenReturn(dto);

            RegistrationResponseDto result =
                    registrationService.registerForEvent(userId, eventId, collegeId);

            assertThat(result).isNotNull();
            ArgumentCaptor<Registration> captor = ArgumentCaptor.forClass(Registration.class);
            verify(registrationRepository).save(captor.capture());
            assertThat(captor.getValue().getQrCode()).isNotBlank();
        }

        @Test
        @DisplayName("throws 409 when student already registered")
        void duplicateRegistration() {
            when(userService.findUserOrThrow(userId)).thenReturn(student);
            when(eventService.findEventInTenantOrThrow(eventId, collegeId)).thenReturn(event);
            when(registrationRepository.existsByUser_UserIdAndEvent_EventIdAndIsCancelledFalse(
                    userId, eventId)).thenReturn(true);

            assertThatThrownBy(() ->
                    registrationService.registerForEvent(userId, eventId, collegeId))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("already registered");

            verify(registrationRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws 409 when event is at full capacity")
        void fullCapacity() {
            when(userService.findUserOrThrow(userId)).thenReturn(student);
            when(eventService.findEventInTenantOrThrow(eventId, collegeId)).thenReturn(event);
            when(registrationRepository.existsByUser_UserIdAndEvent_EventIdAndIsCancelledFalse(
                    userId, eventId)).thenReturn(false);
            when(registrationRepository.countActiveByEventId(eventId)).thenReturn(50L); // at max

            assertThatThrownBy(() ->
                    registrationService.registerForEvent(userId, eventId, collegeId))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("maximum capacity");
        }

        @Test
        @DisplayName("throws 400 when event status is not PUBLISHED")
        void eventNotPublished() {
            event.setStatus(EventStatus.DRAFT);
            when(userService.findUserOrThrow(userId)).thenReturn(student);
            when(eventService.findEventInTenantOrThrow(eventId, collegeId)).thenReturn(event);

            assertThatThrownBy(() ->
                    registrationService.registerForEvent(userId, eventId, collegeId))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("not open for registration");
        }

        @Test
        @DisplayName("throws 400 when registration deadline has passed")
        void deadlinePassed() {
            event.setRegistrationDeadline(LocalDateTime.now().minusHours(1));
            when(userService.findUserOrThrow(userId)).thenReturn(student);
            when(eventService.findEventInTenantOrThrow(eventId, collegeId)).thenReturn(event);

            assertThatThrownBy(() ->
                    registrationService.registerForEvent(userId, eventId, collegeId))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("deadline has passed");
        }
    }
}
