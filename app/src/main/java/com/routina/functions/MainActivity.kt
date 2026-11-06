package com.routina.functions

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.routina.functions.network.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : BaseActivity() {

    // Firebase Authentication
    private lateinit var auth: FirebaseAuth

    // Google Sign In
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var btnGoogleAuth: View

    // XML components
    private lateinit var txtWelcome: TextView
    private lateinit var txtSubtitle: TextView
    private lateinit var tabSignIn: TextView
    private lateinit var tabSignUp: TextView

    private lateinit var labelName: TextView
    private lateinit var edtName: EditText

    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText

    private lateinit var labelConfirmPassword: TextView
    private lateinit var edtConfirmPassword: EditText

    private lateinit var txtForgotPassword: TextView
    private lateinit var btnAuth: Button
    private lateinit var txtMessage: TextView

    // Track the selected authentication mode
    private var isSignUp = false

    private val googleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken: String? = account?.idToken
                if (!idToken.isNullOrEmpty()) {
                    firebaseAuthWithGoogle(idToken)
                } else {
                    setLoading(false)
                    showMessage("Google sign-in failed: No ID Token.", false)
                }
            } catch (e: ApiException) {
                setLoading(false)
                showMessage(e.message ?: "Google sign-in failed.", false)
            }
        } else {
            setLoading(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()

        // Remember user session: Bypasses login screen when user is already signed in (online or offline)
        if (auth.currentUser != null) {
            openDashboard()
            return
        }

        setContentView(R.layout.activity_main)

        // Configure Google Sign-In
        val webClientIdResId = resources.getIdentifier("default_web_client_id", "string", packageName)
        val webClientId = if (webClientIdResId != 0) getString(webClientIdResId) else ""

        val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()

        if (webClientId.isNotEmpty() && webClientId != "YOUR_WEB_CLIENT_ID") {
            gsoBuilder.requestIdToken(webClientId)
        }

        googleSignInClient = GoogleSignIn.getClient(this, gsoBuilder.build())

        // Connect XML components
        initializeViews()

        // Set default screen
        showSignIn()

        // Tab listeners
        tabSignIn.setOnClickListener {
            showSignIn()
        }

        tabSignUp.setOnClickListener {
            showSignUp()
        }

        // Authentication button
        btnAuth.setOnClickListener {
            if (isSignUp) {
                registerUser()
            } else {
                loginUser()
            }
        }

        // Google Sign-In button
        btnGoogleAuth.setOnClickListener {
            signInWithGoogle()
        }

        // Forgot password
        txtForgotPassword.setOnClickListener {
            resetPassword()
        }
    }

    // Connect XML elements
    private fun initializeViews() {
        txtWelcome = findViewById(R.id.txtWelcome)
        txtSubtitle = findViewById(R.id.txtSubtitle)

        tabSignIn = findViewById(R.id.tabSignIn)
        tabSignUp = findViewById(R.id.tabSignUp)

        labelName = findViewById(R.id.labelName)
        edtName = findViewById(R.id.edtName)

        edtEmail = findViewById(R.id.edtEmail)
        edtPassword = findViewById(R.id.edtPassword)

        labelConfirmPassword = findViewById(R.id.labelConfirmPassword)
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword)

        txtForgotPassword = findViewById(R.id.txtForgotPassword)

        btnAuth = findViewById(R.id.btnAuth)
        btnGoogleAuth = findViewById(R.id.btnGoogleAuth)
        txtMessage = findViewById(R.id.txtMessage)
    }

    // Display Sign In screen
    private fun showSignIn() {
        isSignUp = false

        txtWelcome.text = getString(R.string.welcome_back)
        txtSubtitle.text = getString(R.string.signin_sub)

        btnAuth.text = getString(R.string.tab_sign_in)

        // Hide registration fields
        labelName.visibility = View.GONE
        edtName.visibility = View.GONE

        labelConfirmPassword.visibility = View.GONE
        edtConfirmPassword.visibility = View.GONE

        txtForgotPassword.visibility = View.VISIBLE

        // Update tab appearance
        tabSignIn.setBackgroundResource(R.drawable.bg_tab_active)
        tabSignIn.setTextColor(Color.WHITE)

        tabSignUp.setBackgroundColor(Color.TRANSPARENT)
        tabSignUp.setTextColor(
            ContextCompat.getColor(this, R.color.text_secondary)
        )

        txtMessage.text = ""
    }

    // Display Sign Up screen
    private fun showSignUp() {
        isSignUp = true

        txtWelcome.text = getString(R.string.create_account)
        txtSubtitle.text = getString(R.string.signup_sub)

        btnAuth.text = getString(R.string.create_account)

        // Show registration fields
        labelName.visibility = View.VISIBLE
        edtName.visibility = View.VISIBLE

        labelConfirmPassword.visibility = View.VISIBLE
        edtConfirmPassword.visibility = View.VISIBLE

        txtForgotPassword.visibility = View.GONE

        // Update tab appearance
        tabSignUp.setBackgroundResource(R.drawable.bg_tab_active)
        tabSignUp.setTextColor(Color.WHITE)

        tabSignIn.setBackgroundColor(Color.TRANSPARENT)
        tabSignIn.setTextColor(
            ContextCompat.getColor(this, R.color.text_secondary)
        )

        txtMessage.text = ""
    }

    // Launch Google Sign-In flow
    private fun signInWithGoogle() {
        setLoading(true)
        val signInIntent = googleSignInClient.signInIntent
        googleLauncher.launch(signInIntent)
    }

    // Authenticate with Firebase using Google ID Token
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                setLoading(false)
                if (task.isSuccessful) {
                    showMessage("Google sign-in successful!", true)
                    openDashboard()
                } else {
                    showMessage(
                        task.exception?.message ?: "Authentication failed.",
                        false
                    )
                }
            }
    }

    // Register a new user
    private fun registerUser() {
        val name = edtName.text.toString().trim()
        val email = edtEmail.text.toString().trim()
        val password = edtPassword.text.toString()
        val confirmPassword = edtConfirmPassword.text.toString()

        // Validate full name
        if (name.isEmpty()) {
            edtName.error = getString(R.string.full_name_hint)
            edtName.requestFocus()
            return
        }

        // Validate email
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.error = "Enter a valid email address"
            edtEmail.requestFocus()
            return
        }

        // Validate password
        if (password.length < 6) {
            edtPassword.error = "Password must contain at least 6 characters"
            edtPassword.requestFocus()
            return
        }

        // Confirm password
        if (password != confirmPassword) {
            edtConfirmPassword.error = "Passwords do not match"
            edtConfirmPassword.requestFocus()
            return
        }

        setLoading(true)

        // Create Firebase account
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->

                if (task.isSuccessful) {

                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()

                    auth.currentUser?.updateProfile(profileUpdates)
                        ?.addOnCompleteListener { profileTask ->

                            setLoading(false)

                            if (profileTask.isSuccessful) {
                                showMessage(
                                    "Account created successfully!",
                                    true
                                )

                                openDashboard()
                            } else {
                                showMessage(
                                    profileTask.exception?.message
                                        ?: "Could not update profile.",
                                    false
                                )
                            }
                        }

                } else {

                    setLoading(false)

                    showMessage(
                        task.exception?.message
                            ?: "Registration failed.",
                        false
                    )
                }
            }
    }

    // Log in an existing user
    private fun loginUser() {
        val email = edtEmail.text.toString().trim()
        val password = edtPassword.text.toString()

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.error = "Enter a valid email address"
            edtEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            edtPassword.error = getString(R.string.password_hint)
            edtPassword.requestFocus()
            return
        }

        setLoading(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->

                setLoading(false)

                if (task.isSuccessful) {

                    showMessage("Login successful!", true)

                    openDashboard()

                } else {

                    showMessage(
                        task.exception?.message
                            ?: "Login failed. Check your details.",
                        false
                    )
                }
            }
    }

    // Reset forgotten password
    private fun resetPassword() {
        val email = edtEmail.text.toString().trim()

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.error = "Enter your registered email address first"
            edtEmail.requestFocus()
            return
        }

        setLoading(true)

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener(this) { task ->

                setLoading(false)

                if (task.isSuccessful) {
                    showMessage(
                        "Password reset link sent to your email.",
                        true
                    )
                } else {
                    showMessage(
                        task.exception?.message
                            ?: "Unable to send reset email.",
                        false
                    )
                }
            }
    }

    // Display feedback
    private fun showMessage(message: String, success: Boolean) {
        txtMessage.text = message

        txtMessage.setTextColor(
            if (success) {
                Color.rgb(22, 163, 74)
            } else {
                Color.rgb(220, 38, 38)
            }
        )
    }

    // Loading state
    private fun setLoading(loading: Boolean) {
        btnAuth.isEnabled = !loading
        btnGoogleAuth.isEnabled = !loading
        tabSignIn.isEnabled = !loading
        tabSignUp.isEnabled = !loading

        btnAuth.text = when {
            loading -> "Connecting (up to 60s)..."
            isSignUp -> getString(R.string.create_account)
            else -> getString(R.string.tab_sign_in)
        }
    }

    // Save device push token to REST API right after login
    private fun saveDeviceToken() {
        try {
            if (auth.currentUser == null) return
            val androidId = android.provider.Settings.Secure.getString(
                contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            ) ?: "device_token"

            ApiClient.apiService.registerPushToken(RegisterTokenRequest(token = androidId))
                .enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {}
                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        // Fallback to saveDeviceToken legacy endpoint
                        ApiClient.apiService.saveDeviceToken(DeviceTokenRequest(deviceToken = androidId))
                            .enqueue(object : Callback<Void> {
                                override fun onResponse(call: Call<Void>, response: Response<Void>) {}
                                override fun onFailure(call: Call<Void>, t: Throwable) {}
                            })
                    }
                })
        } catch (e: Exception) {
            // Ignore token error on launch
        }
    }

    // Navigate to Dashboard
    private fun openDashboard() {
        saveDeviceToken()
        val intent = Intent(this, DashboardActivity::class.java)

        startActivity(intent)
        finish()
    }
}
