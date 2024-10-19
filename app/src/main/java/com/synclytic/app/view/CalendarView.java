package com.synclytic.app.view;

import com.synclytic.app.model.CalendarModel;

public interface CalendarView {
    void showCalendarEvents(List<CalendarEvent> calendarEvents);
}