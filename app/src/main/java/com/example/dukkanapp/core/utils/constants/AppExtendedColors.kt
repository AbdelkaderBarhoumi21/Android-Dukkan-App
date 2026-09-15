package com.example.dukkanapp.core.utils.constants

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

@Immutable
data class AppExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
    val sale: Color,
    val onSale: Color,
    val favorite: Color,
)

internal val LightExtendedColors = AppExtendedColors(
    success = Color(0xFF15803D),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFDCFCE7),
    onSuccessContainer = Color(0xFF052E16),
    warning = Color(0xFFA16207),
    onWarning = Color(0xFFFFFFFF),
    warningContainer = Color(0xFFFEF3C7),
    onWarningContainer = Color(0xFF422006),
    info = Color(0xFF0369A1),
    onInfo = Color(0xFFFFFFFF),
    infoContainer = Color(0xFFE0F2FE),
    onInfoContainer = Color(0xFF082F49),
    sale = Color(0xFFBE123C),
    onSale = Color(0xFFFFFFFF),
    favorite = Color(0xFFE11D48),
)

internal val DarkExtendedColors = AppExtendedColors(
    success = Color(0xFF86EFAC),
    onSuccess = Color(0xFF052E16),
    successContainer = Color(0xFF166534),
    onSuccessContainer = Color(0xFFDCFCE7),
    warning = Color(0xFFFDE68A),
    onWarning = Color(0xFF422006),
    warningContainer = Color(0xFF854D0E),
    onWarningContainer = Color(0xFFFEF3C7),
    info = Color(0xFF7DD3FC),
    onInfo = Color(0xFF082F49),
    infoContainer = Color(0xFF075985),
    onInfoContainer = Color(0xFFE0F2FE),
    sale = Color(0xFFFDA4AF),
    onSale = Color(0xFF4C0519),
    favorite = Color(0xFFFB7185),
)

internal val LocalAppExtendedColors = staticCompositionLocalOf {
    LightExtendedColors
}

fun lerp(
    start: AppExtendedColors,
    stop: AppExtendedColors,
    fraction: Float
): AppExtendedColors = AppExtendedColors(
    success = lerp(start.success, stop.success, fraction),
    onSuccess = lerp(start.onSuccess, stop.onSuccess, fraction),
    successContainer = lerp(start.successContainer, stop.successContainer, fraction),
    onSuccessContainer = lerp(
        start.onSuccessContainer,
        stop.onSuccessContainer,
        fraction,
    ),
    warning = lerp(start.warning, stop.warning, fraction),
    onWarning = lerp(start.onWarning, stop.onWarning, fraction),
    warningContainer = lerp(start.warningContainer, stop.warningContainer, fraction),
    onWarningContainer = lerp(
        start.onWarningContainer,
        stop.onWarningContainer,
        fraction,
    ),
    info = lerp(start.info, stop.info, fraction),
    onInfo = lerp(start.onInfo, stop.onInfo, fraction),
    infoContainer = lerp(start.infoContainer, stop.infoContainer, fraction),
    onInfoContainer = lerp(start.onInfoContainer, stop.onInfoContainer, fraction),
    sale = lerp(start.sale, stop.sale, fraction),
    onSale = lerp(start.onSale, stop.onSale, fraction),
    favorite = lerp(start.favorite, stop.favorite, fraction),
)