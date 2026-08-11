package com.seochang.church.repository;

import com.seochang.church.entity.CalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    List<CalendarEvent> findByStartDateBetweenOrEndDateBetween(
            LocalDateTime start1, LocalDateTime end1,
            LocalDateTime start2, LocalDateTime end2
    );

    // More general query for overlapping events
    List<CalendarEvent> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDateTime end, LocalDateTime start);
}
