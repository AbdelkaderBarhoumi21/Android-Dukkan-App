package com.example.dukkanapp.core.navigation.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dukkanapp.core.error.getOrNull
import com.example.dukkanapp.core.navigation.AppRoute
import com.example.dukkanapp.core.usecase.NoParams
import com.example.dukkanapp.features.settings.domain.model.ThemeMode
import com.example.dukkanapp.features.settings.domain.usecase.ObserveThemeModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppStartViewModel @Inject constructor(
    private val resolveStartDestinationUseCase: ResolveStartDestinationUseCase,
    private val observeThemeModeUseCase: ObserveThemeModeUseCase,
) : ViewModel() {
    private val _startDestination = MutableStateFlow<AppRoute?>(null)

    val startDestination: StateFlow<AppRoute?> = _startDestination.asStateFlow()
    val themeMode: StateFlow<ThemeMode?> =
        observeThemeModeUseCase(NoParams).map { result -> result.getOrNull() ?: ThemeMode.SYSTEM }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly, initialValue = null
            )

    init {
        viewModelScope.launch {
            _startDestination.value = resolveStartDestinationUseCase()

        }
    }
}