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
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.util.*
import reactor.core.publisher.Mono

// Google Sign-In imports
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest
import com.google.api.client.auth.oauth2.TokenRequest
import com.google.api.client.http.GenericUrl
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.AccessToken as GoogleAccessToken
import com.google.auth.oauth2.GoogleCredentials



// Google Calendar API imports
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event as GoogleEvent

// MSAL (Microsoft Authentication Library) imports
import com.azure.core.credential.AccessToken as MicrosoftAccessToken
import com.azure.core.credential.TokenCredential
import com.azure.core.credential.TokenRequestContext
import com.microsoft.graph.authentication.TokenCredentialAuthProvider
import com.microsoft.graph.core.ClientException
import com.microsoft.graph.models.Event as MicrosoftEvent
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

    // Google Cloud Console Credentials
    private val CLIENT_ID = "915129291320-sc0e1kg26vlamahsqo12tm4flvtsuu7p.apps.googleusercontent.com"
    private val CLIENT_SECRET = "YOUR_CLIENT_SECRET" //TODO need to find this as it is not showing up on Google Cloud Console

    // Request code for Google Sign-In
    private val RC_SIGN_IN = 9001

    // Scopes for Microsoft Calendar access
    private val MS_SCOPES = arrayOf("Calendars.Read")

    // Creation of list for calendar events
    private val calendarEvents: MutableList<CalendarEvent> = mutableListOf()

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

        // Initialize Google Sign-In and trigger the sign-in process
        signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data

                val task = GoogleSignIn.getSignedInAccountFromIntent(data)

                handleGoogleSignInResult(task)
            }
        }

        // Check if the user is already signed in (using refresh token, etc.)
        val refreshToken = getRefreshTokenFromStorage()
        if (refreshToken != null) {
            val newAccessToken = getAccessTokenUsingRefreshToken(refreshToken)
        } else {
            signInWithGoogle() // User is not signed in, initiate the sign-in process
        }

        // Initialize MSAL and trigger Microsoft sign-in process
        initializeMSAL()
    }

    // Start the Google Sign-In process if not signed in
    private fun signInWithGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(CalendarScopes.CALENDAR))

            .requestServerAuthCode(CLIENT_ID) // Request server auth code
            .build()

        googleSignInClient = GoogleSignIn.getClient(this,
        gso)

        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)

    }

    private fun getAccessTokenUsingRefreshToken(refreshToken: String): String? {
        try {
            // Get a new access token using the refresh token
            val tokenResponse = GoogleAuthorizationCodeTokenRequest(
                NetHttpTransport(),
                GsonFactory(),
                "https://oauth2.googleapis.com/token",
                CLIENT_ID,
                CLIENT_SECRET,
                refreshToken,
                "" // Redirect URI is not needed for refresh token flow
            ).execute()

            // Create new GoogleCredentials with the access token
            val googleCredentials = GoogleCredentials.create(GoogleAccessToken(tokenResponse.accessToken, null))

            return tokenResponse.accessToken
        } catch (e: IOException) {
            Log.e("TokenRefresh", "Error refreshing access token", e)
            return null
        }
    }


    // TODO come up with description
    private fun storeRefreshTokenToStorage(refreshToken: String) {
        val sharedPreferences = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("refresh_token", refreshToken)
        editor.apply()
    }

    // TODO come up with description
    private fun getRefreshTokenFromStorage(): String? {
        val sharedPreferences = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("refresh_token", null)
    }

    // Google Calendar Sign-In Step TBD
    // Process the result of Google Sign-In
    private fun handleGoogleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            // Get signed-in account information
            val account = task.getResult(ApiException::class.java)

            Log.d("GoogleSignIn", "Signed in as: ${account.email}")

            val serverAuthCode = account.serverAuthCode

            val refreshToken = account.serverAuthCode
            if (refreshToken != null) {
                storeRefreshTokenToStorage(refreshToken)
            }

            // Get the access token
            val accessToken = account.idToken // Get the ID token

            // Now you have the accessToken, use it to fetch Google Calendar data
            fetchGoogleCalendarData(accessToken)

        } catch (e: ApiException) {
            Log.e("GoogleSignIn", "Sign-in failed: ${e.statusCode}")
        }
    }

    // Outlook Calendar Sign-In Step 1
    // MSAL (Microsoft) Authentication setup
    private fun initializeMSAL() {
        // Initialize MSAL with configuration
        msalApp = PublicClientApplication.create(this, R.raw.msal_config) // Referencing MSAL config JSON

        // Fetch accounts from MSAL cache
        getMicrosoftAccounts()
    }

    // Outlook Calendar Sign-In Step 2
    // Retrieve the accounts from the MSAL cache
    private fun getMicrosoftAccounts() {
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

    // Outlook Calendar Sign-In Step 3
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

    // Fetch Google and Microsoft Calendar Data Step 1
    // Fetch Google Calendar data using the access token
    private fun fetchGoogleCalendarData(accessTokenString: String?) {
        if (accessTokenString == null) return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. Initialize Google Calendar Service using Google Auth Library
                val accessToken = GoogleAccessToken(accessTokenString, null)
                val googleCredentials = GoogleCredentials.create(accessToken)
                val requestInitializer = HttpCredentialsAdapter(googleCredentials)

                val service = Calendar.Builder(
                    NetHttpTransport(),
                    GsonFactory(),
                    requestInitializer
                )
                    .setApplicationName("Your Application Name")
                    .build()

                // 2. Fetch Calendar Events
                val now = DateTime(System.currentTimeMillis())
                val eventsResult = service.events().list("primary")
                    .setTimeMin(now)
                    .setSingleEvents(true)
                    .setOrderBy("startTime")
                    .execute()

                val calendarEvents = eventsResult.items?.mapNotNull { event ->
                    // Date Conversion
                    val startDateTime = if (event.start.dateTime != null) event.start.dateTime else event.start.date
                    val endDateTime = if (event.end.dateTime != null) event.end.dateTime else event.end.date

                    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                    val startDate = dateFormat.parse(startDateTime.toString())
                    val endDate = dateFormat.parse(endDateTime.toString())

                    val rsvpStatus = getRsvpStatus(event)

                    // Filter out declined events
                    if (rsvpStatus == CalendarEvent.RsvpStatus.DECLINED) {
                        null
                    } else {
                        CalendarEvent(
                            event.id ?: "",
                            event.summary ?: "",
                            startDate ?: Date(0),
                            endDate ?: Date(0),
                            event.start.date != null,
                            event.organizer?.email ?: "",
                            0,
                            CalendarEvent.Source.GOOGLE,
                            rsvpStatus
                        )
                    }
                } ?: emptyList()

                // 3. Update UI
                withContext(Dispatchers.Main) {
                    this@MainActivity.showCalendarEvents(calendarEvents)
                }
            } catch (ex: Exception) {
                Log.e("GoogleCalendar", "Failed to fetch calendar events: ${ex.message}")
            }
        }
    }

    // Fetch Google and Microsoft Calendar Data Step 2
    // Fetch Microsoft Calendar data using the access token
    private fun fetchMicrosoftCalendarData(accessToken: String?) {
        if (accessToken == null) return

        // Initialize Microsoft Graph client
        val authProvider = TokenCredentialAuthProvider(
            MS_SCOPES.toList(),
            object : TokenCredential {
                override fun getToken(tokenRequestContext: TokenRequestContext): Mono<MicrosoftAccessToken> {
                    return Mono.just(
                        MicrosoftAccessToken(accessToken, OffsetDateTime.now().plusHours(1)) // Set token expiration
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

                val calendarEvents = eventCollectionPage?.currentPage?.mapNotNull { event ->
                    // Date Conversion
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()) // Adjust date format if needed
                    val startDate = dateFormat.parse(event.start?.dateTime)
                    val endDate = dateFormat.parse(event.end?.dateTime)

                    val rsvpStatus = getRsvpStatus(event)

                    // Filter out declined events
                    if (rsvpStatus == CalendarEvent.RsvpStatus.DECLINED) {
                        null
                    } else {
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
                    }
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

    // Google and Microsoft Step 3
    // getRsvpStatus() function
    private fun getRsvpStatus(event: Any): CalendarEvent.RsvpStatus {
        return when (event) {
            is GoogleEvent -> {
                // Logic for Google events
                val attendees = event.attendees
                if (attendees != null) {
                    val myAttendance = attendees.find { it.self }
                    if (myAttendance != null) {
                        return when (myAttendance.responseStatus) {
                            "accepted" -> CalendarEvent.RsvpStatus.ACCEPTED
                            "declined" -> CalendarEvent.RsvpStatus.DECLINED
                            "tentative" -> CalendarEvent.RsvpStatus.TENTATIVE
                            "needsAction" -> CalendarEvent.RsvpStatus.NEEDS_ACTION
                            else -> CalendarEvent.RsvpStatus.NEEDS_ACTION
                        }
                    }
                }
                CalendarEvent.RsvpStatus.NEEDS_ACTION // Default if no attendees or self not found
            }
            is MicrosoftEvent -> {
                // Logic for Microsoft events (your existing code)
                when (event.responseStatus?.response) {
                    ResponseType.ACCEPTED -> CalendarEvent.RsvpStatus.ACCEPTED
                    ResponseType.ORGANIZER -> CalendarEvent.RsvpStatus.ACCEPTED
                    ResponseType.DECLINED -> CalendarEvent.RsvpStatus.DECLINED
                    ResponseType.TENTATIVELY_ACCEPTED -> CalendarEvent.RsvpStatus.TENTATIVE
                    ResponseType.NOT_RESPONDED -> CalendarEvent.RsvpStatus.NEEDS_ACTION
                    ResponseType.NONE -> CalendarEvent.RsvpStatus.NEEDS_ACTION
                    ResponseType.UNEXPECTED_VALUE -> CalendarEvent.RsvpStatus.NEEDS_ACTION
                    else -> CalendarEvent.RsvpStatus.NEEDS_ACTION
                }
            }
            else -> CalendarEvent.RsvpStatus.NEEDS_ACTION // Default for unknown event type
        }
    }

    // Google and Microsoft Step 4
    // Method to show calendar events in the RecyclerView
    override fun showCalendarEvents(calendarEvents: List<CalendarEvent>?) {
        // Here you would populate your RecyclerView with the events
        recyclerView.adapter = CalendarAdapter(calendarEvents) // Pass the CalendarEvent list directly
    }
}