package com.example.dukkanapp.core.common.components.feedback

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import com.example.dukkanapp.R
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppPillShape
import com.example.dukkanapp.core.theme.AppShadow
import com.example.dukkanapp.core.theme.AppTheme
import com.example.dukkanapp.core.theme.AppToastTokens
import com.example.dukkanapp.core.theme.appShadow
import com.example.dukkanapp.core.theme.appShadowLayers
import com.example.dukkanapp.core.utils.constants.AppIcons

enum class AppToastType { Success, Error, Warning, Info, Loading }

/** Figma has no status/info token: Info and Loading use the brand colour. */
@Composable
internal fun AppToastType.statusColor(): Color {
    val c = AppTheme.colors
    return when (this) {
        AppToastType.Success -> c.success
        AppToastType.Error -> c.error
        AppToastType.Warning -> c.warning
        AppToastType.Info, AppToastType.Loading -> c.brand
    }
}

/**
 * The toast itself: no timer logic, no position. [AppToastHost] adds those.
 * Pass [timerProgress] (1 → 0) to show the draining ring, [onClose] to show the close button.
 */
@Composable
fun AppToast(
    title: String,
    type: AppToastType,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionText: String? = null,
    onAction: () -> Unit = {},
    onClose: (() -> Unit)? = null,
    timerProgress: (() -> Float)? = null,
) {
    val colors = AppTheme.colors
    val tokens = AppTheme.shadows.toast
    val status = type.statusColor()
    val shape = RoundedCornerShape(AppDimens.radiusCard)
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val wash = status.copy(alpha = tokens.washAlpha)
    val washBrush = if (isRtl) {
        Brush.horizontalGradient(0.38f to Color.Transparent, 1f to wash)
    } else {
        Brush.horizontalGradient(0f to wash, 0.62f to Color.Transparent)
    }
    val typography = MaterialTheme.typography

    Row(
        modifier = modifier
            .semantics {
                liveRegion =
                    if (type == AppToastType.Error) LiveRegionMode.Assertive else LiveRegionMode.Polite
            }
            .heightIn(min = AppDimens.toast)
            .appShadowLayers(tokens.layers, shape)
            .clip(shape)
            .background(colors.card)
            .background(washBrush)
            .border(AppDimens.strokeHairline, status.copy(alpha = tokens.borderAlpha), shape)
            .then(
                tokens.topHighlight?.let { highlight ->
                    Modifier.innerShadow(
                        shape = shape,
                        shadow = Shadow(
                            radius = highlight.blur,
                            color = highlight.color,
                            offset = DpOffset(Dp.Hairline, highlight.offsetY),
                        ),
                    )
                } ?: Modifier,
            )
            // Padding is 10 on the icon side and 12 on the close side; start/end mirror in RTL.
            .padding(
                PaddingValues(
                    start = AppDimens.toastPaddingStart,
                    end = AppDimens.space12,
                    top = AppDimens.toastPaddingStart,
                    bottom = AppDimens.toastPaddingStart,
                ),
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.space12),
    ) {
        ToastHalo(type = type, status = status, tokens = tokens)
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = if (isRtl) typography.labelLarge else typography.titleSmall,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = if (isRtl) typography.labelMedium else typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        actionText?.let {
            ToastAction(text = it, status = status, onStatus = tokens.onStatus, onClick = onAction)
        }
        if (onClose != null || timerProgress != null) {
            ToastClose(
                status = status,
                onClose = onClose,
                timerProgress = timerProgress,
                isRtl = isRtl
            )
        }
    }
}

