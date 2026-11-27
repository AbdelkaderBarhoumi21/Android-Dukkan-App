package com.example.dukkanapp.features.auth.presentation.logic.emailverification

import com.example.dukkanapp.core.common.text.AppUiText

data class EmailVerificationUiState(
    val email: String = "",
    val isChecking: Boolean = false,
    val isResending: Boolean = false,
    val isSigningOut: Boolean = false,
    val resendCooldownSeconds: Int = 0,
    val isNotVerified: Boolean = false,
    val message: Message? = null
) {
    data class Message(val text: AppUiText, val isError: Boolean)

    val canResend: Boolean get() = !isResending && resendCooldownSeconds == 0
}