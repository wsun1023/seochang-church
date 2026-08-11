package com.seochang.church.controller;

import com.seochang.church.entity.CalendarEvent;
import com.seochang.church.service.CalendarEventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/api/calendar")
public class AdminCalendarRestController {

    private final CalendarEventService calendarEventService;

    public AdminCalendarRestController(CalendarEventService calendarEventService) {
        this.calendarEventService = calendarEventService;
    }

    @PostMapping("/events")
    public ResponseEntity<CalendarEvent> createEvent(@RequestBody CalendarEvent event) {
        CalendarEvent savedEvent = calendarEventService.saveEvent(event);
        return ResponseEntity.ok(savedEvent);
    }

    @PutMapping("/events/{id}")
    public ResponseEntity<CalendarEvent> updateEvent(@PathVariable("id") Long id, @RequestBody CalendarEvent event) {
        CalendarEvent updatedEvent = calendarEventService.updateEvent(id, event);
        return ResponseEntity.ok(updatedEvent);
    }

    @DeleteMapping("/events/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable("id") Long id) {
        calendarEventService.deleteEvent(id);
        return ResponseEntity.ok().build();
    }
}
