package com.example.dukkanapp.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** Colors of the 3 blurred blobs behind auth and home headers. Alpha is already applied. */
@Immutable
data class AppAurora(
    val blue: Color,
    val sky: Color,
    val sun: Color,
) {
    /** A radial blob: [color] in the center, fully transparent at [radius] (pixels). */
    fun blob(color: Color, center: Offset, radius: Float): Brush =
        Brush.radialGradient(
            colors = listOf(color, color.copy(alpha = 0f)),
            center = center,
            radius = radius,
        )
}

/** Dukkan gradients. Read them with `AppTheme.gradients.button`. */
@Immutable
data class AppGradients(
    val brand: Brush,
    val button: Brush,
    val screenTint: Brush,
    val bottomFade: Brush,
    val deal: Brush,
    val gold: Brush,
    val navy: Brush,
    val imageScrim: Brush,
    val aurora: AppAurora,
    /** White glow in the top-end corner of gradient cards. Same in light and dark. */
    val cardHighlight: Color,
)

/** 135°: from the top-start corner to the bottom-end corner of whatever it fills. */
private fun diagonal(start: Color, end: Color): Brush =
    Brush.linearGradient(
        colors = listOf(start, end),
        start = Offset.Zero,
        end = Offset.Infinite,
    )

/** Transparent → opaque [color], top to bottom. */
private fun fadeTo(color: Color): Brush =
    Brush.verticalGradient(listOf(color.copy(alpha = 0f), color))

private val SharedBrand = diagonal(AppPalette.Azure400, AppPalette.Blue700)
private val SharedButton = Brush.horizontalGradient(listOf(AppPalette.Azure500, AppPalette.Blue700))
private val SharedDeal = diagonal(AppPalette.Orange400, AppPalette.Rose500)
private val SharedGold = diagonal(AppPalette.Amber300, AppPalette.Amber500)
private val SharedNavy = diagonal(AppPalette.Blue900, AppPalette.Blue950)
private val SharedImageScrim = Brush.verticalGradient(
    listOf(AppPalette.Slate900.copy(alpha = 0f), AppPalette.Slate900.copy(alpha = 0.72f)),
)
private val SharedCardHighlight = AppPalette.White.copy(alpha = 0.35f)

internal val LightAppGradients = AppGradients(
    brand = SharedBrand,
    button = SharedButton,
    screenTint = Brush.verticalGradient(listOf(AppPalette.Azure100, AppPalette.Slate50)),
    bottomFade = fadeTo(AppPalette.Slate50),
    deal = SharedDeal,
    gold = SharedGold,
    navy = SharedNavy,
    imageScrim = SharedImageScrim,
    aurora = AppAurora(
        blue = AppPalette.Azure300.copy(alpha = 0.62f),
        sky = AppPalette.Azure200.copy(alpha = 0.55f),
        sun = AppPalette.Amber200.copy(alpha = 0.42f),
    ),
    cardHighlight = SharedCardHighlight,
)

internal val DarkAppGradients = AppGradients(
    brand = SharedBrand,
    button = SharedButton,
    screenTint = Brush.verticalGradient(listOf(AppPalette.Blue950, AppPalette.Night950)),
    bottomFade = fadeTo(AppPalette.Night950),
    deal = SharedDeal,
    gold = SharedGold,
    navy = SharedNavy,
    imageScrim = SharedImageScrim,
    // Figma 13.1b · Header glow · spec (dark).
    aurora = AppAurora(
        blue = AppPalette.Blue500.copy(alpha = 0.35f),
        sky = AppPalette.Blue800.copy(alpha = 0.45f),
        sun = AppPalette.Amber300.copy(alpha = 0.08f),
    ),
    cardHighlight = SharedCardHighlight,
)

internal val LocalAppGradients = staticCompositionLocalOf<AppGradients> {
    error("AppGradients not provided: wrap content in AppTheme")
}

/**
 * Figma "Card highlight": a soft white glow centered on the top-end corner (top-right in LTR,
 * top-left in RTL), fading out at [radius]. Only for gradient cards: promo banners, wallet, earnings.
 *
 * Put it AFTER `clip(shape)` and `background(gradient)`, so it is drawn on the gradient,
 * under the content, and clipped to the card:
 *
 *     Modifier.clip(shape).background(AppTheme.gradients.deal).cardHighlight(AppTheme.gradients.cardHighlight)
 */
fun Modifier.cardHighlight(color: Color, radius: Dp = 90.dp): Modifier = drawBehind {
    val r = radius.toPx()
    val center = Offset(x = if (layoutDirection == LayoutDirection.Ltr) size.width else 0f, y = 0f)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(color, color.copy(alpha = 0f)),
            center = center,
            radius = r
        ),
        radius = r,
        center = center,
    )
}