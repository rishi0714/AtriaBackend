package com.campus.platform.event;

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
import com.campus.platform.event.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventService")
class EventServiceTest {

    @Mock EventRepository eventRepository;
    @Mock EventMapper eventMapper;
    @Mock CollegeService collegeService;
    @Mock ClubService clubService;

    @InjectMocks EventService eventService;

    private UUID collegeId, clubId, eventId;
    private College college;
    private Club club;
    private Event event;
    private EventDto dto;

    @BeforeEach
    void setUp() {
        collegeId = UUID.randomUUID();
        clubId    = UUID.randomUUID();
        eventId   = UUID.randomUUID();

        college = College.builder().collegeId(collegeId).name("Test College").build();
        club    = Club.builder().clubId(clubId).college(college).name("Coding Club").build();

        event = Event.builder()
                .eventId(eventId)
                .college(college)
                .club(club)
                .title("Hackathon")
                .status(EventStatus.PUBLISHED)
                .eventDate(LocalDateTime.now().plusDays(5))
                .registrationDeadline(LocalDateTime.now().plusDays(2))
                .maxCapacity(100)
                .build();

        dto = EventDto.builder()
                .clubId(clubId)
                .title("Hackathon")
                .venue("Main Hall")
                .eventDate(LocalDateTime.now().plusDays(5))
                .registrationDeadline(LocalDateTime.now().plusDays(2))
                .maxCapacity(100)
                .build();
    }

    @Test
    @DisplayName("createEvent() saves and returns mapped DTO")
    void createEvent() {
        when(collegeService.findCollegeOrThrow(collegeId)).thenReturn(college);
        when(clubService.findClubInTenantOrThrow(clubId, collegeId)).thenReturn(club);
        when(eventMapper.toEntity(dto, club, college)).thenReturn(event);
        when(eventRepository.save(event)).thenReturn(event);
        EventResponseDto expected = new EventResponseDto();
        when(eventMapper.toResponseDto(event)).thenReturn(expected);

        EventResponseDto result = eventService.createEvent(collegeId, dto);

        assertThat(result).isEqualTo(expected);
        verify(eventRepository).save(event);
    }

    @Test
    @DisplayName("deleteEvent() sets status to CANCELLED instead of deleting the row")
    void deleteEventSetsCancelled() {
        when(eventRepository.findByEventIdAndCollege_CollegeId(eventId, collegeId))
                .thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);

        eventService.deleteEvent(eventId, collegeId);

        assertThat(event.getStatus()).isEqualTo(EventStatus.CANCELLED);
        verify(eventRepository).save(event);
    }

    @Test
    @DisplayName("getEventById() throws 404 when event not in tenant")
    void getEventCrossTenantThrows404() {
        UUID otherCollegeId = UUID.randomUUID();
        when(eventRepository.findByEventIdAndCollege_CollegeId(eventId, otherCollegeId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getEventById(eventId, otherCollegeId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("does not belong to this college");
    }
}
