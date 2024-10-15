package com.synclytic.app

import com.synclytic.app.CalendarAdapter
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.synclytic.app.presenter.CalendarPresenter
import com.synclytic.app.view.CalendarWidgetView

class MainActivity : AppCompatActivity(), CalendarWidgetView {

    private lateinit var calendarPresenter: CalendarPresenter
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)

        // Initialize Presenter
        calendarPresenter = CalendarPresenter(this)

        // Fetch and display calendar data
        calendarPresenter.fetchCalendarData()
    }

    override fun showCalendarEvents(events: Array<String>) {
        // Handle displaying the calendar events in RecyclerView
        // We will implement this in the next step
        val adapter = CalendarAdapter(events)
        recyclerView.adapter = adapter
    }
}
