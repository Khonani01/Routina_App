package com.routina.app.model

data class User(
    val id: String,
    val name: String,
    val email: String
)

data class AuthResponse(
    val user: User,
    val token: String
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class Habit(
    val id: String? = null,
    val title: String,
    val frequency: String = "daily",
    val goal: Int = 1,
    val progress: Int = 0,
    val streak: Int = 0,
    val createdAt: String? = null
)

data class HabitsResponse(
    val habits: List<Habit>
)

data class Settings(
    val darkMode: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val language: String = "en"
)

data class SettingsResponse(
    val settings: Settings
)
