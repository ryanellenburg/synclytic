package com.synclytic.app

// Android framework imports
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView

// Google Sign-In imports
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task

// Google Calendar API imports
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.calendar.CalendarScopes

// Local imports
import com.synclytic.app.presenter.CalendarPresenter
import com.synclytic.app.view.CalendarAdapter
import com.synclytic.app.view.CalendarView

class MainActivity : AppCompatActivity(), CalendarView {

    // Presenter for handling calendar data logic
    private lateinit var calendarPresenter: CalendarPresenter

    // RecyclerView to display calendar events
    private lateinit var recyclerView: RecyclerView

    // Google Sign-In client to handle authentication
    private lateinit var googleSignInClient: GoogleSignInClient

    // Request code for Google Sign-In
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerView)

        // Initialize Presenter
        calendarPresenter = CalendarPresenter(this)

        // Configure Google Sign-In options
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail() // Request email permission
            .requestScopes(Scope(CalendarScopes.CALENDAR)) // Request access to Google Calendar API
            .build()

        // Initialize Google SignInClient with the options above
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Trigger the Google Sign-In flow
        signIn()

        // Fetch and display calendar data
        calendarPresenter.fetchCalendarData()
    }

    // Start the Google Sign-In process
    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    // Handle the result of the Google Sign-In activity
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    // Process the result of Google Sign-In
    private fun handleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            // Get signed-in account information
            val account = task.getResult(ApiException::class.java)
            Log.d("GoogleSignIn", "Signed in as: ${account.email}")

            // Use the signed-in account to access Google Calendar API
        } catch (e: ApiException) {
            Log.w("GoogleSignIn", "signInResult:failed code=" + e.statusCode)
        }
    }

    // Display the fetched calendar events using a RecyclerView
    override fun showCalendarEvents(events: Array<String>) {
        val adapter = CalendarAdapter(events)
        recyclerView.adapter = adapter
    }
}
