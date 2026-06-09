package com.campus.platform.event.mapper;

import com.campus.platform.club.entity.Club;
import com.campus.platform.college.entity.College;
import com.campus.platform.common.enums.EventStatus;
import com.campus.platform.event.dto.EventDto;
import com.campus.platform.event.dto.EventResponseDto;
import com.campus.platform.event.entity.Event;
import org.springframework.stereotype.Component;

@Component
public class EventMapper {

    public Event toEntity(EventDto dto, Club club, College college) {
        return Event.builder()
                .club(club)
                .college(college)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .venue(dto.getVenue())
                .eventDate(dto.getEventDate())
                .registrationDeadline(dto.getRegistrationDeadline())
                .maxCapacity(dto.getMaxCapacity())
                .status(EventStatus.PENDING_APPROVAL) // ← changed from PUBLISHED
                .posterUrl(dto.getPosterUrl())
                .isOpenToAll(dto.isOpenToAll())
                .category(dto.getCategory())
                .build();
    }

    /** Full response with registeredCount — use when count is available (e.g. getEventById) */
    public EventResponseDto toResponseDto(Event event, int registeredCount) {
        return EventResponseDto.builder()
                .eventId(event.getEventId())
                .clubId(event.getClub().getClubId())
                .club(event.getClub().getName())
                .collegeId(event.getCollege().getCollegeId())
                .collegeName(event.getCollege().getName())
                .title(event.getTitle())
                .description(event.getDescription())
                .venue(event.getVenue())
                .date(event.getEventDate())
                .registrationDeadline(event.getRegistrationDeadline())
                .maxCapacity(event.getMaxCapacity())
                .registeredCount(registeredCount)
                .status(event.getStatus())
                .image(event.getPosterUrl())
                .category(event.getCategory())
                .isOpenToAll(event.isOpenToAll())
                .rejectionReason(event.getRejectionReason()) // ← added
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }

    /** List/page contexts — registeredCount defaults to 0 to avoid N+1 queries */
    public EventResponseDto toResponseDto(Event event) {
        return toResponseDto(event, 0);
    }

    public void updateEntityFromDto(EventDto dto, Event event, Club club) {
        event.setClub(club);
        event.setTitle(dto.getTitle());
        event.setDescription(dto.getDescription());
        event.setVenue(dto.getVenue());
        event.setEventDate(dto.getEventDate());
        event.setRegistrationDeadline(dto.getRegistrationDeadline());
        event.setMaxCapacity(dto.getMaxCapacity());
        event.setPosterUrl(dto.getPosterUrl());
        event.setCategory(dto.getCategory());
        // status and rejectionReason are never touched here — state-transition endpoints only
    }
}