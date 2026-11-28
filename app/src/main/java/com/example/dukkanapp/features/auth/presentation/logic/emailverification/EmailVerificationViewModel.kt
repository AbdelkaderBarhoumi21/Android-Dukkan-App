package com.example.dukkanapp.features.auth.presentation.logic.emailverification

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.dukkanapp.R
import com.example.dukkanapp.core.common.mvi.AppViewModel
import com.example.dukkanapp.core.common.text.AppUiText
import com.example.dukkanapp.core.error.AppFailure
import com.example.dukkanapp.core.error.onFailure
import com.example.dukkanapp.core.error.onSuccess
import com.example.dukkanapp.core.error.ui.toUiText
import com.example.dukkanapp.core.navigation.AppRoute
import com.example.dukkanapp.features.auth.domain.usecase.CheckEmailVerificationUseCase
import com.example.dukkanapp.features.auth.domain.usecase.GetCurrentUserUseCase
import com.example.dukkanapp.features.auth.domain.usecase.SendEmailVerificationUseCase
import com.example.dukkanapp.features.auth.domain.usecase.SignOutUseCase
import com.example.dukkanapp.features.auth.presentation.logic.emailverification.EmailVerificationUiState.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmailVerificationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getCurrentUser: GetCurrentUserUseCase,
    private val checkEmailVerification: CheckEmailVerificationUseCase,
    private val sendEmailVerification: SendEmailVerificationUseCase,
    private val signOut: SignOutUseCase,
) : AppViewModel<EmailVerificationUiState, EmailVerificationIntent, EmailVerificationEffect>(
    initialState = EmailVerificationUiState(email = getCurrentUser()?.email.orEmpty())
) {
    /** True when sign-up has just sent the email, i.e. a brand-new account. */
    private val emailJustSent =
        savedStateHandle.toRoute<AppRoute.EmailVerificationScreen>().emailJustSent

    /** "Ahmed Ben Ali" -> "Ahmed", for the All set greeting. */
    private val firstName =
        getCurrentUser()?.displayName.orEmpty().substringBefore(" ")

    private var checkJob: Job? = null
    private var cooldownJob: Job? = null

    init {
        // Sign-up has just sent the email: resending right away would hit Firebase's rate limit.
        if (emailJustSent) startResendCooldown()
    }

    override fun onIntent(intent: EmailVerificationIntent) {
        when (intent) {
            EmailVerificationIntent.ScreenResumed -> checkVerification(silent = true)
            EmailVerificationIntent.CheckClicked -> checkVerification(silent = false)
            EmailVerificationIntent.ResendClicked -> resend()
            EmailVerificationIntent.OpenEmailAppClicked -> sendEffect(EmailVerificationEffect.OpenEmailApp)
            EmailVerificationIntent.EmailAppUnavailable ->
                showMessage(R.string.email_verification_no_email_app, isError = true)

            EmailVerificationIntent.UseAnotherAccountClicked -> signOutAndLeave()
            EmailVerificationIntent.ChangeEmailClicked -> signOutAndLeave(toSignUp = true)
            EmailVerificationIntent.BackClicked -> signOutAndLeave()
        }
    }

    /**
     * [silent] = automatic check on resume: only navigates, never shows a message.
     * One check at a time: a resume during a manual check doesn't start a second one.
     */
    private fun checkVerification(silent: Boolean) {
        if (checkJob?.isActive == true) return
        if (!silent) setState { copy(isChecking = true, message = null) }

        checkJob = viewModelScope.launch {
            checkEmailVerification()
                .onSuccess { isVerified ->
                    when {
                        isVerified -> sendEffect(
                            if (emailJustSent) EmailVerificationEffect.NavigateToAllSet(firstName)
                            else EmailVerificationEffect.NavigateToHome
                        )
                        // Figma 02.7: show the "isn't verified yet" card, not a banner.
                        !silent -> setState { copy(isNotVerified = true) }
                    }
                }
                .onFailure { failure -> handleFailure(failure, silent) }
            setState { copy(isChecking = false) }
        }
    }

    private fun resend() {
        if (!currentState.canResend) return
        // A new link is on its way: go back from 02.7 to 02.6.
        setState { copy(isResending = true, message = null, isNotVerified = false) }

        viewModelScope.launch {
            sendEmailVerification()
                .onSuccess {
                    showMessage(R.string.email_verification_resent_success, isError = false)
                    startResendCooldown()
                }
                .onFailure { failure -> handleFailure(failure, silent = false) }
            setState { copy(isResending = false) }
        }
    }

    private fun handleFailure(failure: AppFailure, silent: Boolean) {
        when {
            failure.meansAccountIsGone -> signOutAndLeave()
            !silent -> setState { copy(message = Message(failure.toUiText(), isError = true)) }
        }
    }

    /** [toSignUp] = "Change" email: the only way to fix a typo is to sign up again. */
    private fun signOutAndLeave(toSignUp: Boolean = false) {
        if (currentState.isSigningOut) return
        setState { copy(isSigningOut = true) }

        viewModelScope.launch {
            signOut()
            sendEffect(
                if (toSignUp) EmailVerificationEffect.NavigateToSignUp
                else EmailVerificationEffect.NavigateToAuthOptions
            )
        }
    }

    private fun startResendCooldown() {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            for (seconds in RESEND_COOLDOWN_SECONDS downTo 1) {
                setState { copy(resendCooldownSeconds = seconds) }
                delay(ONE_SECOND_MS)
            }
            setState { copy(resendCooldownSeconds = 0) }
        }
    }

    private fun showMessage(@StringRes textRes: Int, isError: Boolean) =
        setState { copy(message = Message(AppUiText.Res(textRes), isError)) }

    /**
     * reload() on a deleted account fails with USER_NOT_FOUND, which the mapper reports as
     * InvalidCredentials. On this screen (no credentials typed) all three mean "account gone":
     * staying here could never succeed.
     */
    private val AppFailure.meansAccountIsGone: Boolean
        get() = this == AppFailure.Auth.SessionExpired ||
                this == AppFailure.Auth.UserDisabled ||
                this == AppFailure.Auth.InvalidCredentials

    private companion object {
        const val RESEND_COOLDOWN_SECONDS = 60
        const val ONE_SECOND_MS = 1_000L
    }
}