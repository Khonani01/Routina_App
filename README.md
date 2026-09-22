# Routina

A student productivity app for Android combining habit tracking, notes, and task/reminder management.

Built for OPSC6312 (Open Source Coding, Intermediate) — Rosebank College Pretoria.

## Team

| Name                     | Student Number | Role            |
| ------------------------ | -------------- | --------------- |
| Khonani Mutobvu          | ST10439622     | Project Manager |
| Ofentse Nyiko Mashigoane | ST10456959     | Developer       |
| Vukosi Sono              | ST10453078     | Developer       |
| Charity Taulene          | ST10438951     | Developer       |
| Khumbelo Tshikororo      | ST10441369     | Developer       |

## Tech Stack

- **Frontend:** Android (Kotlin, Android Studio)
- **Backend:** Node.js / Express
- **Database:** Firebase Firestore
- **Hosting:** Render

## Project Structure

Routina_App/
├── api/ # Node/Express REST API
└── app/ # Android application

## Setup Instructions

_To be completed once the API and app skeletons are built._

## Features

- Habit tracking with progress goals
- Colour-coded notes and checklists
- Recurring reminders and priority task management

# Routina App - Habit & Task Tracker

Routina is a native Android application built to assist users in building daily consistency and managing habits effectively.

## Features
- **Authentication**: User registration and login via REST API integration.
- **Habit & Task Management**: Create, track, and manage daily habits.
- **Settings & Customization**: Configure app settings and multi-language support.
- **Offline Readiness**: Local session persistence for offline handling.

## Tech Stack
- **Language**: Kotlin
- **Architecture**: MVVM / Clean Architecture
- **Networking**: Retrofit2 & OkHttp3
- **Build System**: Gradle

## Prerequisites & Backend Setup
1. **Android Studio**: Ladybug or newer with Android SDK API 35 installed.
2. **Backend Service**: Ensure the backend API is running locally at `http://10.0.2.2:5000` or configure the live URL in `RetrofitClient.kt`.
3. **Emulator/Device**: Android 15.0 (API Level 35 recommended).

## How to Run the Application
1. Clone this repository:
   ```bash
  git clone https://github.com/Khonani01/Routina_App.git

## Changelog - Charity Fix 2026-05-11
Fixed HabitUtils.kt - BUILD SUCCESSFUL
Fixed HabitsActivity.kt
Added Models.kt
Added NotesScreen.kt and NotesViewModel.kt
Pushed by Charity Taulene ST10438951