/** 44 halo → 34 ring → 24 core → 13 glyph. */
@Composable
private fun ToastHalo(type: AppToastType, status: Color, tokens: AppToastTokens) {
    Box(
        modifier = Modifier
            .size(AppDimens.toastHalo)
            .clip(CircleShape)
            .background(status.copy(alpha = tokens.haloAlpha)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(AppDimens.toastRing)
                .clip(CircleShape)
                .background(status.copy(alpha = tokens.ringAlpha)),
            contentAlignment = Alignment.Center,
        ) {
            if (type == AppToastType.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(AppDimens.toastSpinner),
                    color = status,
                    trackColor = AppTheme.colors.border,
                    strokeWidth = AppDimens.toastSpinnerStroke,
                )
            } else {
                ToastCore(type = type, status = status, onStatus = tokens.onStatus)
            }
        }
    }
}

@Composable
private fun ToastCore(type: AppToastType, status: Color, onStatus: Color) {
    Box(
        modifier = Modifier
            .size(AppDimens.toastCore)
            .appShadow(
                AppShadow(
                    status.copy(alpha = 0.35f),
                    offsetY = AppDimens.toastCoreShadowY,
                    blur = AppDimens.toastCoreShadowBlur
                ),
                CircleShape,
            )
            .background(status, CircleShape)
            .innerShadow(
                shape = CircleShape,
                shadow = Shadow(
                    radius = Dp.Hairline,
                    color = Color.White.copy(alpha = 0.25f),
                    offset = DpOffset(Dp.Hairline, AppDimens.strokeHairline),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        when (type) {
            AppToastType.Success -> ToastGlyphIcon(AppIcons.Check, onStatus)
            AppToastType.Error -> ToastGlyphIcon(AppIcons.X, onStatus)
            // "!" and "i" are bare glyphs. The Lucide icons of the same name carry their own circle.
            AppToastType.Warning -> ToastGlyphText("!", onStatus)
            AppToastType.Info -> ToastGlyphText("i", onStatus)
            AppToastType.Loading -> Unit
        }
    }
}

@Composable
private fun ToastGlyphIcon(icon: Int, tint: Color) {
    Icon(
        painterResource(icon),
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(AppDimens.toastGlyph)
    )
}

@Composable
private fun ToastGlyphText(text: String, color: Color) {
    Text(text = text, style = AppTheme.textStyles.labelStrong, color = color)
}

@Composable
private fun ToastAction(text: String, status: Color, onStatus: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .heightIn(min = AppDimens.toastAction)
            .appShadow(
                AppShadow(
                    status.copy(alpha = 0.30f),
                    offsetY = AppDimens.toastActionShadowY,
                    blur = AppDimens.toastActionShadowBlur,
                    spread = AppDimens.toastActionShadowSpread,
                ),
                AppPillShape,
            )
            .clip(AppPillShape)
            .background(status)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = AppDimens.toastActionPaddingH, vertical = AppDimens.space8),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = onStatus)
    }
}

/** 28 ring inside a 44 touch target: track + status arc that drains, × 12 in the middle. */
@Composable
private fun ToastClose(
    status: Color,
    onClose: (() -> Unit)?,
    timerProgress: (() -> Float)?,
    isRtl: Boolean,
) {
    val colors = AppTheme.colors
    val track = colors.textTertiary.copy(alpha = 0.25f)
    Box(
        modifier = Modifier
            .size(AppDimens.iconButton)
            .then(
                if (onClose != null) {
                    Modifier
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = onClose)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(AppDimens.toastTimer)) {
            val strokePx = AppDimens.toastTimerStroke.toPx()
            val inset = strokePx / 2
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            drawCircle(color = track, radius = arcSize.width / 2, style = Stroke(strokePx))
            // Reading the lambda here redraws only the ring on each tick, not the whole toast.
            val progress = timerProgress?.invoke() ?: 1f
            val direction = if (isRtl) -1f else 1f
            drawArc(
                color = status,
                startAngle = -90f,
                sweepAngle = direction * 360f * progress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(strokePx, cap = StrokeCap.Round),
            )
        }
        if (onClose != null) {
            Icon(
                painter = painterResource(AppIcons.X),
                contentDescription = stringResource(R.string.cd_dismiss),
                tint = colors.textSecondary,
                modifier = Modifier.size(AppDimens.iconXxs),
            )
        }
    }
}