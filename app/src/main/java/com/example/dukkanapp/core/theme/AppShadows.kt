package com.example.dukkanapp.core.theme

import androidx.compose.foundation.border
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/**
 * One Figma drop shadow: color already includes its alpha.
 * [outline] is an optional 1 dp border drawn with the shadow (dark-mode cards).
 */
@Immutable
data class AppShadow(
    val color: Color,
    val offsetY: Dp,
    val blur: Dp,
    val spread: Dp = 0.dp,
    val outline: Color = Color.Unspecified,
)

/** Dukkan elevation tokens. Read them with `AppTheme.shadows.card`. */
@Immutable
data class AppShadows(
    val card: AppShadow,
    val float: AppShadow,
    val button: AppShadow,
    val deal: AppShadow,
    val screen: AppShadow,
)

internal val LightAppShadows = AppShadows(
    card = AppShadow(AppPalette.Slate900.copy(alpha = 0.06f), offsetY = 4.dp, blur = 14.dp),
    float = AppShadow(AppPalette.Slate900.copy(alpha = 0.12f), offsetY = 10.dp, blur = 28.dp),
    button = AppShadow(AppPalette.Blue600.copy(alpha = 0.30f), offsetY = 8.dp, blur = 20.dp),
    deal = AppShadow(AppPalette.Orange500.copy(alpha = 0.30f), offsetY = 8.dp, blur = 20.dp),
    screen = AppShadow(AppPalette.Slate900.copy(alpha = 0.10f), offsetY = 18.dp, blur = 44.dp),
)

/** Figma 01b: surfaces go black + 1 dp night-700 border; button and deal keep a coloured glow. */
internal val DarkAppShadows = AppShadows(
    card = AppShadow(
        AppPalette.Black.copy(alpha = 0.45f), offsetY = 4.dp, blur = 14.dp,
        outline = AppPalette.Night700,
    ),
    float = AppShadow(
        AppPalette.Black.copy(alpha = 0.55f), offsetY = 10.dp, blur = 28.dp,
        outline = AppPalette.Night700,
    ),
    button = AppShadow(AppPalette.Azure400.copy(alpha = 0.25f), offsetY = 8.dp, blur = 20.dp),
    deal = AppShadow(AppPalette.Orange400.copy(alpha = 0.25f), offsetY = 8.dp, blur = 20.dp),
    screen = AppShadow(
        AppPalette.Black.copy(alpha = 0.60f), offsetY = 18.dp, blur = 44.dp,
        outline = AppPalette.Night700,
    ),
)
internal val LocalAppShadow = staticCompositionLocalOf<AppShadows> {
    error("AppShadows not provided: wrap content in AppTheme")
}

/**
 * Draws a Figma-style drop shadow behind the content, plus the token's 1 dp outline if it has one.
 * Put it BEFORE `clip` / `background` in the modifier chain, so the shadow is not clipped
 * and the outline is drawn on top of the background.
 * Box(
 *     modifier = Modifier
 *         .appShadow(AppTheme.shadows.card, RoundedCornerShape(AppDimens.radiusCard)) // 1. shadow (+ dark border)
 *         .clip(RoundedCornerShape(AppDimens.radiusCard))                            // 2. then clip
 *         .background(AppTheme.colors.card),                                         // 3. then paint
 * )
 */
fun Modifier.appShadow(shadow: AppShadow, shape: Shape): Modifier {
    val withShadow = dropShadow(
        shape = shape,
        shadow = Shadow(
            radius = shadow.blur,
            color = shadow.color,
            spread = shadow.spread,
            offset = DpOffset(x = 0.dp, y = shadow.offsetY),
        ),
    )
    return if (shadow.outline.isSpecified) {
        withShadow.border(width = 1.dp, color = shadow.outline, shape = shape)
    } else {
        withShadow
    }
}