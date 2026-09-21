package com.routina.app

import com.routina.app.util.HabitUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class HabitUtilsTest {

    @Test
    fun `nextProgress marks habit complete when not yet at goal`() {
        assertEquals(8, HabitUtils.nextProgress(currentProgress = 3, goal = 8))
    }

    @Test
    fun `nextProgress resets habit when already at goal`() {
        assertEquals(0, HabitUtils.nextProgress(currentProgress = 8, goal = 8))
    }

    @Test
    fun `nextProgress treats a zero or negative goal as 1`() {
        assertEquals(1, HabitUtils.nextProgress(currentProgress = 0, goal = 0))
    }

    @Test
    fun `nextStreak increments when habit becomes complete`() {
        assertEquals(6, HabitUtils.nextStreak(currentStreak = 5, currentProgress = 3, goal = 8))
    }

    @Test
    fun `nextStreak decrements but never goes below zero when un-completing`() {
        assertEquals(0, HabitUtils.nextStreak(currentStreak = 0, currentProgress = 8, goal = 8))
        assertEquals(4, HabitUtils.nextStreak(currentStreak = 5, currentProgress = 8, goal = 8))
    }
}
