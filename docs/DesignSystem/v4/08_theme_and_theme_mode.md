# 08 — AppTheme, theme mode (Light / Dark / System) and the token catalog

**Files:**

| File | Action |
|---|---|
| `core/designsystem/theme/AppTheme.kt` | new (replaces `core/config/theme/AppTheme.kt`) |
| `core/designsystem/theme/ThemeCatalogPreview.kt` | new (replaces `core/config/theme/ThemePreview.kt`) |
| `features/settings/domain/model/ThemeMode.kt` | new |
| `features/settings/domain/repository/ThemeRepository.kt` | new |
| `features/settings/domain/usecase/ObserveThemeModeUseCase.kt` | new |
| `features/settings/domain/usecase/SetThemeModeUseCase.kt` | new |
| `features/settings/data/datasource/ThemePreferenceLocalDataSource.kt` | new |
| `features/settings/data/repository/ThemeRepositoryImpl.kt` | new |
| `features/settings/di/SettingsModule.kt` | new |
| `core/utils/constants/AppPreferencesKeys.kt` | edit: add `THEME_MODE` |
| `core/navigation/startup/AppStartViewModel.kt` | edit: expose `themeMode` |
| `MainActivity.kt` | edit: pass the mode to `AppTheme`, update system bars |

**Delete at the end of this doc:**
`core/config/theme/AppTheme.kt`, `core/config/theme/AppTypography.kt`, `core/config/theme/ThemePreview.kt`,
`core/utils/constants/AppColors.kt`, `core/utils/constants/AppExtendedColors.kt`, `res/font/konnect_*.otf`.

## Why

`AppTheme { }` is the **composition root** of the design system. It's the one place that:

1. picks light or dark tokens,
2. provides Dukkan's own tokens through composition locals,
3. hands Material its `ColorScheme`, `Typography` and `Shapes`.

Everything below it reads tokens and never decides anything.

Figma screen **11.3 App settings** has an **Appearance: Light / Dark / System** control. Today the app always follows the system. The choice must survive restarts, so it goes to DataStore, the same way the language choice does (`LanguagePreferenceLocalDataSource`). It follows our clean-architecture layers: data source → repository → use case → ViewModel.

## Part A — AppTheme

### Code — `AppTheme.kt`

```kotlin
package com.example.dukkanapp.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Accessor for Dukkan tokens inside composables:
 * `AppTheme.colors.brand`, `AppTheme.gradients.button`, `AppTheme.shadows.card`,
 * `AppTheme.textStyles.priceSmall`.
 */
object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current

    val gradients: AppGradients
        @Composable
        @ReadOnlyComposable
        get() = LocalAppGradients.current

    val shadows: AppShadows
        @Composable
        @ReadOnlyComposable
        get() = LocalAppShadows.current

    val textStyles: AppTextStyles
        @Composable
        @ReadOnlyComposable
        get() = LocalAppTextStyles.current
}

/**
 * Dukkan theme. Wrap every screen and every preview in it.
 *
 * @param darkTheme light or dark tokens (Figma 01 / 01b).
 * @param isArabic Cairo type scale instead of Plus Jakarta Sans (Figma "Typography · Cairo").
 *   Defaults to the current app language.
 */
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isArabic: Boolean = LocalConfiguration.current.locales[0].language == "ar",
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkAppColors else LightAppColors
    val gradients = if (darkTheme) DarkAppGradients else LightAppGradients
    val shadows = if (darkTheme) DarkAppShadows else LightAppShadows
    val colorScheme = remember(darkTheme) { colors.toColorScheme(isDark = darkTheme) }
    val typography = if (isArabic) ArabicTypography else LatinTypography
    val textStyles = if (isArabic) ArabicTextStyles else LatinTextStyles

    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalAppGradients provides gradients,
        LocalAppShadows provides shadows,
        LocalAppTextStyles provides textStyles,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = AppShapes,
            content = content,
        )
    }
}
```

### Explained

- **`object AppTheme` and `fun AppTheme` share a name.** Kotlin allows it, and it's the pattern Material uses (`MaterialTheme` the object and `MaterialTheme(...)` the function). You write `AppTheme { }` to wrap and `AppTheme.colors` to read.
- **`@ReadOnlyComposable`** tells Compose the getter only reads a local and doesn't emit UI, so it skips some bookkeeping.
- **`remember(darkTheme)`** builds the 36-slot `ColorScheme` only when the mode changes, not on every recomposition.
- **`dynamicColor` is gone.** See doc 04 for why.
- **Light and dark values are both official Figma values** (`01 · Foundations` and `01b · Dark mode`), except the few derived fields listed in doc 03.

