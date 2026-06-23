package com.campus.platform.event.scheduler;

import com.campus.platform.common.enums.EventStatus;
import com.campus.platform.event.entity.Event;
import com.campus.platform.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventScheduler {

    private final EventRepository eventRepository;
    private final Clock clock;  // ← inject clock

    @Scheduled(fixedRate = 300_000)
    @Transactional
    public void autoCloseRegistrations() {
        List<Event> events = eventRepository
                .findAllByStatusAndRegistrationDeadlineBefore(
                        EventStatus.PUBLISHED, LocalDateTime.now(clock));  // ← fixed
        events.forEach(e -> e.setStatus(EventStatus.REGISTRATION_CLOSED));
        eventRepository.saveAll(events);
        if (!events.isEmpty()) {
            log.info("Auto-closed registrations for {} events", events.size());
        }
    }

    @Scheduled(fixedRate = 300_000)
    @Transactional
    public void autoCompleteEvents() {
        List<Event> events = eventRepository
                .findAllByStatusAndEventDateBefore(
                        EventStatus.REGISTRATION_CLOSED, LocalDateTime.now(clock));  // ← fixed
        eventRepository.saveAll(events);
        if (!events.isEmpty()) {
            log.info("Auto-completed {} events", events.size());
        }
    }
}