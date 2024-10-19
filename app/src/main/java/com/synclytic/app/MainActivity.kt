package com.synclytic.app

// Android framework imports
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Java imports
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.util.*
import java.util.concurrent.CompletableFuture
import reactor.core.publisher.Mono

// Google Sign-In imports
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task

// Google Calendar API imports
import com.google.api.services.calendar.CalendarScopes

// MSAL (Microsoft Authentication Library) imports
import com.azure.core.credential.AccessToken
import com.azure.core.credential.TokenCredential
import com.azure.core.credential.TokenRequestContext
import com.microsoft.graph.authentication.TokenCredentialAuthProvider
import com.microsoft.graph.core.ClientException
import com.microsoft.graph.models.Event
import com.microsoft.graph.models.ResponseType
import com.microsoft.graph.requests.GraphServiceClient
import com.microsoft.identity.client.*
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.exception.MsalException

// Local imports
import com.synclytic.app.model.CalendarEvent
import com.synclytic.app.view.CalendarAdapter
import com.synclytic.app.view.CalendarView
import com.synclytic.app.view.CalendarWidgetView
import com.synclytic.app.presenter.CalendarPresenter

class MainActivity : AppCompatActivity(), CalendarView {

    // Presenter for handling calendar data logic
    private lateinit var calendarPresenter: CalendarPresenter

    // RecyclerView to display calendar events
    private lateinit var recyclerView: RecyclerView

    // Google Sign-In client to handle authentication
    private lateinit var googleSignInClient: GoogleSignInClient

    // ActivityResultLauncher for Google Sign-In
    private lateinit var signInLauncher: ActivityResultLauncher<Intent>

    // MSAL client for Microsoft authentication
    private var msalApp: IPublicClientApplication? = null

    // Request code for Google Sign-In
    private val RC_SIGN_IN = 9001

    // Scopes for Microsoft Calendar access
    private val MS_SCOPES = arrayOf("Calendars.Read")

