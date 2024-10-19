package com.synclytic.app.model;

import java.util.Date;

public class CalendarEvent {
    public enum Source {
        GOOGLE, OUTLOOK
    }
    public enum RsvpStatus {
        ACCEPTED, TENTATIVE, NEEDS_ACTION, DECLINED
    }

    private String id;
    private String title;
    private Date startTime;
    private Date endTime;
    private boolean allDay;
    private String calendarId;
    private int color;
    private Source source;
    private RsvpStatus rsvpStatus;

    // Constructor
    public CalendarEvent(String id, String title, Date startTime, Date endTime,
                         boolean allDay, String calendarId, int color,
                         Source source, RsvpStatus rsvpStatus) { // Update constructor
        this.id = id;
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.allDay = allDay;
        this.calendarId = calendarId;
        this.color = color;
        this.source = source;
        this.rsvpStatus = rsvpStatus;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public boolean isAllDay() {
        return allDay;
    }

    public String getCalendarId() {
        return calendarId;
    }

    public int getColor() {
        return color;
    }

    public Source getSource() {
        return source;
    }

    public RsvpStatus getRsvpStatus() {
        return rsvpStatus;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public void setAllDay(boolean allDay) {
        this.allDay = allDay;
    }

    public void setCalendarId(String calendarId) {
        this.calendarId = calendarId;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public void setRsvpStatus(RsvpStatus rsvpStatus) {
        this.rsvpStatus = rsvpStatus;
    }
}
