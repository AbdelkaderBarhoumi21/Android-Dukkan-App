package com.example.dukkanapp.features.auth.presentation.logic.signup

import androidx.lifecycle.viewModelScope
import com.example.dukkanapp.core.common.mvi.AppViewModel
import com.example.dukkanapp.core.error.ui.toUiText
import com.example.dukkanapp.core.utils.constants.AppLinks
import com.example.dukkanapp.features.auth.domain.validation.AuthFormValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor() :
    AppViewModel<SignUpUiState, SignUpIntent, SignUpEffect>(
        initialState = SignUpUiState()
    ) {
    override fun onIntent(intent: SignUpIntent) {
        when (intent) {
            is SignUpIntent.FullNameChanged -> setState {
                copy(
                    fullName = intent.fullName,
                    fullNameError = null
                )
            }

            is SignUpIntent.EmailChanged -> setState {
                copy(
                    email = intent.email,
                    emailError = null,
                    generalError = null
                )
            }

            is SignUpIntent.PasswordChanged -> setState {
                copy(
                    password = intent.password,
                    passwordError = null
                )
            }

            is SignUpIntent.AcceptTermsChanged -> setState {
                copy(
                    acceptedTerms = intent.checked,
                    termsError = null
                )
            }

            is SignUpIntent.MarketingOptInChanged -> setState { copy(marketingOptIn = intent.checked) }

            SignUpIntent.TermsClicked -> sendEffect(SignUpEffect.OpenUrl(AppLinks.TERMS))
            SignUpIntent.PrivacyClicked -> sendEffect(SignUpEffect.OpenUrl(AppLinks.PRIVACY))
            SignUpIntent.SignUpClicked -> onSignUpClicked()
            SignUpIntent.LoginClicked -> sendEffect(SignUpEffect.NavigateToLogin)
            SignUpIntent.BackClicked -> sendEffect(SignUpEffect.NavigateBack)

        }
    }

    private fun onSignUpClicked() {
        val current = currentState
        if (current.isLoading) return

        val fullName = current.fullName.trim()
        val email = current.email.trim()
        val password = current.password

        val fullNameError = AuthFormValidator.validateFullName(fullName)
        val emailError = AuthFormValidator.validateEmail(email)
        val passwordError = AuthFormValidator.validateNewPassword(password)
        val termsError = AuthFormValidator.validateTermsAccepted(current.acceptedTerms)

        if (listOf(fullNameError, emailError, passwordError, termsError).any { it != null }) {
            setState {
                copy(
                    fullNameError = fullNameError?.toUiText(),
                    emailError = emailError?.toUiText(),
                    passwordError = passwordError?.toUiText(),
                    termsError = termsError?.toUiText(),
                )
            }
            return
        }

        setState { copy(isLoading = true, generalError = null) }
        viewModelScope.launch {
            // Auth guide's SignUpWithEmailUseCase, extended (see below).
            signUpWithEmail(
                email = email,
                password = password,
                fullName = fullName,
                marketingOptIn = current.marketingOptIn,
                rememberMe = true, // (Decision 3)
            )
                .onSuccess { sendEffect(SignUpEffect.NavigateToEmailVerification(emailJustSent = true)) }
                .onFailure { failure -> setState { copy(generalError = failure.toUiText()) } }
            setState { copy(isLoading = false) }
        }
    }