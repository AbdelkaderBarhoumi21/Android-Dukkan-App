package com.example.dukkanapp.core.common.components.scaffold

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.example.dukkanapp.core.theme.AppAurora
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppTheme

/** Band height and warm-blob position per aurora size (Figma 393 dp frames). */
enum class AppScreenBackground(val bandHeight: Dp, val sunCenterY: Dp) {
    Plain(Dp.Hairline, Dp.Hairline), // Hairline = 0 dp: no aurora band
    AuroraCompact(AppDimens.auroraCompact, AppDimens.auroraSunYShort),
    AuroraShort(AppDimens.auroraShort, AppDimens.auroraSunYShort),
    AuroraTall(AppDimens.auroraTall, AppDimens.auroraSunYTall),
}

@Composable
fun AppScreenBackgroundBox(
    background: AppScreenBackground,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier
        .fillMaxSize()
        .background(AppTheme.colors.background)) {
        if (background != AppScreenBackground.Plain) {
            AppAuroraHeader(background)
        }
        content()
    }
}

@Composable
private fun AppAuroraHeader(background: AppScreenBackground) {
    val gradients = AppTheme.gradients
    val aurora = gradients.aurora
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val fadeHeight = AppDimens.auroraFade

    Canvas(modifier = Modifier
        .fillMaxWidth()
        .height(background.bandHeight)) {
        drawRect(brush = gradients.screenTint)

        drawBlob(
            aurora,
            aurora.blue,
            xFraction = 0.153f,
            y = AppDimens.auroraBlueY.toPx(),
            radius = AppDimens.auroraBlueRadius.toPx(),
            isRtl
        )
        drawBlob(
            aurora,
            aurora.sky,
            xFraction = 0.916f,
            y = AppDimens.auroraSkyY.toPx(),
            radius = AppDimens.auroraSkyRadius.toPx(),
            isRtl
        )
        drawBlob(
            aurora,
            aurora.sun,
            xFraction = 0.763f,
            y = background.sunCenterY.toPx(),
            radius = AppDimens.auroraSunRadius.toPx(),
            isRtl
        )

        // Melt the band into the plain background so there is no visible edge.
        val fadeTop = size.height - fadeHeight.toPx()
        translate(top = fadeTop) {
            drawRect(brush = gradients.bottomFade, size = Size(size.width, fadeHeight.toPx()))
        }
    }
}

/** [color] already holds its alpha (light or dark, from the tokens). */
private fun DrawScope.drawBlob(
    aurora: AppAurora,
    color: Color,
    xFraction: Float,
    y: Float,
    radius: Float,
    isRtl: Boolean,
) {
    val x = size.width * (if (isRtl) 1f - xFraction else xFraction)
    val center = Offset(x, y)
    drawCircle(
        brush = aurora.blob(color, center, radius),
        radius = radius,
        center = center,
    )
}