package com.synclytic.app

// Android framework imports
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView

// Java imports
import java.util.concurrent.CompletableFuture

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

// MSAL (Microsoft Authentication Library) imports
import com.microsoft.identity.client.*
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication.CurrentAccountCallback
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.graph.core.ClientException
import com.microsoft.graph.http.IHttpRequest
import com.microsoft.graph.requests.extensions.GraphServiceClient
import com.microsoft.graph.requests.extensions.IEventCollectionPage

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
        calendarPresenter = CalendarPresenter(this)

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
        val graphClient = GraphServiceClient.builder()
            .authenticationProvider { request: IHttpRequest ->
                request.addHeader("Authorization", "Bearer $accessToken")
            }
            .buildClient()

        // Fetch calendar events asynchronously using Java CompletableFuture
        CompletableFuture.supplyAsync {
            graphClient
                .me()
                .events()
                .buildRequest()
                .get() // This is a synchronous call that will be executed in a separate thread
        }.thenApply { eventCollectionPage: IEventCollectionPage ->
            // Extract events after the API call completes
            val events = eventCollectionPage.currentPage.map { event -> event.subject }.toTypedArray()
            showCalendarEvents(events) // Update the UI with calendar events
        }.exceptionally { ex: Throwable ->
            Log.e("MicrosoftGraph", "Failed to fetch calendar events: ${ex.message}")
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
    override fun showCalendarEvents(events: Array<String>) {
        // Here you would populate your RecyclerView with the events
        recyclerView.adapter = CalendarAdapter(events) // Assuming you have a CalendarAdapter
    }
}
