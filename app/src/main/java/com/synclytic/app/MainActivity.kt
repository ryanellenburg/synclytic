package com.synclytic.app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.synclytic.app.presenter.CalendarPresenter
import com.synclytic.app.view.CalendarView

class MainActivity : AppCompatActivity(), CalendarView {  // Implement CalendarView interface
    private lateinit var recyclerView: RecyclerView
    private lateinit var presenter: CalendarPresenter  // Initialize Presenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerView)

        // Initialize Presenter
        presenter = CalendarPresenter(this)
        presenter.loadCalendarData() // Call presenter to load data
    }

    // Implement CalendarView methods
    override fun showCalendarEvents(events: List<String>) {
        // You can display events on RecyclerView here
        Toast.makeText(this, "Calendar events loaded", Toast.LENGTH_SHORT).show()
        // In the future, update RecyclerView with calendar events data here
    }

    override fun showError(error: String) {
        // Display error message if data loading fails
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
    }
}
