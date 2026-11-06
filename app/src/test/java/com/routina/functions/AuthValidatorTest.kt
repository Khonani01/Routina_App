package com.routina.functions

import org.junit.Assert.*
import org.junit.Test

class AuthValidatorTest {

    @Test
    fun testValidEmailReturnsTrue() {
        assertTrue(AuthValidator.isValidEmail("test@example.com"))
    }

    @Test
    fun testInvalidEmailReturnsFalse() {
        assertFalse(AuthValidator.isValidEmail("not-an-email"))
        assertFalse(AuthValidator.isValidEmail(""))
    }

    @Test
    fun testPasswordLengthValidation() {
        assertTrue(AuthValidator.isValidPassword("123456"))
        assertFalse(AuthValidator.isValidPassword("123"))
    }

    @Test
    fun testPasswordMatching() {
        assertTrue(AuthValidator.doPasswordsMatch("secret123", "secret123"))
        assertFalse(AuthValidator.doPasswordsMatch("secret123", "wrongmatch"))
    }
}