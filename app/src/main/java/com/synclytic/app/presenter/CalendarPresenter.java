package com.synclytic.app.presenter;

import com.synclytic.app.model.CalendarModel;
import com.synclytic.app.view.CalendarWidgetView;

public class CalendarPresenter {
    private CalendarModel model;
    private CalendarWidgetView view;

    public CalendarPresenter(CalendarWidgetView view) {
        this.view = view;
        this.model = new CalendarModel(); // Initialize the model
    }

    // This method fetches the data and updates the view
    public void loadCalendarEvents() {
        model.fetchGoogleCalendarEvents();  // Fetch Google events
        model.fetchOutlookCalendarEvents(); // Fetch Outlook events

        // Now merge and sort the events
        model.mergeAndSortEvents();

        // TODO: Pass the fetched data to the view for display
        // view.updateWidget(...);
    }
}