### Arabic typography

`isArabic` picks the Cairo scale from doc 06 (`ArabicTypography`, `ArabicTextStyles`). Both sets are top-level `val`s built once, so switching costs nothing. The names stay the same, so no screen changes: `MaterialTheme.typography.headlineLarge` is Plus Jakarta ExtraBold 24/32 in English and Cairo Bold 24/36 in Arabic.

The app switches language with AppCompat per-app locales, which recreates the activity, so `LocalConfiguration` already carries the new language. Previews can force it: `AppTheme(isArabic = true) { … }` plus `@Preview(locale = "ar")` for RTL. Figma `13.4 · Log in · Arabic (RTL)` is the reference screen.

## Part B — Theme mode (Light / Dark / System)

### Code — `ThemeMode.kt` (domain)

```kotlin
package com.example.dukkanapp.features.settings.domain.model

/** The user's appearance choice on the App settings screen. */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM,
}
```

It's plain Kotlin with no Compose or Android import, so the domain layer stays framework-free.

### Code — `AppPreferencesKeys.kt` (add one line)

```kotlin
val THEME_MODE = stringPreferencesKey("theme_mode")
```

Import `androidx.datastore.preferences.core.stringPreferencesKey` if the file doesn't already.

### Code — `ThemePreferenceLocalDataSource.kt`

```kotlin
package com.example.dukkanapp.features.settings.data.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.dukkanapp.core.utils.constants.AppPreferencesKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ThemePreferenceLocalDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    /** The saved enum name, or null when the user never chose. */
    val savedThemeMode: Flow<String?> =
        dataStore.data.map { prefs -> prefs[AppPreferencesKeys.THEME_MODE] }

    suspend fun saveThemeMode(name: String) {
        dataStore.edit { it[AppPreferencesKeys.THEME_MODE] = name }
    }
}
```

### Code — `ThemeRepository.kt`

```kotlin
package com.example.dukkanapp.features.settings.domain.repository

import com.example.dukkanapp.features.settings.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface ThemeRepository {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}
```

### Code — `ThemeRepositoryImpl.kt`

```kotlin
package com.example.dukkanapp.features.settings.data.repository

import com.example.dukkanapp.features.settings.data.datasource.ThemePreferenceLocalDataSource
import com.example.dukkanapp.features.settings.domain.model.ThemeMode
import com.example.dukkanapp.features.settings.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ThemeRepositoryImpl @Inject constructor(
    private val localDataSource: ThemePreferenceLocalDataSource
) : ThemeRepository {

    override val themeMode: Flow<ThemeMode> =
        localDataSource.savedThemeMode
            .map { saved -> ThemeMode.entries.firstOrNull { it.name == saved } ?: ThemeMode.SYSTEM }
            .distinctUntilChanged()

    override suspend fun setThemeMode(mode: ThemeMode) {
        localDataSource.saveThemeMode(mode.name)
    }
}
```

An unknown or missing value falls back to `SYSTEM`. If you rename an enum entry later, old installs won't crash.

### Code — use cases

```kotlin
package com.example.dukkanapp.features.settings.domain.usecase

import com.example.dukkanapp.features.settings.domain.model.ThemeMode
import com.example.dukkanapp.features.settings.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveThemeModeUseCase @Inject constructor(
    private val repository: ThemeRepository
) {
    operator fun invoke(): Flow<ThemeMode> = repository.themeMode
}
```

```kotlin
package com.example.dukkanapp.features.settings.domain.usecase

import com.example.dukkanapp.features.settings.domain.model.ThemeMode
import com.example.dukkanapp.features.settings.domain.repository.ThemeRepository
import javax.inject.Inject

class SetThemeModeUseCase @Inject constructor(
    private val repository: ThemeRepository
) {
    suspend operator fun invoke(mode: ThemeMode) = repository.setThemeMode(mode)
}
```

### Code — `SettingsModule.kt`

```kotlin
package com.example.dukkanapp.features.settings.di

import com.example.dukkanapp.features.settings.data.repository.ThemeRepositoryImpl
import com.example.dukkanapp.features.settings.domain.repository.ThemeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {

    @Binds
    @Singleton
    abstract fun bindThemeRepository(impl: ThemeRepositoryImpl): ThemeRepository
}
```

