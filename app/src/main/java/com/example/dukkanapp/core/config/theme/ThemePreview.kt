package com.example.dukkanapp.core.config.theme

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Interactive Design System Catalog - Color Scheme Screen
 */
@Composable
fun ColorSchemePreviewScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "1. Material3 Color Scheme",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Live preview of all color slots in MaterialTheme.colorScheme",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            ColorSchemeSection()

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text(
                text = "2. Extended Custom Colors",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Domain colors in AppTheme.extendedColors",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ExtendedColorsSection()
        }
    }
}

/**
 * Interactive Design System Catalog - Typography Screen
 */
@Composable
fun TypographyPreviewScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Typography Scale (Konnect Font)",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Live preview of all 15 text styles in MaterialTheme.typography",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            TypographySection()
        }
    }
}

@Composable
private fun ColorSchemeSection() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val colorPairs = listOf(
            ColorPairSample("Primary", MaterialTheme.colorScheme.primary, "On Primary", MaterialTheme.colorScheme.onPrimary),
            ColorPairSample("Primary Container", MaterialTheme.colorScheme.primaryContainer, "On Primary Container", MaterialTheme.colorScheme.onPrimaryContainer),
            ColorPairSample("Secondary", MaterialTheme.colorScheme.secondary, "On Secondary", MaterialTheme.colorScheme.onSecondary),
            ColorPairSample("Secondary Container", MaterialTheme.colorScheme.secondaryContainer, "On Secondary Container", MaterialTheme.colorScheme.onSecondaryContainer),
            ColorPairSample("Tertiary", MaterialTheme.colorScheme.tertiary, "On Tertiary", MaterialTheme.colorScheme.onTertiary),
            ColorPairSample("Tertiary Container", MaterialTheme.colorScheme.tertiaryContainer, "On Tertiary Container", MaterialTheme.colorScheme.onTertiaryContainer),
            ColorPairSample("Background", MaterialTheme.colorScheme.background, "On Background", MaterialTheme.colorScheme.onBackground),
            ColorPairSample("Surface", MaterialTheme.colorScheme.surface, "On Surface", MaterialTheme.colorScheme.onSurface),
            ColorPairSample("Surface Variant", MaterialTheme.colorScheme.surfaceVariant, "On Surface Variant", MaterialTheme.colorScheme.onSurfaceVariant),
            ColorPairSample("Inverse Surface", MaterialTheme.colorScheme.inverseSurface, "Inverse On Surface", MaterialTheme.colorScheme.inverseOnSurface),
            ColorPairSample("Error", MaterialTheme.colorScheme.error, "On Error", MaterialTheme.colorScheme.onError),
            ColorPairSample("Error Container", MaterialTheme.colorScheme.errorContainer, "On Error Container", MaterialTheme.colorScheme.onErrorContainer),
        )

        colorPairs.chunked(2).forEach { pairRow ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                pairRow.forEach { sample ->
                    Box(modifier = Modifier.weight(1f)) {
                        ColorSwatchCard(sample)
                    }
                }
                if (pairRow.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Borders & Outlines",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Outline",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Outline Variant",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ExtendedColorsSection() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val extColors = AppTheme.extendedColors

        val extendedPairs = listOf(
            ColorPairSample("Success", extColors.success, "On Success", extColors.onSuccess),
            ColorPairSample("Success Container", extColors.successContainer, "On Success Container", extColors.onSuccessContainer),
            ColorPairSample("Warning", extColors.warning, "On Warning", extColors.onWarning),
            ColorPairSample("Warning Container", extColors.warningContainer, "On Warning Container", extColors.onWarningContainer),
            ColorPairSample("Info", extColors.info, "On Info", extColors.onInfo),
            ColorPairSample("Info Container", extColors.infoContainer, "On Info Container", extColors.onInfoContainer),
            ColorPairSample("Sale", extColors.sale, "On Sale", extColors.onSale),
            ColorPairSample("Favorite", extColors.favorite, "White", Color.White),
        )

        extendedPairs.chunked(2).forEach { pairRow ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                pairRow.forEach { sample ->
                    Box(modifier = Modifier.weight(1f)) {
                        ColorSwatchCard(sample)
                    }
                }
                if (pairRow.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private data class ColorPairSample(
    val bgName: String,
    val bgColor: Color,
    val contentName: String,
    val contentColor: Color,
)

@Composable
private fun ColorSwatchCard(sample: ColorPairSample) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(sample.bgColor)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = sample.bgName,
                style = MaterialTheme.typography.titleSmall,
                color = sample.contentColor,
            )
            Text(
                text = "Text on " + sample.bgName,
                style = MaterialTheme.typography.labelSmall,
                color = sample.contentColor.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun TypographySection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val typographyItems = listOf(
            TypographySample("displayLarge", "57sp Bold", MaterialTheme.typography.displayLarge),
            TypographySample("displayMedium", "45sp Bold", MaterialTheme.typography.displayMedium),
            TypographySample("displaySmall", "36sp Bold", MaterialTheme.typography.displaySmall),
            TypographySample("headlineLarge", "32sp Bold", MaterialTheme.typography.headlineLarge),
            TypographySample("headlineMedium", "28sp Bold", MaterialTheme.typography.headlineMedium),
            TypographySample("headlineSmall", "24sp SemiBold", MaterialTheme.typography.headlineSmall),
            TypographySample("titleLarge", "22sp SemiBold", MaterialTheme.typography.titleLarge),
            TypographySample("titleMedium", "16sp SemiBold", MaterialTheme.typography.titleMedium),
            TypographySample("titleSmall", "14sp SemiBold", MaterialTheme.typography.titleSmall),
            TypographySample("bodyLarge", "16sp Normal", MaterialTheme.typography.bodyLarge),
            TypographySample("bodyMedium", "14sp Normal", MaterialTheme.typography.bodyMedium),
            TypographySample("bodySmall", "12sp Normal", MaterialTheme.typography.bodySmall),
            TypographySample("labelLarge", "14sp SemiBold", MaterialTheme.typography.labelLarge),
            TypographySample("labelMedium", "12sp Medium", MaterialTheme.typography.labelMedium),
            TypographySample("labelSmall", "11sp Medium", MaterialTheme.typography.labelSmall),
        )

        typographyItems.forEach { item ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp),
                    )
                    .padding(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = item.details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Dukkan App - The quick brown fox",
                    style = item.style,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

private data class TypographySample(
    val name: String,
    val details: String,
    val style: TextStyle,
)

// ════════════════════════════════════════════════════════════
// INDIVIDUAL DEDICATED PREVIEWS (VISIBLE DIRECTLY IN IDE PREVIEW)
// ════════════════════════════════════════════════════════════

@Preview(name = "1. Color Scheme (Light)", showBackground = true, widthDp = 380, heightDp = 1200)
@Composable
private fun ColorSchemeLightPreview() {
    AppTheme(darkTheme = false) {
        ColorSchemePreviewScreen()
    }
}

@Preview(
    name = "1. Color Scheme (Dark)",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    widthDp = 380,
    heightDp = 1200
)
@Composable
private fun ColorSchemeDarkPreview() {
    AppTheme(darkTheme = true) {
        ColorSchemePreviewScreen()
    }
}

@Preview(name = "2. Typography (Light)", showBackground = true, widthDp = 380, heightDp = 1800)
@Composable
private fun TypographyLightPreview() {
    AppTheme(darkTheme = false) {
        TypographyPreviewScreen()
    }
}

@Preview(
    name = "2. Typography (Dark)",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    widthDp = 380,
    heightDp = 1800
)
@Composable
private fun TypographyDarkPreview() {
    AppTheme(darkTheme = true) {
        TypographyPreviewScreen()
    }
}
