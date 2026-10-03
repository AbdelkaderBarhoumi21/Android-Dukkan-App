package com.example.dukkanapp.core.utils.extension

import android.util.Patterns
import com.example.dukkanapp.core.utils.constants.AppValidationConstants.MIN_PASSWORD_LENGTH

/**
 * Checks if the string is a valid email address format.
 *
 * This extension function validates the string against Android's built-in
 * [Patterns.EMAIL_ADDRESS] regular expression and ensures it is not empty.
 *
 * @return `true` if the string is a valid email format, `false` otherwise.
 */
fun String.isValidEmail(): Boolean = isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(this).matches()
fun String.isValidPassword(): Boolean = this.length >= MIN_PASSWORD_LENGTH