### Code — `AppStartViewModel.kt` (add)

`AppStartViewModel` already lives at activity level and holds the splash screen. Give it the theme mode too, so the splash stays up until **both** the destination and the theme are known. Without this, a dark-mode user sees one white frame.

Add the constructor parameter and the property. Keep everything that's already there.

```kotlin
// constructor: add
private val observeThemeMode: ObserveThemeModeUseCase,

// body: add
/** null until DataStore answered; MainActivity keeps the splash while null. */
val themeMode: StateFlow<ThemeMode?> = observeThemeMode()
    .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = null)
```

Imports: `kotlinx.coroutines.flow.SharingStarted`, `kotlinx.coroutines.flow.StateFlow`, `kotlinx.coroutines.flow.stateIn`, `androidx.lifecycle.viewModelScope`, and the two `features.settings.domain` classes.

### Code — `MainActivity.kt` (full file)

```kotlin
package com.example.dukkanapp

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dukkanapp.core.designsystem.theme.AppTheme
import com.example.dukkanapp.core.navigation.AppNavHost
import com.example.dukkanapp.core.navigation.startup.AppStartViewModel
import com.example.dukkanapp.features.settings.domain.model.ThemeMode
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val appStartViewModel: AppStartViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Keep the splash until we know where to go AND which theme to paint
        splashScreen.setKeepOnScreenCondition {
            appStartViewModel.startDestination.value == null ||
                appStartViewModel.themeMode.value == null
        }
        enableEdgeToEdge()
        setContent {
            val themeMode by appStartViewModel.themeMode.collectAsStateWithLifecycle()
            val destination by appStartViewModel.startDestination.collectAsStateWithLifecycle()
            val darkTheme = themeMode.isDarkTheme()

            SystemBarsEffect(darkTheme)

            AppTheme(darkTheme = darkTheme) {
                destination?.let { AppNavHost(startDestination = it) }
            }
        }
    }

    /** Status and navigation bar icons follow the app theme, not only the system. */
    @Composable
    private fun SystemBarsEffect(darkTheme: Boolean) {
        DisposableEffect(darkTheme) {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
            )
            onDispose {}
        }
    }
}

@Composable
private fun ThemeMode?.isDarkTheme(): Boolean = when (this) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    ThemeMode.SYSTEM, null -> isSystemInDarkTheme()
}
```

`android.graphics.Color` is the platform int color that `SystemBarStyle` expects, not Compose's `Color`. This is one of the few places a raw color constant is correct.

### Using it on the settings screen

The settings screen (Figma 11.3) is a normal MVI feature. Its ViewModel calls `SetThemeModeUseCase(mode)` on `SettingsIntent.ThemeModeSelected(mode)`. The activity's flow emits the new mode and the whole app re-themes instantly, with no restart.

## Part C — Token catalog previews

### Code — `ThemeCatalogPreview.kt`

```kotlin
package com.example.dukkanapp.core.designsystem.theme

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
```

`widthDp = 393` is the Figma frame width (393 × 852), so previews line up with the design.

## Part D — Clean up

1. Search the project for `core.config.theme.AppTheme` and replace it with `core.designsystem.theme.AppTheme` (Ctrl+Shift+R). Previews in every screen import it.
2. Search for `AppTheme.extendedColors` and replace each use (`success` → `AppTheme.colors.success`, `successContainer` → `successSoft`, `favorite` → `error`…). See the table in doc 03.
3. Delete the old files listed at the top of this doc.
4. Delete the 18 `konnect_*.otf` files.
5. Build.

## Check

- [ ] App builds and runs. Login looks blue `#2563EB`, with the Plus Jakarta Sans font.
- [ ] A screen without `AppTheme` in its preview crashes with "AppColors not provided" (that's correct).
- [ ] Switching the phone to dark mode changes the app (mode = SYSTEM by default).
- [ ] After adding the settings screen: choosing Dark survives an app restart.
- [ ] No white flash at launch in dark mode (the splash waits for `themeMode`).
- [ ] Status bar icons are dark on light theme and light on dark theme.
- [ ] `ThemeCatalog` previews render in light, dark and Arabic. Dark matches Figma `01b · Dark mode` (background `#0B1220`, brand `#6090FA`).
- [ ] Switching the app to Arabic shows Cairo everywhere, with taller lines.
- [ ] `core/config/theme/` is empty and deleted.
