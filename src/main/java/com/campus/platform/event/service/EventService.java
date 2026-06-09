package com.campus.platform.event.service;

import com.campus.platform.club.entity.Club;
import com.campus.platform.club.service.ClubService;
import com.campus.platform.college.entity.College;
import com.campus.platform.college.service.CollegeService;
import com.campus.platform.common.enums.EventStatus;
import com.campus.platform.event.dto.EventDto;
import com.campus.platform.event.dto.EventResponseDto;
import com.campus.platform.event.entity.Event;
import com.campus.platform.event.mapper.EventMapper;
import com.campus.platform.event.repository.EventRepository;
import com.campus.platform.registration.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import com.campus.platform.user.entity.User;
import com.campus.platform.user.service.UserService;
import com.campus.platform.common.util.SecurityContextUtil;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final CollegeService collegeService;
    private final ClubService clubService;
    private final RegistrationRepository registrationRepository;
    private final UserService userService;


    // ── Club Admin ───────────────────────────────────────────────────────────────

    @Transactional
    public EventResponseDto createEvent(UUID collegeId, EventDto dto) {
        College college = collegeService.findCollegeOrThrow(collegeId);
        Club club = clubService.findClubInTenantOrThrow(dto.getClubId(), collegeId);
        if (!dto.getRegistrationDeadline().isBefore(dto.getEventDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Registration deadline must be before the event date.");
        }
        Event event = eventMapper.toEntity(dto, club, college);
        // status defaults to PENDING_APPROVAL via toEntity — no explicit set needed
        return eventMapper.toResponseDto(eventRepository.save(event));
    }

    @Transactional
    public EventResponseDto createEventForManager(UUID userId, UUID collegeId, EventDto dto) {
        Club club = clubService.findClubByManagerOrThrow(userId);
        College college = collegeService.findCollegeOrThrow(collegeId);
        if (!dto.getRegistrationDeadline().isBefore(dto.getEventDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Registration deadline must be before the event date.");
        }
        Event event = eventMapper.toEntity(dto, club, college);
        return eventMapper.toResponseDto(eventRepository.save(event));
    }

    @Transactional(readOnly = true)
    public Page<EventResponseDto> getVisibleEvents(Pageable pageable) {
        UUID userId = SecurityContextUtil.currentUserId();
        User user = userService.findUserOrThrow(userId);

        if (user.getCollege() != null) {
            // College student — own college published events + all open published events
            return eventRepository
                    .findVisibleEventsForCollege(
                            user.getCollege().getCollegeId(),
                            EventStatus.PUBLISHED,
                            pageable)
                    .map(eventMapper::toResponseDto);
        } else {
            // Guest student — only open published events
            return eventRepository
                    .findAllByIsOpenToAllTrueAndStatus(
                            EventStatus.PUBLISHED,
                            pageable)
                    .map(eventMapper::toResponseDto);
        }
    }

    @Transactional(readOnly = true)
    public Event findEventOrThrow(UUID eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Event not found."));
    }

    @Transactional(readOnly = true)
    public List<EventResponseDto> getEventsByManager(UUID userId, UUID collegeId) {
        Club club = clubService.findClubByManagerOrThrow(userId);
        return eventRepository.findAllByClub_ClubId(club.getClubId())
                .stream()
                .map(eventMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public EventResponseDto updateEvent(UUID eventId, UUID collegeId, EventDto dto) {
        Event event = findEventInTenantOrThrow(eventId, collegeId);

        if (event.getStatus() == EventStatus.COMPLETED
                || event.getStatus() == EventStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Completed or cancelled events cannot be edited.");
        }

        Club club = (dto.getClubId() != null)
                ? clubService.findClubInTenantOrThrow(dto.getClubId(), collegeId)
                : event.getClub();

        eventMapper.updateEntityFromDto(dto, event, club);
        return eventMapper.toResponseDto(eventRepository.save(event));
    }

    @Transactional
    public EventResponseDto resubmitEvent(UUID eventId, UUID collegeId) {
        Event event = findEventInTenantOrThrow(eventId, collegeId);
        if (event.getStatus() != EventStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only rejected events can be resubmitted.");
        }
        event.setStatus(EventStatus.PENDING_APPROVAL);
        event.setRejectionReason(null); // clear previous reason
        return eventMapper.toResponseDto(eventRepository.save(event));
    }

    @Transactional
    public void deleteEvent(UUID eventId, UUID collegeId) {
        Event event = findEventInTenantOrThrow(eventId, collegeId);
        eventRepository.delete(event);
    }

    @Transactional
    public EventResponseDto updateStatus(UUID eventId, UUID collegeId, EventStatus newStatus) {
        Event event = findEventInTenantOrThrow(eventId, collegeId);

        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A cancelled event cannot be updated.");
        }
        if (newStatus == EventStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Use the delete endpoint to cancel an event.");
        }
        // Approval-flow statuses are never set manually
        if (newStatus == EventStatus.PUBLISHED
                || newStatus == EventStatus.PENDING_APPROVAL
                || newStatus == EventStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Status '" + newStatus + "' cannot be set manually.");
        }
        // REGISTRATION_CLOSED and COMPLETED only make sense on a live event
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Status can only be updated on a published event.");
        }

        event.setStatus(newStatus);
        return eventMapper.toResponseDto(eventRepository.save(event));
    }

    // ── College Admin ────────────────────────────────────────────────────────────

    @Transactional
    public EventResponseDto approveEvent(UUID eventId, UUID collegeId) {
        Event event = findEventInTenantOrThrow(eventId, collegeId);
        if (event.getStatus() != EventStatus.PENDING_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only events pending approval can be approved.");
        }
        event.setStatus(EventStatus.PUBLISHED); // approve and publish atomically
        event.setRejectionReason(null);
        return eventMapper.toResponseDto(eventRepository.save(event));
    }

    @Transactional
    public EventResponseDto rejectEvent(UUID eventId, UUID collegeId, String reason) {
        Event event = findEventInTenantOrThrow(eventId, collegeId);
        if (event.getStatus() != EventStatus.PENDING_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only events pending approval can be rejected.");
        }
        event.setStatus(EventStatus.REJECTED);
        event.setRejectionReason(reason);
        return eventMapper.toResponseDto(eventRepository.save(event));
    }

    // ── Shared reads ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<EventResponseDto> getPublishedEvents(UUID collegeId, Pageable pageable) {
        return eventRepository
                .findAllByCollege_CollegeIdAndStatus(collegeId, EventStatus.PUBLISHED, pageable)
                .map(eventMapper::toResponseDto);
    }

    @Transactional(readOnly = true)
    public Page<EventResponseDto> searchEvents(UUID collegeId, String keyword, Pageable pageable) {
        return eventRepository.searchByKeyword(collegeId, keyword, pageable)
                .map(eventMapper::toResponseDto);
    }

    @Transactional(readOnly = true)
    public EventResponseDto getEventById(UUID eventId, UUID collegeId) {
        Event event = findEventInTenantOrThrow(eventId, collegeId);
        int count = (int) registrationRepository.countByEvent_EventIdAndIsCancelledFalse(eventId);
        return eventMapper.toResponseDto(event, count);
    }

    @Transactional(readOnly = true)
    public List<EventResponseDto> getEventsByCollege(UUID collegeId) {
        // Returns ALL statuses — college admin sees everything including PENDING_APPROVAL
        return eventRepository.findAllByCollege_CollegeId(collegeId)
                .stream()
                .map(eventMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EventResponseDto> getEventsByClub(UUID clubId, UUID collegeId) {
        // Club admin sees their own events in all statuses including PENDING_APPROVAL + REJECTED
        clubService.findClubInTenantOrThrow(clubId, collegeId); // tenant guard
        return eventRepository.findAllByClub_ClubId(clubId)
                .stream()
                .map(eventMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    // ── Internal helpers ─────────────────────────────────────────────────────────

    public Event findEventInTenantOrThrow(UUID eventId, UUID collegeId) {
        return eventRepository.findByEventIdAndCollege_CollegeId(eventId, collegeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Event not found or does not belong to this college."));
    }
}