package com.campus.platform.registration.service;

import com.campus.platform.attendance.repository.AttendanceRepository;
import com.campus.platform.common.enums.EventStatus;
import com.campus.platform.common.service.EmailService;
import com.campus.platform.event.entity.Event;
import com.campus.platform.event.service.EventService;
import com.campus.platform.registration.dto.RegistrationResponseDto;
import com.campus.platform.registration.entity.Registration;
import com.campus.platform.registration.mapper.RegistrationMapper;
import com.campus.platform.registration.repository.RegistrationRepository;
import com.campus.platform.user.entity.User;
import com.campus.platform.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final RegistrationMapper registrationMapper;
    private final UserService userService;
    private final EventService eventService;
    private final AttendanceRepository attendanceRepository;
    private final EmailService emailService;


    @Transactional
    public RegistrationResponseDto registerForEvent(UUID userId, UUID eventId) {
        User user = userService.findUserOrThrow(userId);
        Event event = eventService.findEventOrThrow(eventId);

        validateRegistrationEligibility(user, event);

        long currentCount = registrationRepository
                .countByEvent_EventIdAndIsCancelledFalse(eventId);
        if (currentCount >= event.getMaxCapacity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Event has reached maximum capacity.");
        }

        Registration registration = registrationRepository
                .findByUser_UserIdAndEvent_EventId(userId, eventId)
                .map(existing -> {
                    existing.setCancelled(false);
                    existing.setQrCode(UUID.randomUUID().toString());
                    existing.setRegisteredAt(LocalDateTime.now());
                    return existing;
                })
                .orElseGet(() -> Registration.builder()
                        .user(user)
                        .event(event)
                        .qrCode(UUID.randomUUID().toString())
                        .registeredAt(LocalDateTime.now())
                        .isCancelled(false)
                        .build());

        Registration saved = registrationRepository.save(registration);

        // Send email with QR image + .ics calendar attachment
        emailService.sendRegistrationEmail(saved);

        return registrationMapper.toResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public List<RegistrationResponseDto> getParticipantsForEvent(UUID eventId) {
        List<Registration> registrations = registrationRepository
                .findAllByEvent_EventIdAndIsCancelledFalse(eventId);

        Set<UUID> attendedIds = attendanceRepository
                .findRegistrationIdsByEvent(eventId); // ← single bulk query

        return registrations.stream()
                .map(r -> registrationMapper.toResponseDto(r,
                        attendedIds.contains(r.getRegistrationId())))
                .collect(Collectors.toList());
    }

    @Transactional
    public void cancelRegistration(UUID registrationId, UUID userId) {
        Registration registration = registrationRepository
                .findByRegistrationIdAndUser_UserId(registrationId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Registration not found or does not belong to this user."));

        if (registration.getEvent().getEventDate().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot cancel a registration after the event has occurred.");
        }

        registration.setCancelled(true);
        registrationRepository.save(registration);
    }

    // Add this method to RegistrationService.java
    @Transactional
    public void cancelByEventId(UUID eventId, UUID userId) {
        Registration registration = registrationRepository
                .findByUser_UserIdAndEvent_EventId(userId, eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Registration not found."));

        if (registration.getEvent().getEventDate().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot cancel after event has occurred.");
        }

        registration.setCancelled(true);
        registrationRepository.save(registration);
    }




    // ── Internal helpers ────────────────────────────────────────────────────────

    public Registration findByQrCodeOrThrow(String qrCode) {
        return registrationRepository.findByQrCode(qrCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No registration found for QR code: " + qrCode));
    }

    private void validateRegistrationEligibility(User user, Event event) {
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Event is not open for registration.");
        }
        if (event.getRegistrationDeadline().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Registration deadline has passed.");
        }

        // College check — guest or cross-college student can only register for open events
        boolean sameCollege = user.getCollege() != null &&
                user.getCollege().getCollegeId()
                        .equals(event.getCollege().getCollegeId());

        if (!event.isOpenToAll() && !sameCollege) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This event is only open to students of this college.");
        }

        boolean alreadyRegistered = registrationRepository
                .existsByUser_UserIdAndEvent_EventIdAndIsCancelledFalse(
                        user.getUserId(), event.getEventId());
        if (alreadyRegistered) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You are already registered for this event.");
        }
    }

    @Transactional(readOnly = true)
    public List<RegistrationResponseDto> getMyRegistrations(UUID userId) {
        List<Registration> registrations = registrationRepository
                .findAllByUser_UserIdAndIsCancelledFalse(userId);

        Set<UUID> attendedIds = attendanceRepository.findRegistrationIdsByUser(userId);

        return registrations.stream()
                .map(r -> registrationMapper.toResponseDto(r,
                        attendedIds.contains(r.getRegistrationId())))
                .collect(Collectors.toList());
    }
}