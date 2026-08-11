package com.seochang.church.controller;

import com.seochang.church.entity.CalendarEvent;
import com.seochang.church.service.CalendarEventService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/calendar")
public class CalendarRestController {

    private final CalendarEventService calendarEventService;
    private static final DateTimeFormatter ICAL_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    private static final DateTimeFormatter ICAL_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    public CalendarRestController(CalendarEventService calendarEventService) {
        this.calendarEventService = calendarEventService;
    }

    // FullCalendar JSON Data Endpoint
    @GetMapping("/events")
    public List<Map<String, Object>> getEvents(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        
        List<CalendarEvent> events = calendarEventService.getEventsBetween(start, end);
        
        return events.stream().map(event -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", event.getId());
            map.put("title", event.getTitle());
            map.put("start", event.getStartDate());
            map.put("end", event.getEndDate());
            map.put("allDay", event.isAllDay());
            map.put("color", event.getColor());
            map.put("description", event.getDescription());
            return map;
        }).collect(Collectors.toList());
    }

    // iCal Export Endpoint
    @GetMapping(value = "/ical", produces = "text/calendar")
    public ResponseEntity<String> getICalExport() {
        List<CalendarEvent> events = calendarEventService.getAllEvents();
        LocalDateTime now = LocalDateTime.now();
        String dtStamp = now.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("UTC")).format(ICAL_FORMATTER);

        StringBuilder ical = new StringBuilder();
        ical.append("BEGIN:VCALENDAR\n");
        ical.append("VERSION:2.0\n");
        ical.append("PRODID:-//Seochang Church//Calendar//KO\n");
        ical.append("CALSCALE:GREGORIAN\n");

        for (CalendarEvent e : events) {
            ical.append("BEGIN:VEVENT\n");
            ical.append("UID:event-").append(e.getId()).append("@seochang-church.com\n");
            ical.append("DTSTAMP:").append(dtStamp).append("\n");
            
            if (e.isAllDay()) {
                String start = e.getStartDate().format(ICAL_DATE_FORMATTER);
                String end = e.getEndDate().format(ICAL_DATE_FORMATTER);
                ical.append("DTSTART;VALUE=DATE:").append(start).append("\n");
                ical.append("DTEND;VALUE=DATE:").append(end).append("\n");
            } else {
                String start = e.getStartDate().atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("UTC")).format(ICAL_FORMATTER);
                String end = e.getEndDate().atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("UTC")).format(ICAL_FORMATTER);
                ical.append("DTSTART:").append(start).append("\n");
                ical.append("DTEND:").append(end).append("\n");
            }

            ical.append("SUMMARY:").append(escapeICal(e.getTitle())).append("\n");
            if (e.getDescription() != null && !e.getDescription().isEmpty()) {
                ical.append("DESCRIPTION:").append(escapeICal(e.getDescription())).append("\n");
            }
            ical.append("END:VEVENT\n");
        }
        ical.append("END:VCALENDAR\n");

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=seochang-church-calendar.ics");
        headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
        headers.add(HttpHeaders.PRAGMA, "no-cache");
        headers.add(HttpHeaders.EXPIRES, "0");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("text/calendar"))
                .body(ical.toString());
    }

    private String escapeICal(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n");
    }
}
