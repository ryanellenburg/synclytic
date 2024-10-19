package com.synclytic.app.view;

import com.synclytic.app.model.CalendarEvent;
import java.util.List;

public interface CalendarView {
    void showCalendarEvents(List<CalendarEvent> calendarEvents);
}