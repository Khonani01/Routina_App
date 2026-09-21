package com.routina.app.util

/**
 * Pure, side-effect-free helpers extracted out of HabitsActivity so the
 * core streak/progress logic can be unit tested without needing an
 * Android device or emulator.
 */
object HabitUtils {

    /** Returns the new progress value after the user toggles completion. */
    fun nextProgress(currentProgress: Int, goal: Int): Int {
        val safeGoal = if (goal <= 0) 1 else goal
        return if (currentProgress >= safeGoal) 0 else safeGoal
    }

    /** Returns the new streak after toggling completion for the day. */
    fun nextStreak(currentStreak: Int, currentProgress: Int, goal: Int): Int {
        val safeGoal = if (goal <= 0) 1 else goal
        val willBeComplete = currentProgress < safeGoal
        return if (willBeComplete) currentStreak + 1 else maxOf(currentStreak - 1, 0)
    }
}
