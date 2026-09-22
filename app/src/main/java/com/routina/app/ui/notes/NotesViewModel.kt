package com.routina.app.ui.notes

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.routina.app.model.Note
import java.text.SimpleDateFormat
import java.util.*

class NotesViewModel : ViewModel() {
    private val _notes = mutableStateListOf<Note>()
    val notes: List<Note> = _notes

    init {
        _notes.add(Note(id = "1", title = "Welcome to Notes", content = "This is your notes feature", createdAt = getCurrentDate()))
    }

    fun addNote(title: String, content: String) {
        if (title.isBlank() && content.isBlank()) return
        val newNote = Note(
            id = UUID.randomUUID().toString(),
            title = title.ifBlank { "Untitled" },
            content = content,
            createdAt = getCurrentDate()
        )
        _notes.add(0, newNote)
    }

    fun deleteNote(noteId: String) {
        _notes.removeAll { it.id == noteId }
    }

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(Date())
    }
}