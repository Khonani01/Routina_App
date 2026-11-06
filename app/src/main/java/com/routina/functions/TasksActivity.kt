package com.routina.functions

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
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

class TasksActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var scrollViewTasks: ScrollView
    private lateinit var txtTotalTasks: TextView
    private lateinit var txtCompletedTasks: TextView
    private lateinit var edtTaskTitle: EditText
    private lateinit var edtTaskDescription: EditText
    private lateinit var btnSelectDate: Button
    private lateinit var spinnerPriority: Spinner
    private lateinit var btnAddTask: Button
    private lateinit var spinnerTaskFilter: Spinner
    private lateinit var taskContainer: LinearLayout
    private lateinit var txtEmptyTasks: TextView
    private lateinit var btnBackDashboard: Button

    private val tasksList = mutableListOf<Task>()

    private var selectedDueDate: String? = null

    data class Task(
        val id: String,
        val title: String,
        val description: String,
        val dueDate: String,
        val priority: String,
        val isCompleted: Boolean
    )

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tasks)

        auth = FirebaseAuth.getInstance()

        initializeViews()

        if (auth.currentUser == null) {
            Toast.makeText(this, "Please sign in first.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupButtons()
        setupFilter()
        loadTasks()
    }

    private fun initializeViews() {
        scrollViewTasks = findViewById(R.id.scrollViewTasks)
        txtTotalTasks = findViewById(R.id.txtTotalTasks)
        txtCompletedTasks = findViewById(R.id.txtCompletedTasks)
        edtTaskTitle = findViewById(R.id.edtTaskTitle)
        edtTaskDescription = findViewById(R.id.edtTaskDescription)
        btnSelectDate = findViewById(R.id.btnSelectDate)
        spinnerPriority = findViewById(R.id.spinnerPriority)
        btnAddTask = findViewById(R.id.btnAddTask)
        spinnerTaskFilter = findViewById(R.id.spinnerTaskFilter)
        taskContainer = findViewById(R.id.taskContainer)
        txtEmptyTasks = findViewById(R.id.txtEmptyTasks)
        btnBackDashboard = findViewById(R.id.btnBackDashboard)
    }

    private fun setupButtons() {
        btnAddTask.setOnClickListener {
            addTask()
        }

        btnSelectDate.setOnClickListener {
            showDateSelectionDialog()
        }

        btnBackDashboard.setOnClickListener {
            finish()
        }
    }

    private fun showDateSelectionDialog() {
        val options = arrayOf("Calendar Date Picker", "Natural Language (e.g., 'tomorrow', 'every Monday')")
        AlertDialog.Builder(this)
            .setTitle("Set Due Date / Reminder")
            .setItems(options) { _, which ->
                if (which == 0) {
                    showDatePicker()
                } else {
                    showNaturalLanguageDateInput()
                }
            }
            .show()
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                selectedDueDate = dateFormat.format(calendar.time)
                btnSelectDate.text = dateFormat.format(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showNaturalLanguageDateInput() {
        val inputEdit = EditText(this).apply {
            hint = "e.g. 'tomorrow', 'in 3 days', 'every Monday'"
            setPadding(32, 24, 32, 24)
        }

        AlertDialog.Builder(this)
            .setTitle("Enter Natural Language Reminder")
            .setMessage("Type phrases like 'today', 'tomorrow', 'in 3 days', or 'every Monday':")
            .setView(inputEdit)
            .setPositiveButton("Set") { _, _ ->
                val phrase = inputEdit.text.toString().trim()
                val parsed = parseNaturalLanguageDate(phrase)
                if (parsed != null) {
                    selectedDueDate = parsed.first
                    btnSelectDate.text = parsed.first + if (parsed.second) " (Recurring)" else ""
                    Toast.makeText(this, "Reminder set for: ${parsed.first}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        this,
                        "Unrecognized reminder date format. Try 'tomorrow', 'in 3 days', or 'every Monday'.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun parseNaturalLanguageDate(input: String): Pair<String, Boolean>? {
        if (input.isBlank()) return null
        val clean = input.trim().lowercase(Locale.getDefault())
        val cal = Calendar.getInstance()

        if (clean == "today") {
            return Pair(dateFormat.format(cal.time), false)
        }
        if (clean == "tomorrow") {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            return Pair(dateFormat.format(cal.time), false)
        }
        if (clean.startsWith("in ") && clean.contains("day")) {
            val parts = clean.split(" ")
            val num = parts.firstOrNull { it.toIntOrNull() != null }?.toIntOrNull()
            if (num != null && num > 0) {
                cal.add(Calendar.DAY_OF_YEAR, num)
                return Pair(dateFormat.format(cal.time), false)
            }
        }
        if (clean == "every day" || clean == "daily") {
            return Pair(dateFormat.format(cal.time), true)
        }

        val weekdays = mapOf(
            "monday" to Calendar.MONDAY,
            "tuesday" to Calendar.TUESDAY,
            "wednesday" to Calendar.WEDNESDAY,
            "thursday" to Calendar.THURSDAY,
            "friday" to Calendar.FRIDAY,
            "saturday" to Calendar.SATURDAY,
            "sunday" to Calendar.SUNDAY
        )

        for ((dayName, targetDay) in weekdays) {
            if (clean.contains(dayName)) {
                val isRecurring = clean.contains("every")
                while (cal.get(Calendar.DAY_OF_WEEK) != targetDay) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
                return Pair(dateFormat.format(cal.time), isRecurring)
            }
        }

        return try {
            val parsedDate = dateFormat.parse(input)
            if (parsedDate != null) Pair(dateFormat.format(parsedDate), false) else null
        } catch (e: Exception) {
            null
        }
    }

    // ADD TASK VIA REST API
    private fun addTask() {
        if (auth.currentUser == null) return

        val title = edtTaskTitle.text.toString().trim()
        val description = edtTaskDescription.text.toString().trim()
        val priority = spinnerPriority.selectedItem.toString()

        if (title.isEmpty()) {
            edtTaskTitle.error = getString(R.string.task_title_hint)
            edtTaskTitle.requestFocus()
            return
        }

        var finalDueDate = selectedDueDate ?: ""
        if (finalDueDate.isEmpty()) {
            val autoParsed = parseNaturalLanguageDate(title) ?: parseNaturalLanguageDate(description)
            if (autoParsed != null) {
                finalDueDate = autoParsed.first
            }
        }

        val req = TaskCreateRequest(
            title = title,
            name = title,
            description = description,
            dueDate = finalDueDate,
            priority = priority,
            isCompleted = false
        )

        btnAddTask.isEnabled = false

        ApiClient.apiService.createTask(req).enqueue(object : Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                btnAddTask.isEnabled = true
                if (response.isSuccessful) {
                    NotificationHelper.sendNotification(
                        this@TasksActivity,
                        "Task Added 📌",
                        "Task '$title' has been created successfully."
                    )
                    Toast.makeText(this@TasksActivity, "Task added successfully!", Toast.LENGTH_SHORT).show()
                    clearForm()
                    loadTasks()
                    scrollViewTasks.post { scrollViewTasks.fullScroll(View.FOCUS_DOWN) }
                } else {
                    Toast.makeText(this@TasksActivity, "Could not add task (${response.code()})", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                btnAddTask.isEnabled = true
                clearForm()
                loadTasks()
            }
        })
    }

    // LOAD TASKS VIA REST API
    private fun loadTasks() {
        if (auth.currentUser == null) return

        ApiClient.apiService.getTasks().enqueue(object : Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if (response.isSuccessful) {
                    tasksList.clear()
                    val dtos = ResponseParser.parseList(response.body(), TaskDto::class.java)
                    dtos.forEach { dto ->
                        tasksList.add(
                            Task(
                                id = dto.realId,
                                title = dto.taskTitle,
                                description = dto.description ?: "",
                                dueDate = dto.dueDate ?: "",
                                priority = dto.priority ?: "Medium",
                                isCompleted = dto.completed
                            )
                        )
                    }
                    updateSummary()
                    displayTasks()
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {}
        })
    }

    private fun setupFilter() {
        spinnerTaskFilter.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    displayTasks()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    private fun displayTasks() {
        taskContainer.removeAllViews()

        val selectedFilterPosition = spinnerTaskFilter.selectedItemPosition

        val filteredList = when (selectedFilterPosition) {
            1 -> tasksList.filter { !it.isCompleted }
            2 -> tasksList.filter { it.isCompleted }
            else -> tasksList
        }

        if (filteredList.isEmpty()) {
            txtEmptyTasks.visibility = View.VISIBLE
            txtEmptyTasks.text = getString(R.string.no_tasks_added)
            taskContainer.addView(txtEmptyTasks)
            return
        }

        txtEmptyTasks.visibility = View.GONE

        filteredList.forEach { task ->

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 18, 20, 18)
                setBackgroundResource(R.drawable.bg_auth_card)
                elevation = 3f
            }

            val topRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
            }

            val titleView = TextView(this).apply {
                text = task.title
                textSize = 16f
                setTextColor(ContextCompat.getColor(this@TasksActivity, R.color.text_primary))
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
                text = if (task.isCompleted) getString(R.string.completed) else getString(R.string.mark_as_completed)
                textSize = 13f
                isChecked = task.isCompleted
                buttonTintList = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this@TasksActivity, R.color.brand_primary)
                )

                setOnCheckedChangeListener { _, isChecked ->
                    updateTaskStatus(task.id, isChecked)
                }
            }

            topRow.addView(titleView)
            topRow.addView(checkBox)

            if (task.description.isNotEmpty()) {
                val descView = TextView(this).apply {
                    text = task.description
                    textSize = 13f
                    setTextColor(ContextCompat.getColor(this@TasksActivity, R.color.text_secondary))
                    setPadding(0, 6, 0, 6)
                }
                card.addView(topRow)
                card.addView(descView)
            } else {
                card.addView(topRow)
            }

            val detailsView = TextView(this).apply {
                val dueText = if (task.dueDate.isNotEmpty()) {
                    getString(R.string.due_date_label, task.dueDate)
                } else {
                    getString(R.string.no_due_date)
                }
                val prioText = getString(R.string.priority_label, task.priority)
                text = "$dueText  •  $prioText"
                textSize = 12f
                setTextColor(ContextCompat.getColor(this@TasksActivity, R.color.text_secondary))
                setPadding(0, 4, 0, 8)
            }

            val deleteButton = Button(this).apply {
                text = getString(R.string.delete_task)
                isAllCaps = false
                textSize = 12f
                setTextColor(ContextCompat.getColor(this@TasksActivity, R.color.signout_text))
                setOnClickListener {
                    confirmDelete(task)
                }
            }

            card.addView(detailsView)
            card.addView(deleteButton)

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            params.bottomMargin = 16

            taskContainer.addView(card, params)
        }
    }

    private fun updateTaskStatus(taskId: String, completed: Boolean) {
        if (auth.currentUser == null) return

        val updateMap = mapOf<String, Any>("isCompleted" to completed)

        ApiClient.apiService.updateTask(taskId, updateMap).enqueue(object : Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if (response.isSuccessful) {
                    if (completed) {
                        NotificationHelper.sendNotification(
                            this@TasksActivity,
                            "Task Completed 🎉",
                            "Great job completing your task!"
                        )
                    }
                    loadTasks()
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {}
        })
    }

    private fun updateSummary() {
        val total = tasksList.size
        val completed = tasksList.count { it.isCompleted }

        txtTotalTasks.text = total.toString()
        txtCompletedTasks.text = completed.toString()
    }

    private fun confirmDelete(task: Task) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.confirm_delete_task_title))
            .setMessage(getString(R.string.confirm_delete_task_msg, task.title))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                deleteTask(task.id)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun deleteTask(taskId: String) {
        if (auth.currentUser == null) return

        ApiClient.apiService.deleteTask(taskId).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful || response.code() == 204) {
                    Toast.makeText(this@TasksActivity, "Task deleted.", Toast.LENGTH_SHORT).show()
                    loadTasks()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@TasksActivity, "Could not delete task.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun clearForm() {
        edtTaskTitle.text.clear()
        edtTaskDescription.text.clear()

        selectedDueDate = null
        btnSelectDate.text = getString(R.string.select_due_date)

        spinnerPriority.setSelection(0)
    }
}
