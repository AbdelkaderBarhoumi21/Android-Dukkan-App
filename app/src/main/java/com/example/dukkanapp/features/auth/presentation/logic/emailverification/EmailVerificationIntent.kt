package com.example.dukkanapp.features.auth.presentation.logic.emailverification

sealed interface EmailVerificationIntent {
    /** Sent on every ON_RESUME, e.g. when the user comes back from their mail app. */
    data object ScreenResumed : EmailVerificationIntent
    data object CheckClicked : EmailVerificationIntent
    data object ResendClicked : EmailVerificationIntent
    data object OpenEmailAppClicked : EmailVerificationIntent

    /** The screen tried to open a mail app and the device has none. */
    data object EmailAppUnavailable : EmailVerificationIntent
    data object UseAnotherAccountClicked : EmailVerificationIntent

    /** "Change" on the Sent-to card. */
    data object ChangeEmailClicked : EmailVerificationIntent
    data object BackClicked : EmailVerificationIntent

}