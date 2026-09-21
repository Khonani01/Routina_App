package com.routina.app.ui.habits

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.routina.app.R
import com.routina.app.adapter.HabitAdapter
import com.routina.app.databinding.ActivityHabitsBinding
import com.routina.app.databinding.DialogAddHabitBinding
import com.routina.app.model.Habit
import com.routina.app.network.RetrofitClient
import com.routina.app.network.SessionManager
import com.routina.app.ui.login.LoginActivity
import com.routina.app.ui.settings.SettingsActivity
import com.routina.app.util.HabitUtils
import kotlinx.coroutines.launch

/**
 * Landing screen after login.
 *
 * Responsibilities:
 * - List the user's habits with streak and progress.
 * - Let the user mark a habit as complete.
 * - Add new habits.
 * - Edit existing habits.
 * - Delete habits.
 * - Refresh the habit list.
 */
class HabitsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHabitsBinding
    private lateinit var session: SessionManager
    private lateinit var adapter: HabitAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHabitsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)

        if (!session.isLoggedIn()) {
            redirectToLogin()
            return
        }

        setupToolbar()
        setupRecyclerView()
        setupRefresh()
        setupAddHabitButton()

        loadHabits()
    }

    override fun onResume() {
        super.onResume()

        if (::session.isInitialized && session.isLoggedIn()) {
            loadHabits()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)

        binding.toolbar.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.action_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }

                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = HabitAdapter(
            onToggleComplete = { habit ->
                toggleHabitComplete(habit)
            },
            onEditHabit = { habit ->
                showEditHabitDialog(habit)
            },
            onDeleteHabit = { habit ->
                confirmDeleteHabit(habit)
            }
        )

        binding.habitsRecyclerView.layoutManager =
            LinearLayoutManager(this)

        binding.habitsRecyclerView.adapter = adapter
    }

    private fun setupRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            loadHabits()
        }
    }

    private fun setupAddHabitButton() {
        binding.addHabitFab.setOnClickListener {
            showAddHabitDialog()
        }
    }

    private fun loadHabits() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getHabits(bearerToken())

                if (response.isSuccessful && response.body() != null) {
                    val habits = response.body()!!.habits

                    adapter.submitList(habits)

                    binding.emptyStateText.visibility =
                        if (habits.isEmpty()) View.VISIBLE else View.GONE
                } else if (response.code() == 401) {
                    redirectToLogin()
                } else {
                    Toast.makeText(
                        this@HabitsActivity,
                        "Could not load habits: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@HabitsActivity,
                    "Network error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun toggleHabitComplete(habit: Habit) {
        val newProgress = HabitUtils.nextProgress(
            currentProgress = habit.progress,
            goal = habit.goal
        )

        val newStreak = HabitUtils.nextStreak(
            currentStreak = habit.streak,
            currentProgress = habit.progress,
            goal = habit.goal
        )

        lifecycleScope.launch {
            try {
                val id = habit.id

                if (id.isNullOrBlank()) {
                    Toast.makeText(
                        this@HabitsActivity,
                        "Habit ID is missing",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadHabits()
                    return@launch
                }

                val response = RetrofitClient.api.updateHabit(
                    bearerToken(),
                    id,
                    mapOf(
                        "progress" to newProgress,
                        "streak" to newStreak
                    )
                )

                if (response.isSuccessful) {
                    loadHabits()
                } else if (response.code() == 401) {
                    redirectToLogin()
                } else {
                    Toast.makeText(
                        this@HabitsActivity,
                        "Could not update habit: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()

                    loadHabits()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@HabitsActivity,
                    "Network error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()

                loadHabits()
            }
        }
    }

    private fun showAddHabitDialog() {
        val dialogBinding =
            DialogAddHabitBinding.inflate(layoutInflater)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Add Habit")
            .setView(dialogBinding.root)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Add", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(
                android.app.AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {

                val title = dialogBinding.habitTitleInput.text
                    ?.toString()
                    ?.trim()
                    .orEmpty()

                val goalText = dialogBinding.habitGoalInput.text
                    ?.toString()
                    ?.trim()
                    .orEmpty()

                val goal = goalText.toIntOrNull() ?: 1

                if (title.isBlank()) {
                    dialogBinding.habitTitleInput.error =
                        "Habit name is required"
                    return@setOnClickListener
                }

                if (goal <= 0) {
                    dialogBinding.habitGoalInput.error =
                        "Goal must be at least 1"
                    return@setOnClickListener
                }

                dialog.dismiss()
                createHabit(title, goal)
            }
        }

        dialog.show()
    }

    private fun createHabit(title: String, goal: Int) {
        lifecycleScope.launch {
            try {
                val habit = Habit(
                    title = title,
                    goal = goal
                )

                val response = RetrofitClient.api.createHabit(
                    bearerToken(),
                    habit
                )

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@HabitsActivity,
                        "Habit added",
                        Toast.LENGTH_SHORT
                    ).show()

                    loadHabits()
                } else if (response.code() == 401) {
                    redirectToLogin()
                } else {
                    Toast.makeText(
                        this@HabitsActivity,
                        "Could not add habit: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@HabitsActivity,
                    "Network error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showEditHabitDialog(habit: Habit) {
        val dialogBinding =
            DialogAddHabitBinding.inflate(layoutInflater)

        dialogBinding.habitTitleInput.setText(habit.title)
        dialogBinding.habitGoalInput.setText(habit.goal.toString())

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Edit Habit")
            .setView(dialogBinding.root)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(
                android.app.AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {

                val title = dialogBinding.habitTitleInput.text
                    ?.toString()
                    ?.trim()
                    .orEmpty()

                val goalText = dialogBinding.habitGoalInput.text
                    ?.toString()
                    ?.trim()
                    .orEmpty()

                val goal = goalText.toIntOrNull() ?: habit.goal

                if (title.isBlank()) {
                    dialogBinding.habitTitleInput.error =
                        "Habit name is required"
                    return@setOnClickListener
                }

                if (goal <= 0) {
                    dialogBinding.habitGoalInput.error =
                        "Goal must be at least 1"
                    return@setOnClickListener
                }

                dialog.dismiss()
                updateHabit(habit, title, goal)
            }
        }

        dialog.show()
    }

    private fun updateHabit(
        habit: Habit,
        title: String,
        goal: Int
    ) {
        lifecycleScope.launch {
            try {
                val id = habit.id

                if (id.isNullOrBlank()) {
                    Toast.makeText(
                        this@HabitsActivity,
                        "Habit ID is missing",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                val response = RetrofitClient.api.updateHabit(
                    bearerToken(),
                    id,
                    mapOf(
                        "title" to title,
                        "goal" to goal
                    )
                )

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@HabitsActivity,
                        "Habit updated",
                        Toast.LENGTH_SHORT
                    ).show()

                    loadHabits()
                } else if (response.code() == 401) {
                    redirectToLogin()
                } else {
                    Toast.makeText(
                        this@HabitsActivity,
                        "Could not update habit: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@HabitsActivity,
                    "Network error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun confirmDeleteHabit(habit: Habit) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Habit")
            .setMessage(
                "Are you sure you want to delete \"${habit.title}\"?"
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                deleteHabit(habit)
            }
            .show()
    }

    private fun deleteHabit(habit: Habit) {
        lifecycleScope.launch {
            try {
                val id = habit.id

                if (id.isNullOrBlank()) {
                    Toast.makeText(
                        this@HabitsActivity,
                        "Habit ID is missing",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                val response = RetrofitClient.api.deleteHabit(
                    bearerToken(),
                    id
                )

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@HabitsActivity,
                        "Habit deleted",
                        Toast.LENGTH_SHORT
                    ).show()

                    loadHabits()
                } else if (response.code() == 401) {
                    redirectToLogin()
                } else {
                    Toast.makeText(
                        this@HabitsActivity,
                        "Could not delete habit: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@HabitsActivity,
                    "Network error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun bearerToken(): String {
        return "Bearer ${session.token}"
    }

    private fun redirectToLogin() {
        session.clear()

        startActivity(
            Intent(this, LoginActivity::class.java)
        )

        finish()
    }
}
