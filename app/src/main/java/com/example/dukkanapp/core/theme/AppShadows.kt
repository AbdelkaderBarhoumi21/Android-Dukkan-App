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
    val toast: AppToastTokens,
)

/**
 * Figma `Elevation/Toast` and the toast's status tints (`4.4 · Toast`).
 * [topHighlight] is drawn inside the shape: dark mode only.
 * [onStatus] is the colour of the glyph and action label on a status fill.
 * [haloAlpha] and [ringAlpha] are the status colour's alpha in the 44 and 34 icon circles.
 */
@Immutable
data class AppToastTokens(
    val layers: List<AppShadow>,
    val topHighlight: AppShadow?,
    val washAlpha: Float,
    val borderAlpha: Float,
    val haloAlpha: Float,
    val ringAlpha: Float,
    val onStatus: Color,
)

internal val LightAppShadows = AppShadows(
    card = AppShadow(AppPalette.Slate900.copy(alpha = 0.06f), offsetY = 4.dp, blur = 14.dp),
    float = AppShadow(AppPalette.Slate900.copy(alpha = 0.12f), offsetY = 10.dp, blur = 28.dp),
    button = AppShadow(AppPalette.Blue600.copy(alpha = 0.30f), offsetY = 8.dp, blur = 20.dp),
    deal = AppShadow(AppPalette.Orange500.copy(alpha = 0.30f), offsetY = 8.dp, blur = 20.dp),
    screen = AppShadow(AppPalette.Slate900.copy(alpha = 0.10f), offsetY = 18.dp, blur = 44.dp),
    toast = AppToastTokens(
        layers = listOf(
            AppShadow(AppPalette.Slate900.copy(alpha = 0.05f), offsetY = 1.dp, blur = 2.dp),
            AppShadow(AppPalette.Slate900.copy(alpha = 0.10f), offsetY = 8.dp, blur = 20.dp, spread = (-4).dp),
            AppShadow(AppPalette.Slate900.copy(alpha = 0.10f), offsetY = 20.dp, blur = 40.dp, spread = (-12).dp),
        ),
        topHighlight = null,
        washAlpha = 0.09f,
        borderAlpha = 0.18f,
        haloAlpha = 0.10f,
        ringAlpha = 0.16f,
        onStatus = AppPalette.White,
    ),
)

/** Figma 1.2: surfaces go black + 1 dp night-700 border; button and deal keep a coloured glow. */
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
    toast = AppToastTokens(
        layers = listOf(
            AppShadow(AppPalette.Black.copy(alpha = 0.40f), offsetY = 1.dp, blur = 2.dp),
            AppShadow(AppPalette.Black.copy(alpha = 0.55f), offsetY = 12.dp, blur = 32.dp, spread = (-8).dp),
        ),
        topHighlight = AppShadow(AppPalette.White.copy(alpha = 0.06f), offsetY = 1.dp, blur = 0.dp),
        washAlpha = 0.16f,
        borderAlpha = 0.24f,
        haloAlpha = 0.14f,
        ringAlpha = 0.22f,
        onStatus = AppPalette.Night900,
    ),
)
internal val LocalAppShadows = staticCompositionLocalOf<AppShadows> {
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
        withShadow.border(width = AppDimens.strokeHairline, color = shadow.outline, shape = shape)
    } else {
        withShadow
    }
}

/** A multi-layer Figma effect style, such as `Elevation/Toast`. Same modifier-order rule as [appShadow]. */
fun Modifier.appShadowLayers(layers: List<AppShadow>, shape: Shape): Modifier =
    layers.fold(this) { modifier, layer -> modifier.appShadow(layer, shape) }
