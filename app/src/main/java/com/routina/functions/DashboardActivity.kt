package com.routina.functions

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.JsonElement
import com.routina.functions.network.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DashboardActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth

    // Dashboard components
    private lateinit var txtGreeting: TextView
    private lateinit var txtDate: TextView
    private lateinit var btnProfile: TextView

    private lateinit var txtProgress: TextView
    private lateinit var progressDaily: ProgressBar

    private lateinit var txtTaskCount: TextView
    private lateinit var txtHabitCount: TextView

    private lateinit var todayTasksContainer: LinearLayout
    private lateinit var txtTaskEmpty: TextView

    // Navigation buttons
    private lateinit var btnSeeTasks: TextView
    private lateinit var btnTasks: Button
    private lateinit var btnHabits: Button
    private lateinit var btnNotes: Button
    private lateinit var btnSignOut: Button

    // Dashboard totals
    private var totalTasks = 0
    private var completedTasks = 0
    private var totalHabits = 0
    private var completedHabits = 0

    private data class DashboardTask(
        val id: String,
        val title: String,
        val description: String,
        val dueDate: String,
        val priority: String,
        val isCompleted: Boolean
    )

    private val todayStr: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null) {
            openLogin()
            return
        }

        NotificationHelper.createNotificationChannel(this)
        requestNotificationPermission()

        initializeViews()
        setupNavigation()
        monitorNetworkStatus()
    }

    override fun onStart() {
        super.onStart()

        if (auth.currentUser == null) {
            openLogin()
            return
        }

        displayGreeting()
        displayDate()
        displayUserInitials()

        loadTasks()
        loadHabits()
    }

    private fun initializeViews() {
        txtGreeting = findViewById(R.id.txtGreeting)
        txtDate = findViewById(R.id.txtDate)
        btnProfile = findViewById(R.id.btnProfile)

        txtProgress = findViewById(R.id.txtProgress)
        progressDaily = findViewById(R.id.progressDaily)

        txtTaskCount = findViewById(R.id.txtTaskCount)
        txtHabitCount = findViewById(R.id.txtHabitCount)

        todayTasksContainer = findViewById(R.id.todayTasksContainer)
        txtTaskEmpty = findViewById(R.id.txtTaskEmpty)

        btnSeeTasks = findViewById(R.id.btnSeeTasks)
        btnTasks = findViewById(R.id.btnTasks)
        btnHabits = findViewById(R.id.btnHabits)
        btnNotes = findViewById(R.id.btnNotes)
        btnSignOut = findViewById(R.id.btnSignOut)
    }

    private fun monitorNetworkStatus() {
        try {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            connectivityManager.registerNetworkCallback(networkRequest, object : ConnectivityManager.NetworkCallback() {
                override fun onLost(network: Network) {
                    runOnUiThread {
                        Toast.makeText(
                            this@DashboardActivity,
                            "Offline Mode: Changes will sync when reconnected.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onAvailable(network: Network) {
                    runOnUiThread {
                        Toast.makeText(
                            this@DashboardActivity,
                            "Online: Synced with cloud database.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            })
        } catch (e: Exception) {
            // Ignore network callback error
        }
    }

    private fun displayGreeting() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        val greeting = when (hour) {
            in 0..11 -> getString(R.string.good_morning)
            in 12..16 -> getString(R.string.good_afternoon)
            else -> getString(R.string.good_evening)
        }

        val userName = auth.currentUser?.displayName
            ?.substringBefore(" ")
            ?.takeIf { it.isNotBlank() }

        txtGreeting.text = if (userName != null) {
            "$greeting, $userName"
        } else {
            greeting
        }
    }

    private fun displayDate() {
        val prefs = getSharedPreferences("RoutinaSettings", MODE_PRIVATE)
        val langName = prefs.getString("language", "English") ?: "English"
        val langCode = LocaleHelper.getLanguageCode(langName)
        val locale = Locale.forLanguageTag(langCode)

        val dateFormat = SimpleDateFormat(
            "EEEE, dd MMMM yyyy",
            locale
        )
        txtDate.text = dateFormat.format(Calendar.getInstance().time)
    }

    private fun displayUserInitials() {
        val name = auth.currentUser?.displayName
            ?.trim()
            .orEmpty()

        val initials = if (name.isNotEmpty()) {
            name.split("\\s+".toRegex())
                .take(2)
                .mapNotNull { it.firstOrNull()?.uppercase() }
                .joinToString("")
        } else {
            auth.currentUser?.email
                ?.firstOrNull()
                ?.uppercase()
                ?: "U"
        }

        btnProfile.text = initials
    }

    // Load tasks from REST API
    private fun loadTasks() {
        if (auth.currentUser == null) return

        txtTaskCount.text = "..."

        ApiClient.apiService.getTasks().enqueue(object : Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if (response.isSuccessful) {
                    val dtos = ResponseParser.parseList(response.body(), TaskDto::class.java)
                    val allTasks = dtos.map { dto ->
                        DashboardTask(
                            id = dto.realId,
                            title = dto.taskTitle,
                            description = dto.description ?: "",
                            dueDate = dto.dueDate ?: "",
                            priority = dto.priority ?: "Medium",
                            isCompleted = dto.completed
                        )
                    }

                    totalTasks = allTasks.size
                    completedTasks = allTasks.count { it.isCompleted }

                    txtTaskCount.text = totalTasks.toString()
                    updateProgress()

                    val todayTasks = allTasks.filter { task ->
                        task.dueDate == todayStr || task.dueDate.isEmpty() || (task.dueDate < todayStr && !task.isCompleted)
                    }

                    displayTodayTasks(todayTasks)
                } else {
                    txtTaskCount.text = "0"
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                txtTaskCount.text = "0"
            }
        })
    }

    private fun displayTodayTasks(tasks: List<DashboardTask>) {
        todayTasksContainer.removeAllViews()

        if (tasks.isEmpty()) {
            txtTaskEmpty.visibility = View.VISIBLE
            txtTaskEmpty.text = if (totalTasks == 0) {
                getString(R.string.no_tasks_added)
            } else {
                getString(R.string.no_tasks_today)
            }
            todayTasksContainer.addView(txtTaskEmpty)
            return
        }

        txtTaskEmpty.visibility = View.GONE

        tasks.forEach { task ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 16, 20, 16)
                setBackgroundResource(R.drawable.bg_auth_card)
                elevation = 2f
            }

            val titleRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val titleView = TextView(this).apply {
                text = task.title
                textSize = 15f
                setTextColor(ContextCompat.getColor(this@DashboardActivity, R.color.text_primary))
                setTypeface(null, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )

                if (task.isCompleted) {
                    paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                }
            }

            val checkBox = CheckBox(this).apply {
                isChecked = task.isCompleted
                buttonTintList = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this@DashboardActivity, R.color.brand_primary)
                )

                setOnCheckedChangeListener { _, isChecked ->
                    updateTaskStatus(task.id, isChecked)
                }
            }

            titleRow.addView(titleView)
            titleRow.addView(checkBox)
            card.addView(titleRow)

            if (task.dueDate.isNotEmpty() || task.priority.isNotEmpty()) {
                val detailsView = TextView(this).apply {
                    val dueText = if (task.dueDate.isNotEmpty()) {
                        getString(R.string.due_date_label, task.dueDate)
                    } else {
                        getString(R.string.no_due_date)
                    }
                    val prioText = getString(R.string.priority_label, task.priority)
                    text = "$dueText  •  $prioText"
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(this@DashboardActivity, R.color.text_secondary))
                    setPadding(0, 4, 0, 0)
                }
                card.addView(detailsView)
            }

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 12
            }

            todayTasksContainer.addView(card, params)
        }
    }

    private fun updateTaskStatus(taskId: String, completed: Boolean) {
        if (auth.currentUser == null) return
        val map = mapOf<String, Any>("isCompleted" to completed)

        ApiClient.apiService.updateTask(taskId, map).enqueue(object : Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if (response.isSuccessful) {
                    loadTasks()
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {}
        })
    }

    // Load habits from REST API
    private fun loadHabits() {
        if (auth.currentUser == null) return

        txtHabitCount.text = "..."

        ApiClient.apiService.getHabits().enqueue(object : Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if (response.isSuccessful) {
                    val habits = ResponseParser.parseList(response.body(), HabitDto::class.java)
                    totalHabits = habits.size
                    completedHabits = habits.count { dto ->
                        dto.completedDates?.contains(todayStr) == true
                    }

                    txtHabitCount.text = totalHabits.toString()
                    updateProgress()
                } else {
                    txtHabitCount.text = "0"
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                txtHabitCount.text = "0"
            }
        })
    }

    private fun updateProgress() {
        val overallTotal = totalTasks + totalHabits
        val overallCompleted = completedTasks + completedHabits

        val percentage = if (overallTotal > 0) {
            (overallCompleted * 100) / overallTotal
        } else {
            0
        }

        txtProgress.text = getString(R.string.progress_completed, percentage)
        progressDaily.progress = percentage
    }

    private fun setupNavigation() {
        btnTasks.setOnClickListener {
            startActivity(Intent(this, TasksActivity::class.java))
        }

        btnSeeTasks.setOnClickListener {
            startActivity(Intent(this, TasksActivity::class.java))
        }

        btnHabits.setOnClickListener {
            startActivity(Intent(this, HabitsActivity::class.java))
        }

        btnNotes.setOnClickListener {
            startActivity(Intent(this, NotesActivity::class.java))
        }

        btnProfile.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        btnSignOut.setOnClickListener {
            signOut()
        }
    }

    private fun signOut() {
        auth.signOut()
        Toast.makeText(this, "You have signed out successfully.", Toast.LENGTH_SHORT).show()
        openLogin()
    }

    private fun openLogin() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }
}