    // Variable to hold the current account for silent authentication
    private var currentAccount: IAccount? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerView)

        // Initialize Presenter
        val prefs = getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
        val widgetViewKey = prefs.getString("widget_view_key", null)

        // Check if the key exists in Shared Preferences
        if (widgetViewKey != null) {
            // Create a CalendarWidgetView instance
            // (In a real implementation, you would deserialize the instance here)
            val calendarWidgetView = CalendarWidgetView()

            calendarPresenter = CalendarPresenter(this, calendarWidgetView)

        } else {
            // Handle the case where the widget view is not available
            Log.e("MainActivity", "CalendarWidgetView not found in SharedPreferences")
        }

        // Register the ActivityResultLauncher for Google Sign-In
        signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
                handleGoogleSignInResult(task)
            }
        }

        // Initialize Google Sign-In and trigger the sign-in process
        initializeGoogleSignIn()

        // Initialize MSAL and trigger Microsoft sign-in process
        initializeMSAL()
    }

    // Google Sign-In setup
    private fun initializeGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail() // Request email permission
            .requestScopes(Scope(CalendarScopes.CALENDAR)) // Request access to Google Calendar API
            .build()

        // Initialize GoogleSignInClient with the options above
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Trigger the Google Sign-In flow
        signInWithGoogle()
    }

    // Start the Google Sign-In process
    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    // MSAL (Microsoft) Authentication setup
    private fun initializeMSAL() {
        // Initialize MSAL with configuration
        msalApp = PublicClientApplication.create(this, R.raw.msal_config) // Referencing MSAL config JSON

        // Fetch accounts from MSAL cache
        getAccounts()
    }

    // Retrieve the accounts from the MSAL cache
    private fun getAccounts() {
        (msalApp as ISingleAccountPublicClientApplication).getCurrentAccountAsync(object : ISingleAccountPublicClientApplication.CurrentAccountCallback {
            override fun onAccountLoaded(account: IAccount?) {
                account?.let {
                    Log.d("MSAL", "Current account: ${it.username}")
                    signInWithMicrosoft() // Call your sign-in method here
                }
            }

            override fun onError(exception: MsalException) {
                Log.e("MSAL", "Error retrieving current account: ${exception.message}")
            }

            // Implement the required onAccountChanged method
            override fun onAccountChanged(oldAccount: IAccount?, newAccount: IAccount?) {
                Log.d("MSAL", "Account changed from ${oldAccount?.username} to ${newAccount?.username}")
                // Handle account change logic here if needed
            }
        })
    }

    // Start Microsoft sign-in process
    private fun signInWithMicrosoft() {
        val parameters = AcquireTokenParameters.Builder()
            .withScopes(MS_SCOPES.toList())
            .withCallback(object : AuthenticationCallback {
                override fun onSuccess(authenticationResult: IAuthenticationResult?) {
                    Log.d("MSAL", "Signed in successfully: ${authenticationResult?.account?.username}")
                    currentAccount = authenticationResult?.account
                    fetchMicrosoftCalendarData(authenticationResult?.accessToken)
                }

                override fun onError(exception: MsalException?) {
                    Log.e("MSAL", "Error during sign-in: ${exception?.message}")
                }

                override fun onCancel() {
                    Log.d("MSAL", "Sign-in canceled by user")
                }
            })
            .startAuthorizationFromActivity(this) // Start from the activity context.
            .build()

        msalApp?.acquireToken(parameters)
    }

    // Fetch Microsoft Calendar data using the access token
    private fun fetchMicrosoftCalendarData(accessToken: String?) {
        if (accessToken == null) return

        // Initialize Microsoft Graph client
        val authProvider = TokenCredentialAuthProvider(
            MS_SCOPES.toList(),
            object : TokenCredential {
                override fun getToken(tokenRequestContext: TokenRequestContext): Mono<AccessToken> {
                    return Mono.just(
                        AccessToken(accessToken, OffsetDateTime.now().plusHours(1)) // Set token expiration
                    )
                }
            }
        )// Build the Graph client using the authProvider
        val graphClient = GraphServiceClient.builder()
            .authenticationProvider(authProvider)
            .buildClient()

        // Fetch calendar events asynchronously (using coroutines is recommended for Kotlin)
        lifecycleScope.launch(Dispatchers.IO) { // Launch in a background thread
            try {
                val eventCollectionPage = graphClient
                    .me()
                    .events()
                    .buildRequest()
                    .get()

                val calendarEvents = eventCollectionPage?.currentPage?.map { event ->
                    // Date Conversion
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()) // Adjust date format if needed
                    val startDate = dateFormat.parse(event.start?.dateTime)
                    val endDate = dateFormat.parse(event.end?.dateTime)
                    CalendarEvent(
                        event.id ?: "",  // Provide a default value ("") if null
                        event.subject ?: "", // Provide a default value ("") if null
                        startDate ?: Date(0), // Provide Date(0) as default
                        endDate ?: Date(0),   // Provide Date(0) as default
                        event.isAllDay ?: false, // Provide false as default
                        event.calendar?.id ?: "", // Provide a default value ("") if null
                        0,
                        CalendarEvent.Source.OUTLOOK,
                        getRsvpStatus(event)
                    )
                } ?: emptyList() // Provide an empty list if eventCollectionPage is null

                // Update the UI on the main thread
                withContext(Dispatchers.Main) {
                    this@MainActivity.showCalendarEvents(calendarEvents)
                }
            } catch (ex: ClientException) {
                Log.e("MicrosoftGraph", "Failed to fetch calendar events: ${ex.message}")
            }
        }
    }

    // getRsvpStatus() function
    private fun getRsvpStatus(event: Event): CalendarEvent.RsvpStatus {
        // (Implementation from my previous response)
        return when (event.responseStatus?.response) {
            ResponseType.ACCEPTED -> CalendarEvent.RsvpStatus.ACCEPTED
            ResponseType.ORGANIZER -> CalendarEvent.RsvpStatus.ACCEPTED
            ResponseType.TENTATIVELY_ACCEPTED -> CalendarEvent.RsvpStatus.TENTATIVE
            ResponseType.NOT_RESPONDED -> CalendarEvent.RsvpStatus.NEEDS_ACTION
            ResponseType.NONE -> CalendarEvent.RsvpStatus.NEEDS_ACTION
            ResponseType.UNEXPECTED_VALUE -> CalendarEvent.RsvpStatus.NEEDS_ACTION
            else -> CalendarEvent.RsvpStatus.NEEDS_ACTION // Default to NEEDS_ACTION if no match
        }
    }

    // Handle the result of Google Sign-In activity
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // MSAL automatically handles redirect responses via super.onActivityResult()
    }

    // Process the result of Google Sign-In
    private fun handleGoogleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            // Get signed-in account information
            val account = task.getResult(ApiException::class.java)
            Log.d("GoogleSignIn", "Signed in as: ${account.email}")

            // Use the signed-in account to fetch calendar events
            calendarPresenter.fetchCalendarData() // Call the method to fetch events
        } catch (e: ApiException) {
            Log.e("GoogleSignIn", "Sign-in failed: ${e.statusCode}")
        }
    }

    // Method to show calendar events in the RecyclerView
    override fun showCalendarEvents(calendarEvents: List<CalendarEvent>?) {
        // Here you would populate your RecyclerView with the events
        recyclerView.adapter = CalendarAdapter(calendarEvents) // Pass the CalendarEvent list directly
    }
}