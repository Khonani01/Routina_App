package com.routina.functions

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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

class NotesActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var scrollViewNotes: ScrollView
    private lateinit var edtSearchNotes: EditText
    private lateinit var edtNoteTitle: EditText
    private lateinit var edtNoteContent: EditText
    private lateinit var spinnerNoteColor: Spinner
    private lateinit var btnSaveNote: Button
    private lateinit var notesContainer: LinearLayout
    private lateinit var txtEmptyNotes: TextView
    private lateinit var btnBackDashboard: Button

    private val notesList = mutableListOf<Note>()

    private var editingNoteId: String? = null

    data class Note(
        val id: String,
        val title: String,
        val content: String,
        val color: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notes)

        auth = FirebaseAuth.getInstance()

        initializeViews()

        if (auth.currentUser == null) {
            Toast.makeText(this, "Please sign in first.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupSearch()
        setupButtons()
        loadNotes()
    }

    private fun initializeViews() {
        scrollViewNotes = findViewById(R.id.scrollViewNotes)
        edtSearchNotes = findViewById(R.id.edtSearchNotes)
        edtNoteTitle = findViewById(R.id.edtNoteTitle)
        edtNoteContent = findViewById(R.id.edtNoteContent)
        spinnerNoteColor = findViewById(R.id.spinnerNoteColor)
        btnSaveNote = findViewById(R.id.btnSaveNote)
        notesContainer = findViewById(R.id.notesContainer)
        txtEmptyNotes = findViewById(R.id.txtEmptyNotes)
        btnBackDashboard = findViewById(R.id.btnBackDashboard)
    }

    private fun setupButtons() {
        btnSaveNote.setOnClickListener {
            saveNote()
        }

        btnBackDashboard.setOnClickListener {
            finish()
        }
    }

    private fun saveNote() {
        if (auth.currentUser == null) return

        val title = edtNoteTitle.text.toString().trim()
        val content = edtNoteContent.text.toString().trim()
        val selectedColor = spinnerNoteColor.selectedItem.toString()

        if (title.isEmpty()) {
            edtNoteTitle.error = getString(R.string.note_title_hint)
            edtNoteTitle.requestFocus()
            return
        }

        if (content.isEmpty()) {
            edtNoteContent.error = getString(R.string.note_content_hint)
            edtNoteContent.requestFocus()
            return
        }

        val currentNoteId = editingNoteId
        btnSaveNote.isEnabled = false

        if (currentNoteId == null) {
            val req = NoteCreateRequest(title = title, content = content, color = selectedColor)
            ApiClient.apiService.createNote(req).enqueue(object : Callback<JsonElement> {
                override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                    btnSaveNote.isEnabled = true
                    if (response.isSuccessful) {
                        NotificationHelper.sendNotification(
                            this@NotesActivity,
                            "Note Saved 📝",
                            "Note '$title' saved successfully."
                        )
                        Toast.makeText(this@NotesActivity, "Note saved successfully!", Toast.LENGTH_SHORT).show()
                        clearForm()
                        loadNotes()
                        scrollViewNotes.post {
                            scrollViewNotes.fullScroll(View.FOCUS_DOWN)
                        }
                    } else {
                        Toast.makeText(this@NotesActivity, "Could not save note: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                    btnSaveNote.isEnabled = true
                    clearForm()
                    loadNotes()
                }
            })
        } else {
            val req = NoteUpdateRequest(title = title, content = content, color = selectedColor)
            ApiClient.apiService.updateNote(currentNoteId, req).enqueue(object : Callback<JsonElement> {
                override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                    btnSaveNote.isEnabled = true
                    if (response.isSuccessful) {
                        NotificationHelper.sendNotification(
                            this@NotesActivity,
                            "Note Updated ✏️",
                            "Note '$title' updated successfully."
                        )
                        Toast.makeText(this@NotesActivity, "Note updated successfully!", Toast.LENGTH_SHORT).show()
                        clearForm()
                        loadNotes()
                    } else {
                        Toast.makeText(this@NotesActivity, "Could not update note: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                    btnSaveNote.isEnabled = true
                    clearForm()
                    loadNotes()
                }
            })
        }
    }

    private fun setupSearch() {
        edtSearchNotes.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterNotes(s.toString().trim())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun loadNotes() {
        if (auth.currentUser == null) return

        ApiClient.apiService.getNotes().enqueue(object : Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if (response.isSuccessful) {
                    notesList.clear()
                    val dtos = ResponseParser.parseList(response.body(), NoteDto::class.java)
                    dtos.forEach { dto ->
                        notesList.add(
                            Note(
                                id = dto.realId,
                                title = dto.noteTitle,
                                content = dto.noteContent,
                                color = dto.color ?: "Blue"
                            )
                        )
                    }
                    filterNotes(edtSearchNotes.text.toString().trim())
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {}
        })
    }

    private fun filterNotes(query: String) {
        notesContainer.removeAllViews()

        val filtered = if (query.isEmpty()) {
            notesList
        } else {
            notesList.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.content.contains(query, ignoreCase = true)
            }
        }

        if (filtered.isEmpty()) {
            txtEmptyNotes.visibility = View.VISIBLE
            txtEmptyNotes.text = if (notesList.isEmpty()) {
                getString(R.string.no_notes_empty)
            } else {
                getString(R.string.no_notes_match)
            }
            notesContainer.addView(txtEmptyNotes)
            return
        }

        txtEmptyNotes.visibility = View.GONE

        filtered.forEach { note ->

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 18, 20, 18)
                setBackgroundColor(getCardColor(note.color))
            }

            val titleView = TextView(this).apply {
                text = note.title
                textSize = 16f
                setTextColor(ContextCompat.getColor(this@NotesActivity, R.color.text_primary))
                setTypeface(null, Typeface.BOLD)
            }

            val contentView = TextView(this).apply {
                text = note.content
                textSize = 14f
                setTextColor(ContextCompat.getColor(this@NotesActivity, R.color.text_secondary))
                setPadding(0, 8, 0, 12)
            }

            val actionsRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
            }

            val editBtn = Button(this).apply {
                text = getString(R.string.edit)
                isAllCaps = false
                textSize = 12f
                setOnClickListener {
                    populateForEdit(note)
                }
            }

            val deleteBtn = Button(this).apply {
                text = getString(R.string.delete)
                isAllCaps = false
                textSize = 12f
                setTextColor(ContextCompat.getColor(this@NotesActivity, R.color.signout_text))
                setOnClickListener {
                    deleteNote(note.id)
                }
            }

            actionsRow.addView(editBtn)
            actionsRow.addView(deleteBtn)

            card.addView(titleView)
            card.addView(contentView)
            card.addView(actionsRow)

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            params.bottomMargin = 16

            notesContainer.addView(card, params)
        }
    }

    private fun populateForEdit(note: Note) {
        editingNoteId = note.id
        edtNoteTitle.setText(note.title)
        edtNoteContent.setText(note.content)

        val colors = resources.getStringArray(R.array.note_colors)
        val index = colors.indexOf(note.color)
        if (index >= 0) {
            spinnerNoteColor.setSelection(index)
        }

        btnSaveNote.text = "Update Note"
        edtNoteTitle.requestFocus()
    }

    private fun deleteNote(noteId: String) {
        if (auth.currentUser == null) return

        ApiClient.apiService.deleteNote(noteId).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful || response.code() == 204) {
                    Toast.makeText(this@NotesActivity, "Note deleted.", Toast.LENGTH_SHORT).show()
                    if (editingNoteId == noteId) {
                        clearForm()
                    }
                    loadNotes()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@NotesActivity, "Could not delete note.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun clearForm() {
        editingNoteId = null
        edtNoteTitle.text.clear()
        edtNoteContent.text.clear()
        spinnerNoteColor.setSelection(0)
        btnSaveNote.text = "Save Note"
    }

    private fun getCardColor(colorName: String): Int {
        return when (colorName) {
            "Purple" -> Color.parseColor("#F3E8FF")
            "Green" -> Color.parseColor("#DCFCE7")
            "Yellow" -> Color.parseColor("#FEF9C3")
            "Pink" -> Color.parseColor("#FCE7F3")
            else -> Color.parseColor("#EFF6FF") // Blue default
        }
    }
}
