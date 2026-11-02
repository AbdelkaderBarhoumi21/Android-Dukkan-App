package com.example.dukkanapp.core.theme
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview

/** Every Dukkan token on one scrollable page. Compare it with Figma "01 · Foundations". */
@Composable
fun ThemeCatalog() {
    val colors = AppTheme.colors
    Column(
        modifier = Modifier
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(AppDimens.screenPadding),
        verticalArrangement = Arrangement.spacedBy(AppDimens.space24),
    ) {
        CatalogSection("Semantic colors") { ColorGrid(colors.asNamedList()) }
        CatalogSection("Gradients") { GradientRow(AppTheme.gradients) }
        CatalogSection("Shadows") { ShadowRow(AppTheme.shadows) }
        CatalogSection("Typography") { TypeList(typeSamples()) }
        CatalogSection("Radius") { RadiusRow() }
    }
}

@Composable
private fun CatalogSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.space12)) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = AppTheme.colors.textPrimary)
        content()
    }
}

private fun AppColors.asNamedList(): List<Pair<String, Color>> = listOf(
    "background" to background, "card" to card, "field" to field, "sheet" to sheet,
    "textPrimary" to textPrimary, "textSecondary" to textSecondary,
    "textTertiary" to textTertiary, "textLabel" to textLabel,
    "border" to border, "borderStrong" to borderStrong,
    "brand" to brand, "brandPressed" to brandPressed, "brandSoft" to brandSoft,
    "brandSoftText" to brandSoftText, "brandRing" to brandRing, "onBrand" to onBrand,
    "success" to success, "successText" to successText, "successSoft" to successSoft,
    "warning" to warning, "warningText" to warningText, "warningSoft" to warningSoft,
    "error" to error, "errorSoft" to errorSoft, "errorBorder" to errorBorder,
    "deal" to deal, "dealSoft" to dealSoft, "gold" to gold,
    "inverse" to inverse, "onInverse" to onInverse, "scrim" to scrim, "shadow" to shadow,
)

@Composable
private fun ColorGrid(items: List<Pair<String, Color>>) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.space8)) {
        items.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.space8)) {
                row.forEach { (name, color) ->
                    Column(Modifier.weight(1f)) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(AppDimens.controlMd)
                                .clip(MaterialTheme.shapes.small)
                                .background(color)
                                .border(AppDimens.strokeThin, AppTheme.colors.border, MaterialTheme.shapes.small),
                        )
                        Text(name, style = AppTheme.textStyles.caption, color = AppTheme.colors.textSecondary)
                    }
                }
                repeat(3 - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun GradientRow(gradients: AppGradients) {
    val items: List<Pair<String, Brush>> = listOf(
        "brand" to gradients.brand, "button" to gradients.button, "screenTint" to gradients.screenTint,
        "bottomFade" to gradients.bottomFade, "deal" to gradients.deal, "gold" to gradients.gold, "navy" to gradients.navy,
        "imageScrim" to gradients.imageScrim,
    )
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.space8)) {
        items.forEach { (name, brush) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .width(AppDimens.space64 + AppDimens.space64)
                        .height(AppDimens.controlSm)
                        .clip(AppPillShape)
                        .background(brush),
                )
                Text(
                    name,
                    modifier = Modifier.padding(start = AppDimens.space12),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTheme.colors.textPrimary,
                )
            }
        }
    }
}

@Composable
private fun ShadowRow(shadows: AppShadows) {
    val items = listOf(
        "card" to shadows.card, "float" to shadows.float, "button" to shadows.button,
        "deal" to shadows.deal, "screen" to shadows.screen,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.space12)) {
        items.forEach { (name, shadow) ->
            Box(
                modifier = Modifier
                    .size(AppDimens.space64)
                    .appShadow(shadow, MaterialTheme.shapes.large)
                    .clip(MaterialTheme.shapes.large)
                    .background(AppTheme.colors.card),
                contentAlignment = Alignment.Center,
            ) {
                Text(name, style = AppTheme.textStyles.caption, color = AppTheme.colors.textSecondary)
            }
        }
    }
}

@Composable
private fun typeSamples(): List<Pair<String, TextStyle>> {
    val t = MaterialTheme.typography
    val x = AppTheme.textStyles
    return listOf(
        "displayLarge 40/48" to t.displayLarge, "displayMedium 32/40" to t.displayMedium,
        "displaySmall 28/36" to t.displaySmall, "headlineLarge 24/32" to t.headlineLarge,
        "headlineMedium 22/28" to t.headlineMedium, "headlineSmall 20/26" to t.headlineSmall,
        "titleLarge 18/24" to t.titleLarge, "titleMedium 16/22" to t.titleMedium,
        "titleSmall 14/20" to t.titleSmall, "bodyLarge 15/22" to t.bodyLarge,
        "bodyMedium 14/20" to t.bodyMedium, "bodySmall 13/18" to t.bodySmall,
        "labelLarge 15/20" to t.labelLarge, "labelMedium 12.5/16" to t.labelMedium,
        "labelSmall 12/16" to t.labelSmall, "caption 11/14" to x.caption,
        "OVERLINE 10.5/12" to x.overline, "priceLarge 279.000 TND" to x.priceLarge,
        "priceSmall 39.900 TND" to x.priceSmall, "fieldText 14.5/20" to x.fieldText,
        "helper 11.5/14" to x.helper,
    )
}

@Composable
private fun TypeList(items: List<Pair<String, TextStyle>>) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.space8)) {
        items.forEach { (label, style) -> Text(label, style = style, color = AppTheme.colors.textPrimary) }
    }
}

@Composable
private fun RadiusRow() {
    val radii = listOf(
        AppDimens.radiusSm, AppDimens.radiusMd, AppDimens.radiusLg,
        AppDimens.radiusCard, AppDimens.radiusSheet, AppDimens.radiusFull,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.space8)) {
        radii.forEach { radius ->
            Box(
                Modifier
                    .size(AppDimens.controlMd)
                    .clip(RoundedCornerShape(radius))
                    .background(AppTheme.colors.brandSoft),
            )
        }
    }
}

@Preview(name = "Catalog · Light", showBackground = true, widthDp = 393, heightDp = 2200)
@Composable
private fun ThemeCatalogLightPreview() {
    AppTheme(darkTheme = false) { ThemeCatalog() }
}

@Preview(
    name = "Catalog · Dark",
    showBackground = true,
    widthDp = 393,
    heightDp = 2200,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ThemeCatalogDarkPreview() {
    AppTheme(darkTheme = true) { ThemeCatalog() }
}

@Preview(name = "Catalog · Arabic", showBackground = true, widthDp = 393, heightDp = 2200, locale = "ar")
@Composable
private fun ThemeCatalogArabicPreview() {
    AppTheme(darkTheme = false, isArabic = true) { ThemeCatalog() }
}