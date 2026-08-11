package com.seochang.church.service;

import com.seochang.church.entity.CalendarEvent;
import com.seochang.church.repository.CalendarEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CalendarEventService {

    private final CalendarEventRepository calendarEventRepository;

    public CalendarEventService(CalendarEventRepository calendarEventRepository) {
        this.calendarEventRepository = calendarEventRepository;
    }

    public List<CalendarEvent> getEventsBetween(LocalDateTime start, LocalDateTime end) {
        // Find events that overlap with the start-end period
        return calendarEventRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(end, start);
    }

    public List<CalendarEvent> getAllEvents() {
        return calendarEventRepository.findAll();
    }

    public CalendarEvent saveEvent(CalendarEvent event) {
        event.setUpdatedAt(LocalDateTime.now());
        if (event.getId() == null) {
            event.setCreatedAt(LocalDateTime.now());
        }
        return calendarEventRepository.save(event);
    }

    public Optional<CalendarEvent> getEventById(Long id) {
        return calendarEventRepository.findById(id);
    }

    public CalendarEvent updateEvent(Long id, CalendarEvent eventDetails) {
        CalendarEvent event = calendarEventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid event Id:" + id));

        event.setTitle(eventDetails.getTitle());
        event.setDescription(eventDetails.getDescription());
        event.setStartDate(eventDetails.getStartDate());
        event.setEndDate(eventDetails.getEndDate());
        event.setAllDay(eventDetails.isAllDay());
        event.setColor(eventDetails.getColor());
        event.setUpdatedAt(LocalDateTime.now());

        return calendarEventRepository.save(event);
    }

    public void deleteEvent(Long id) {
        calendarEventRepository.deleteById(id);
    }
}
