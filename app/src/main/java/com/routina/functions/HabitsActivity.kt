package com.routina.functions

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Typeface
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

class HabitsActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var scrollViewHabits: ScrollView
    private lateinit var txtHabitProgress: TextView
    private lateinit var progressHabits: ProgressBar
    private lateinit var edtHabitName: EditText
    private lateinit var spinnerHabitFrequency: Spinner
    private lateinit var btnAddHabit: Button
    private lateinit var habitContainer: LinearLayout
    private lateinit var txtEmptyHabits: TextView
    private lateinit var btnBackDashboard: Button

    private val habitsList = mutableListOf<Habit>()

    data class Habit(
        val id: String,
        val name: String,
        val frequency: String,
        val completedDates: List<String>
    )

    private val today: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_habits)

        auth = FirebaseAuth.getInstance()

        initializeViews()

        if (auth.currentUser == null) {
            Toast.makeText(this, "Please sign in first.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupButtons()
        loadHabits()
    }

    private fun initializeViews() {
        scrollViewHabits = findViewById(R.id.scrollViewHabits)
        txtHabitProgress = findViewById(R.id.txtHabitProgress)
        progressHabits = findViewById(R.id.progressHabits)
        edtHabitName = findViewById(R.id.edtHabitName)
        spinnerHabitFrequency = findViewById(R.id.spinnerHabitFrequency)
        btnAddHabit = findViewById(R.id.btnAddHabit)
        habitContainer = findViewById(R.id.habitContainer)
        txtEmptyHabits = findViewById(R.id.txtEmptyHabits)
        btnBackDashboard = findViewById(R.id.btnBackDashboard)
    }

    private fun setupButtons() {
        btnAddHabit.setOnClickListener {
            addHabit()
        }

        btnBackDashboard.setOnClickListener {
            finish()
        }
    }

    private fun addHabit() {
        if (auth.currentUser == null) return

        val name = edtHabitName.text.toString().trim()
        val frequency = spinnerHabitFrequency.selectedItem.toString()

        if (name.isEmpty()) {
            edtHabitName.error = getString(R.string.habit_name_hint)
            edtHabitName.requestFocus()
            return
        }

        val req = HabitCreateRequest(title = name, name = name, frequency = frequency)
        btnAddHabit.isEnabled = false

        ApiClient.apiService.createHabit(req).enqueue(object : Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                btnAddHabit.isEnabled = true
                if (response.isSuccessful) {
                    NotificationHelper.sendNotification(
                        this@HabitsActivity,
                        "Habit Added ⭐",
                        "Habit '$name' added to your daily routine."
                    )
                    edtHabitName.text.clear()
                    spinnerHabitFrequency.setSelection(0)
                    Toast.makeText(this@HabitsActivity, "Habit added successfully!", Toast.LENGTH_SHORT).show()
                    loadHabits()
                    scrollViewHabits.post {
                        scrollViewHabits.fullScroll(View.FOCUS_DOWN)
                    }
                } else {
                    Toast.makeText(this@HabitsActivity, "Could not add habit: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                btnAddHabit.isEnabled = true
                edtHabitName.text.clear()
                loadHabits()
            }
        })
    }

    private fun loadHabits() {
        if (auth.currentUser == null) return

        ApiClient.apiService.getHabits().enqueue(object : Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if (response.isSuccessful) {
                    habitsList.clear()
                    val dtos = ResponseParser.parseList(response.body(), HabitDto::class.java)
                    dtos.forEach { dto ->
                        habitsList.add(
                            Habit(
                                id = dto.realId,
                                name = dto.habitName,
                                frequency = dto.frequency ?: "Daily",
                                completedDates = dto.completedDates ?: emptyList()
                            )
                        )
                    }
                    displayHabits()
                    updateProgress()
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {}
        })
    }

    private fun calculateStreak(completedDates: List<String>): Int {
        if (completedDates.isEmpty()) return 0
        val sortedDates = completedDates.distinct().sortedDescending()
        var streak = 0
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()

        var checkDate = sdf.format(cal.time)
        if (!sortedDates.contains(checkDate)) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
            checkDate = sdf.format(cal.time)
            if (!sortedDates.contains(checkDate)) {
                return 0
            }
        }

        while (sortedDates.contains(checkDate)) {
            streak++
            cal.add(Calendar.DAY_OF_YEAR, -1)
            checkDate = sdf.format(cal.time)
        }
        return streak
    }

    private fun displayHabits() {
        habitContainer.removeAllViews()

        if (habitsList.isEmpty()) {
            txtEmptyHabits.visibility = View.VISIBLE
            txtEmptyHabits.text = getString(R.string.no_habits_empty)
            habitContainer.addView(txtEmptyHabits)
            return
        }

        txtEmptyHabits.visibility = View.GONE

        habitsList.forEach { habit ->

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 18, 20, 18)
                setBackgroundResource(R.drawable.bg_auth_card)
                elevation = 3f
            }

            val topRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val nameView = TextView(this).apply {
                text = habit.name
                textSize = 16f
                setTextColor(ContextCompat.getColor(this@HabitsActivity, R.color.text_primary))
                setTypeface(null, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            val checkBox = CheckBox(this).apply {
                text = if (habit.completedDates.contains(today)) getString(R.string.completed) else getString(R.string.mark_as_completed)
                textSize = 13f
                isChecked = habit.completedDates.contains(today)
                buttonTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(this@HabitsActivity, R.color.brand_purple)
                )

                setOnCheckedChangeListener { _, isChecked ->
                    updateHabitCompletion(habit, isChecked)
                }
            }

            topRow.addView(nameView)
            topRow.addView(checkBox)

            val streak = calculateStreak(habit.completedDates)
            val frequencyView = TextView(this).apply {
                text = "${getString(R.string.frequency)}: ${habit.frequency}  •  Streak: 🔥 $streak days  •  Total: ${habit.completedDates.size} days"
                textSize = 12f
                setTextColor(ContextCompat.getColor(this@HabitsActivity, R.color.text_secondary))
                setPadding(0, 8, 0, 8)
            }

            val deleteButton = Button(this).apply {
                text = getString(R.string.delete_habit)
                isAllCaps = false
                textSize = 12f
                setTextColor(ContextCompat.getColor(this@HabitsActivity, R.color.signout_text))
                setOnClickListener {
                    confirmDeleteHabit(habit)
                }
            }

            card.addView(topRow)
            card.addView(frequencyView)
            card.addView(deleteButton)

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            params.bottomMargin = 16

            habitContainer.addView(card, params)
        }
    }

    private fun updateHabitCompletion(habit: Habit, completed: Boolean) {
        if (auth.currentUser == null) return

        val updatedDates = habit.completedDates.toMutableList()
        if (completed) {
            if (!updatedDates.contains(today)) updatedDates.add(today)
        } else {
            updatedDates.remove(today)
        }

        val req = HabitUpdateRequest(title = habit.name, name = habit.name, frequency = habit.frequency, completedDates = updatedDates)

        ApiClient.apiService.updateHabit(habit.id, req).enqueue(object : Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if (response.isSuccessful) {
                    if (completed) {
                        NotificationHelper.sendNotification(
                            this@HabitsActivity,
                            "Habit Progress 💪",
                            "Awesome job completing '${habit.name}' today!"
                        )
                    }
                    loadHabits()
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {}
        })
    }

    private fun updateProgress() {
        val total = habitsList.size

        val completed = habitsList.count {
            it.completedDates.contains(today)
        }

        txtHabitProgress.text = getString(R.string.habits_completed_fmt, completed, total)

        val percentage = if (total > 0) {
            (completed * 100) / total
        } else {
            0
        }

        progressHabits.progress = percentage
    }

    private fun confirmDeleteHabit(habit: Habit) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.confirm_delete_habit_title))
            .setMessage(getString(R.string.confirm_delete_habit_msg, habit.name))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                deleteHabit(habit.id)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun deleteHabit(habitId: String) {
        if (auth.currentUser == null) return

        ApiClient.apiService.deleteHabit(habitId).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful || response.code() == 204) {
                    Toast.makeText(this@HabitsActivity, "Habit deleted.", Toast.LENGTH_SHORT).show()
                    loadHabits()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@HabitsActivity, "Could not delete habit.", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
