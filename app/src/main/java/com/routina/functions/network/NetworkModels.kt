package com.routina.functions.network

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import java.util.UUID

// SSO AUTH MODELS
data class SsoRequest(
    @SerializedName("idToken") val idToken: String
)

data class SsoResponse(
    @SerializedName("token") val token: String? = null,
    @SerializedName("user") val user: Any? = null
)

// PUSH NOTIFICATION MODEL
data class RegisterTokenRequest(
    @SerializedName("token") val token: String
)

// TASKS MODELS
data class TaskDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("_id") val mongoId: String? = null,
    @SerializedName("title") val title: String? = "",
    @SerializedName("name") val nameAlt: String? = "",
    @SerializedName("description") val description: String? = "",
    @SerializedName("dueDate") val dueDate: String? = "",
    @SerializedName("priority") val priority: String? = "Medium",
    @SerializedName("isCompleted") val isCompleted: Boolean? = null,
    @SerializedName("completed") val completedAlt: Boolean? = null,
    @SerializedName("clientId") val clientId: String? = null
) {
    val realId: String
        get() = if (!id.isNullOrEmpty()) id else mongoId ?: ""

    val taskTitle: String
        get() = if (!title.isNullOrEmpty()) title else nameAlt ?: ""

    val completed: Boolean
        get() = isCompleted ?: completedAlt ?: false
}

data class TaskCreateRequest(
    @SerializedName("title") val title: String,
    @SerializedName("name") val name: String = title,
    @SerializedName("description") val description: String = "",
    @SerializedName("dueDate") val dueDate: String = "",
    @SerializedName("priority") val priority: String = "Medium",
    @SerializedName("isCompleted") val isCompleted: Boolean = false,
    @SerializedName("clientId") val clientId: String = UUID.randomUUID().toString()
)

data class TaskStatusUpdateRequest(
    val isCompleted: Boolean
)

// HABITS MODELS
data class HabitDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("_id") val mongoId: String? = null,
    @SerializedName("name") val name: String? = "",
    @SerializedName("title") val titleAlt: String? = "",
    @SerializedName("frequency") val frequency: String? = "Daily",
    @SerializedName("completedDates") val completedDates: List<String>? = emptyList(),
    @SerializedName("clientId") val clientId: String? = null
) {
    val realId: String
        get() = if (!id.isNullOrEmpty()) id else mongoId ?: ""

    val habitName: String
        get() = when {
            !name.isNullOrEmpty() -> name
            !titleAlt.isNullOrEmpty() -> titleAlt
            else -> ""
        }
}

data class HabitCreateRequest(
    @SerializedName("title") val title: String,
    @SerializedName("name") val name: String = title,
    @SerializedName("frequency") val frequency: String = "Daily",
    @SerializedName("completedDates") val completedDates: List<String>? = null,
    @SerializedName("clientId") val clientId: String? = UUID.randomUUID().toString()
)

data class HabitUpdateRequest(
    @SerializedName("title") val title: String? = null,
    @SerializedName("name") val name: String? = title,
    @SerializedName("frequency") val frequency: String? = null,
    @SerializedName("completedDates") val completedDates: List<String>? = null
)

// NOTES MODELS
data class NoteDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("_id") val mongoId: String? = null,
    @SerializedName("title") val title: String? = "",
    @SerializedName("name") val nameAlt: String? = "",
    @SerializedName("content") val content: String? = "",
    @SerializedName("description") val descriptionAlt: String? = "",
    @SerializedName("color") val color: String? = "Blue",
    @SerializedName("clientId") val clientId: String? = null
) {
    val realId: String
        get() = if (!id.isNullOrEmpty()) id else mongoId ?: ""

    val noteTitle: String
        get() = if (!title.isNullOrEmpty()) title else nameAlt ?: ""

    val noteContent: String
        get() = if (!content.isNullOrEmpty()) content else descriptionAlt ?: ""
}

data class NoteCreateRequest(
    @SerializedName("title") val title: String,
    @SerializedName("name") val name: String = title,
    @SerializedName("content") val content: String,
    @SerializedName("description") val description: String = content,
    @SerializedName("color") val color: String = "Blue",
    @SerializedName("clientId") val clientId: String = UUID.randomUUID().toString()
)

data class NoteUpdateRequest(
    @SerializedName("title") val title: String? = null,
    @SerializedName("name") val name: String? = title,
    @SerializedName("content") val content: String? = null,
    @SerializedName("description") val description: String? = content,
    @SerializedName("color") val color: String? = null
)

// USER PROFILE & DEVICE TOKEN MODELS
data class UserProfileDto(
    @SerializedName("uid") val uid: String? = null,
    @SerializedName("displayName") val displayName: String? = null,
    @SerializedName("email") val email: String? = null
)

data class UserProfileUpdateRequest(
    val displayName: String? = null,
    val email: String? = null
)

data class DeviceTokenRequest(
    val deviceToken: String,
    val platform: String = "android"
)

// HELPER PARSER FUNCTION FOR FLEXIBLE LIST & OBJECT RESPONSES
object ResponseParser {
    private val gson = Gson()

    fun <T> parseList(jsonElement: JsonElement?, clazz: Class<T>): List<T> {
        if (jsonElement == null || jsonElement.isJsonNull) return emptyList()
        val list = mutableListOf<T>()

        if (jsonElement.isJsonArray) {
            jsonElement.asJsonArray.forEach { elem ->
                try {
                    val item = gson.fromJson(elem, clazz)
                    if (item != null) list.add(item)
                } catch (_: Exception) {}
            }
        } else if (jsonElement.isJsonObject) {
            val obj = jsonElement.asJsonObject
            val array = when {
                obj.has("tasks") && obj.get("tasks").isJsonArray -> obj.getAsJsonArray("tasks")
                obj.has("habits") && obj.get("habits").isJsonArray -> obj.getAsJsonArray("habits")
                obj.has("notes") && obj.get("notes").isJsonArray -> obj.getAsJsonArray("notes")
                obj.has("data") && obj.get("data").isJsonArray -> obj.getAsJsonArray("data")
                obj.has("items") && obj.get("items").isJsonArray -> obj.getAsJsonArray("items")
                obj.has("result") && obj.get("result").isJsonArray -> obj.getAsJsonArray("result")
                else -> null
            }

            if (array != null) {
                array.forEach { elem ->
                    try {
                        val item = gson.fromJson(elem, clazz)
                        if (item != null) list.add(item)
                    } catch (_: Exception) {}
                }
            } else {
                try {
                    val singleItem = gson.fromJson(jsonElement, clazz)
                    if (singleItem != null) list.add(singleItem)
                } catch (_: Exception) {}
            }
        }

        return list
    }
}
