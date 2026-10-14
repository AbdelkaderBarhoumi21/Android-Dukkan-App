package com.example.dukkanapp.core.common.components.texts

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

sealed interface AppUiText {
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : AppUiText
    data class Raw(val value: String) : AppUiText
}

@Composable
fun AppUiText.asString(): String = when (this) {
    is AppUiText.Res -> stringResource(id)
    is AppUiText.Raw -> value
}

fun AppUiText.asString(context: Context): String = when (this) {
    is AppUiText.Res -> context.getString(id, *args.toTypedArray())
    is AppUiText.Raw -> value
}