package com.routina.functions

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.routina.functions.network.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SettingsActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var preferences: android.content.SharedPreferences

    private lateinit var txtProfileInitials: TextView
    private lateinit var txtProfileName: TextView
    private lateinit var txtProfileEmail: TextView
    private lateinit var edtDisplayName: EditText
    private lateinit var btnSaveProfile: Button

    private lateinit var switchNotifications: Switch
    private lateinit var switchDarkMode: Switch
    private lateinit var spinnerLanguage: Spinner

    private lateinit var btnSignOut: Button
    private lateinit var btnBackDashboard: Button

    override fun onCreate(savedInstanceState: Bundle?) {

        preferences = getSharedPreferences(
            "RoutinaSettings",
            MODE_PRIVATE
        )

        val darkMode = preferences.getBoolean(
            "darkMode",
            false
        )

        AppCompatDelegate.setDefaultNightMode(
            if (darkMode) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
        )

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null) {
            openLogin()
            return
        }

        initializeViews()

        loadProfile()
        loadPreferences()
        setupListeners()
    }

    private fun initializeViews() {
        txtProfileInitials = findViewById(R.id.txtProfileInitials)
        txtProfileName = findViewById(R.id.txtProfileName)
        txtProfileEmail = findViewById(R.id.txtProfileEmail)
        edtDisplayName = findViewById(R.id.edtDisplayName)
        btnSaveProfile = findViewById(R.id.btnSaveProfile)
        switchNotifications = findViewById(R.id.switchNotifications)
        switchDarkMode = findViewById(R.id.switchDarkMode)
        spinnerLanguage = findViewById(R.id.spinnerLanguage)
        btnSignOut = findViewById(R.id.btnSignOut)
        btnBackDashboard = findViewById(R.id.btnBackDashboard)
    }

    private fun loadProfile() {
        val user = auth.currentUser ?: return

        val name = user.displayName ?: "Routina User"
        val email = user.email ?: "No email available"

        txtProfileName.text = name
        txtProfileEmail.text = email

        edtDisplayName.setText(name)

        updateInitials(name)

        ApiClient.apiService.getUserProfile().enqueue(object : Callback<UserProfileDto> {
            override fun onResponse(call: Call<UserProfileDto>, response: Response<UserProfileDto>) {
                if (response.isSuccessful) {
                    val savedName = response.body()?.displayName
                    if (!savedName.isNullOrBlank()) {
                        txtProfileName.text = savedName
                        edtDisplayName.setText(savedName)
                        updateInitials(savedName)
                    }
                }
            }

            override fun onFailure(call: Call<UserProfileDto>, t: Throwable) {}
        })
    }

    private fun updateInitials(name: String) {
        val initials = name.trim()
            .split("\\s+".toRegex())
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .joinToString("")

        txtProfileInitials.text = initials.ifEmpty { "U" }
    }

    private fun saveProfile() {
        val newName = edtDisplayName.text.toString().trim()

        if (newName.isEmpty()) {
            edtDisplayName.error = getString(R.string.enter_display_name_hint)
            edtDisplayName.requestFocus()
            return
        }

        val user = auth.currentUser ?: return

        btnSaveProfile.isEnabled = false
        btnSaveProfile.text = getString(R.string.saving)

        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(newName)
            .build()

        user.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->

                if (task.isSuccessful) {
                    val req = UserProfileUpdateRequest(
                        displayName = newName,
                        email = user.email ?: ""
                    )

                    ApiClient.apiService.updateUserProfile(req).enqueue(object : Callback<UserProfileDto> {
                        override fun onResponse(call: Call<UserProfileDto>, response: Response<UserProfileDto>) {
                            btnSaveProfile.isEnabled = true
                            btnSaveProfile.text = getString(R.string.save_profile)

                            txtProfileName.text = newName
                            updateInitials(newName)

                            Toast.makeText(
                                this@SettingsActivity,
                                "Profile updated successfully!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        override fun onFailure(call: Call<UserProfileDto>, t: Throwable) {
                            btnSaveProfile.isEnabled = true
                            btnSaveProfile.text = getString(R.string.save_profile)

                            txtProfileName.text = newName
                            updateInitials(newName)

                            Toast.makeText(
                                this@SettingsActivity,
                                "Profile updated locally.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })

                } else {
                    btnSaveProfile.isEnabled = true
                    btnSaveProfile.text = getString(R.string.save_profile)

                    Toast.makeText(
                        this,
                        "Unable to update profile.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun loadPreferences() {
        val notifications = preferences.getBoolean(
            "notifications",
            true
        )

        val darkMode = preferences.getBoolean(
            "darkMode",
            false
        )

        switchNotifications.isChecked = notifications
        switchDarkMode.isChecked = darkMode

        val savedLanguage = preferences.getString(
            "language",
            "English"
        ) ?: "English"

        val languages = resources.getStringArray(
            R.array.app_languages
        )

        val languagePosition = languages.indexOf(savedLanguage)

        if (languagePosition >= 0) {
            spinnerLanguage.setSelection(languagePosition, false)
        }
    }

    private fun setupListeners() {
        btnSaveProfile.setOnClickListener {
            saveProfile()
        }

        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            preferences.edit()
                .putBoolean("notifications", isChecked)
                .apply()

            val message = if (isChecked) {
                "Notifications enabled."
            } else {
                "Notifications disabled."
            }

            Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
            ).show()
        }

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            preferences.edit()
                .putBoolean("darkMode", isChecked)
                .apply()

            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) {
                    AppCompatDelegate.MODE_NIGHT_YES
                } else {
                    AppCompatDelegate.MODE_NIGHT_NO
                }
            )
        }

        spinnerLanguage.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedLanguage = parent?.getItemAtPosition(position).toString()

                    val currentSaved = preferences.getString("language", "English") ?: "English"
                    if (selectedLanguage == currentSaved) return

                    preferences.edit()
                        .putString("language", selectedLanguage)
                        .apply()

                    val langCode = LocaleHelper.getLanguageCode(selectedLanguage)

                    AppCompatDelegate.setApplicationLocales(
                        LocaleListCompat.forLanguageTags(langCode)
                    )

                    Toast.makeText(
                        this@SettingsActivity,
                        "$selectedLanguage selected.",
                        Toast.LENGTH_SHORT
                    ).show()

                    recreate()
                }

                override fun onNothingSelected(
                    parent: android.widget.AdapterView<*>?
                ) {}
            }

        btnSignOut.setOnClickListener {
            signOut()
        }

        btnBackDashboard.setOnClickListener {
            finish()
        }
    }

    private fun signOut() {
        auth.signOut()

        Toast.makeText(
            this,
            "You have signed out.",
            Toast.LENGTH_SHORT
        ).show()

        openLogin()
    }

    private fun openLogin() {
        val intent = Intent(
            this,
            MainActivity::class.java
        )

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }
}
