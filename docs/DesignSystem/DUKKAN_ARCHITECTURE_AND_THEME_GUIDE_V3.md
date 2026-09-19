# Dukkan App — Architecture and Theme Guide (v3)

This document is the implementation blueprint for the Dukkan e-commerce Android app.
It supersedes `DESIGN_TOKENS_THEMING_V2.md` for this project because that document uses
the unrelated `com.example.pulse_app` package and misspelled token names such as
`hight` and `hightest`.

The examples below use the real application package:

```text
com.example.dukkanapp
```

## 1. Important naming correction

The correct Material 3 typography accessor is:

```kotlin
MaterialTheme.typography
```

There is no `MaterialThemeTypography`, `MaterialThemetypograp`, or
`MaterialCompositionLocal`. In Jetpack Compose:

- `MaterialTheme.colorScheme` exposes standard Material color roles.
- `MaterialTheme.typography` exposes Material text styles.
- `MaterialTheme.shapes` exposes Material shapes.
- `CompositionLocal` is the Compose equivalent of Flutter's inherited theme lookup.
- A custom `CompositionLocal` is only needed for app-specific values that Material 3
  does not already provide.

## 2. Architecture decisions

Use a single Android application module initially, organized by package and feature.
This is simpler than creating many Gradle modules before the application needs them.
Packages can later become modules without changing the dependency direction.

Use:

- package-by-feature for business functionality;
- MVVM plus unidirectional data flow in presentation;
- repositories as the boundary between domain and data;
- use cases only when they contain reusable business rules;
- immutable UI state;
- `StateFlow` from ViewModels;
- Material 3 semantic roles instead of raw colors in screens.

Dependency direction:

```text
presentation -> domain <- data
       |          |
       +-------> core

app/navigation -> feature public APIs
app/di         -> concrete data implementations
```

Rules:

1. `domain` must not import Compose, Retrofit, Room, Android UI classes, or DTOs.
2. `presentation` may depend on `domain` and reusable `core` UI.
3. `data` implements domain repository interfaces.
4. One feature must not import another feature's internal data or presentation code.
5. Cross-feature navigation uses routes or small public feature APIs.
6. Raw color hex values are allowed only in `core/config/theme/Color.kt`.
7. Screens use semantic roles, never palette names such as `Blue700`.

## 3. Recommended project tree

```text
app/src/main/java/com/example/dukkanapp/
├── DukkanApplication.kt
├── MainActivity.kt
│
├── app/
│   ├── DukkanApp.kt
│   ├── navigation/
│   │   ├── AppDestination.kt
│   │   ├── AppNavHost.kt
│   │   └── MainGraph.kt
│   └── di/
│       ├── AppModule.kt
│       ├── DatabaseModule.kt
│       ├── NetworkModule.kt
│       └── RepositoryModule.kt
│
├── core/
│   ├── config/
│   │   └── theme/
│   │       ├── Color.kt
│   │       ├── ExtendedColors.kt
│   │       ├── Shape.kt
│   │       ├── Theme.kt
│   │       └── Typography.kt
│   │
│   ├── utils/
│   │   ├── constants/
│   │   │   ├── AppConstants.kt
│   │   │   ├── AppDimens.kt
│   │   │   └── ValidationConstants.kt
│   │   └── extensions/
│   │       ├── FlowExtensions.kt
│   │       └── StringExtensions.kt
│   │
│   ├── common/
│   │   ├── result/
│   │   │   └── AppResult.kt
│   │   ├── error/
│   │   │   └── AppError.kt
│   │   └── dispatcher/
│   │       ├── DispatcherProvider.kt
│   │       └── DefaultDispatcherProvider.kt
│   │
│   ├── data/
│   │   ├── local/
│   │   │   ├── database/
│   │   │   └── preferences/
│   │   └── remote/
│   │       ├── api/
│   │       └── dto/
│   │
│   └── ui/
│       ├── components/
│       │   ├── DukkanButton.kt
│       │   ├── DukkanTextField.kt
│       │   ├── ProductCard.kt
│       │   └── PriceText.kt
│       └── feedback/
│           ├── EmptyContent.kt
│           ├── ErrorContent.kt
│           └── LoadingContent.kt
│
└── features/
    ├── auth/
    │   ├── data/
    │   │   ├── remote/
    │   │   ├── mapper/
    │   │   └── AuthRepositoryImpl.kt
    │   ├── domain/
    │   │   ├── model/
    │   │   ├── repository/AuthRepository.kt
    │   │   └── usecase/
    │   └── presentation/
    │       ├── login/
    │       └── register/
    ├── home/
    ├── catalog/
    ├── product/
    ├── search/
    ├── cart/
    ├── checkout/
    ├── orders/
    ├── favorites/
    ├── profile/
    └── settings/
```

Not every feature needs all three layers. A small feature may start with
`presentation/` and gain `domain/` or `data/` only when required.

## 4. Theme file map

```text
core/config/theme/
├── Color.kt           primitive palette and Material ColorScheme values
├── ExtendedColors.kt  custom e-commerce semantic colors and lerp
├── Typography.kt      Material 3 type scale
├── Shape.kt           Material 3 shape scale
└── Theme.kt           selects and provides the active theme

core/utils/constants/
└── AppDimens.kt       spacing, radius, icon, and component dimensions
```

This separation keeps a single source of truth:

- standard colors come from `MaterialTheme.colorScheme`;
- app-only colors come from `DukkanTheme.extendedColors`;
- typography comes from `MaterialTheme.typography`;
- shapes come from `MaterialTheme.shapes`;
- static dimensions come from `AppDimens`.

## 5. `Color.kt`

Palette values are implementation details. Screens should not use them directly.
The light theme uses darker brand/status colors on pale surfaces. The dark theme uses
lighter brand/status colors on dark surfaces. This is not a simple inversion: each
role is selected for contrast and meaning.

```kotlin
package com.example.dukkanapp.core.config.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Brand blue
private val Blue50 = Color(0xFFEFF6FF)
private val Blue100 = Color(0xFFDBEAFE)
private val Blue200 = Color(0xFFBFDBFE)
private val Blue300 = Color(0xFF93C5FD)
private val Blue400 = Color(0xFF60A5FA)
private val Blue600 = Color(0xFF2563EB)
private val Blue700 = Color(0xFF1D4ED8)
private val Blue800 = Color(0xFF1E40AF)
private val Blue900 = Color(0xFF1E3A8A)
private val Blue950 = Color(0xFF172554)

// Commerce accent orange
private val Orange50 = Color(0xFFFFF7ED)
private val Orange100 = Color(0xFFFFEDD5)
private val Orange300 = Color(0xFFFDBA74)
private val Orange700 = Color(0xFFC2410C)
private val Orange800 = Color(0xFF9A3412)
private val Orange950 = Color(0xFF431407)

// Tertiary teal
private val Teal50 = Color(0xFFF0FDFA)
private val Teal100 = Color(0xFFCCFBF1)
private val Teal300 = Color(0xFF5EEAD4)
private val Teal700 = Color(0xFF0F766E)
private val Teal800 = Color(0xFF115E59)
private val Teal950 = Color(0xFF042F2E)

// Neutral surfaces
private val Slate50 = Color(0xFFF8FAFC)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate300 = Color(0xFFCBD5E1)
private val Slate400 = Color(0xFF94A3B8)
private val Slate500 = Color(0xFF64748B)
private val Slate600 = Color(0xFF475569)
private val Slate700 = Color(0xFF334155)
private val Slate800 = Color(0xFF1E293B)
private val Slate900 = Color(0xFF0F172A)
private val Slate950 = Color(0xFF020617)

private val Red50 = Color(0xFFFEF2F2)
private val Red100 = Color(0xFFFEE2E2)
private val Red300 = Color(0xFFFCA5A5)
private val Red700 = Color(0xFFB91C1C)
private val Red800 = Color(0xFF991B1B)
private val Red950 = Color(0xFF450A0A)

private val White = Color(0xFFFFFFFF)
private val Black = Color(0xFF000000)

internal val DukkanLightColorScheme = lightColorScheme(
    primary = Blue700,
    onPrimary = White,
    primaryContainer = Blue100,
    onPrimaryContainer = Blue950,
    inversePrimary = Blue300,

    secondary = Orange700,
    onSecondary = White,
    secondaryContainer = Orange100,
    onSecondaryContainer = Orange950,

    tertiary = Teal700,
    onTertiary = White,
    tertiaryContainer = Teal100,
    onTertiaryContainer = Teal950,

    background = Slate50,
    onBackground = Slate900,
    surface = White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,
    surfaceTint = Blue700,

    inverseSurface = Slate800,
    inverseOnSurface = Slate50,
    outline = Slate500,
    outlineVariant = Slate300,
    scrim = Black,

    error = Red700,
    onError = White,
    errorContainer = Red100,
    onErrorContainer = Red950,
)

internal val DukkanDarkColorScheme = darkColorScheme(
    primary = Blue300,
    onPrimary = Blue950,
    primaryContainer = Blue900,
    onPrimaryContainer = Blue100,
    inversePrimary = Blue700,

    secondary = Orange300,
    onSecondary = Orange950,
    secondaryContainer = Orange800,
    onSecondaryContainer = Orange100,

    tertiary = Teal300,
    onTertiary = Teal950,
    tertiaryContainer = Teal800,
    onTertiaryContainer = Teal100,

    background = Slate950,
    onBackground = Slate50,
    surface = Slate900,
    onSurface = Slate50,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate300,
    surfaceTint = Blue300,

    inverseSurface = Slate100,
    inverseOnSurface = Slate900,
    outline = Slate400,
    outlineVariant = Slate700,
    scrim = Black,

    error = Red300,
    onError = Red950,
    errorContainer = Red800,
    onErrorContainer = Red50,
)
```

### Which Material role should be used?

```text
primary             main checkout/add-to-cart action, active navigation
secondary           promotions and secondary actions
tertiary            complementary accents and category highlights
background          app canvas behind the screen
surface             cards, sheets, dialogs, app bars
surfaceVariant      subdued sections, chips, input backgrounds
onX                 text or icons drawn on X
outline             borders that must remain clearly visible
outlineVariant      subtle separators
error               destructive and validation-error states
```

Always pair a container with its matching content color:

```kotlin
Button(
    colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ),
    onClick = onAddToCart,
) {
    Text("Add to cart")
}
```

Do not use `primary` as text on `background` merely because it is the brand color.
Use `onBackground` for ordinary content and reserve `primary` for intentional accents.

## 6. `ExtendedColors.kt`: CompositionLocal and `lerp`

Material 3 already provides error colors, but it has no standard roles for success,
warning, informational messages, sale labels, or favorite state. Those roles belong in
a small custom extension.

The names are semantic. Avoid ambiguous ramp names such as `high`, `higher`, and
`highest`: feature code should state the purpose of a color, not its palette intensity.

```kotlin
package com.example.dukkanapp.core.config.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

@Immutable
data class DukkanExtendedColors(
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

internal val LightExtendedColors = DukkanExtendedColors(
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

internal val DarkExtendedColors = DukkanExtendedColors(
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

internal val LocalDukkanExtendedColors = staticCompositionLocalOf {
    LightExtendedColors
}

fun lerp(
    start: DukkanExtendedColors,
    stop: DukkanExtendedColors,
    fraction: Float,
): DukkanExtendedColors = DukkanExtendedColors(
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
```

`@Immutable` tells Compose that the value cannot change internally. When the provided
instance changes, composables reading `LocalDukkanExtendedColors.current` recompose.
This is the same broad idea as a Flutter `InheritedWidget`: descendants read the
nearest value from the tree and update when that provided value changes.

`lerp` is useful when an animation needs an intermediate custom theme. It is not
required for an immediate system light/dark switch. Material's own colors still belong
in `ColorScheme`; do not duplicate every Material slot in this data class.

## 7. `AppDimens.kt`

Dimensions do not change between light and dark mode, so they do not need a
`CompositionLocal`.

```kotlin
package com.example.dukkanapp.core.utils.constants

import androidx.compose.ui.unit.dp

object AppDimens {
    // Spacing: one 4 dp scale
    val Space2Xs = 4.dp
    val SpaceXs = 8.dp
    val SpaceSm = 12.dp
    val SpaceMd = 16.dp
    val SpaceLg = 24.dp
    val SpaceXl = 32.dp
    val Space2Xl = 48.dp

    // Corner radii
    val RadiusXs = 4.dp
    val RadiusSm = 8.dp
    val RadiusMd = 12.dp
    val RadiusLg = 16.dp
    val RadiusXl = 24.dp
    val RadiusFull = 999.dp

    // Reusable component measurements
    val ScreenHorizontalPadding = 16.dp
    val ButtonHeight = 52.dp
    val TextFieldMinHeight = 56.dp
    val ProductImageHeight = 180.dp
    val BottomBarHeight = 80.dp
    val TouchTargetMin = 48.dp
    val IconSm = 16.dp
    val IconMd = 24.dp
    val IconLg = 32.dp
    val BorderWidth = 1.dp
}
```

Avoid making a constant for every one-off measurement. Promote a value to
`AppDimens` only when it is part of the scale or reused across components.

## 8. `Shape.kt`

```kotlin
package com.example.dukkanapp.core.config.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import com.example.dukkanapp.core.utils.constants.AppDimens

internal val DukkanShapes = Shapes(
    extraSmall = RoundedCornerShape(AppDimens.RadiusXs),
    small = RoundedCornerShape(AppDimens.RadiusSm),
    medium = RoundedCornerShape(AppDimens.RadiusMd),
    large = RoundedCornerShape(AppDimens.RadiusLg),
    extraLarge = RoundedCornerShape(AppDimens.RadiusXl),
)
```

Usage:

```kotlin
Card(shape = MaterialTheme.shapes.medium) {
    // Product content
}
```

## 9. `Typography.kt`

Start with the platform sans-serif font. Add a custom font only when the actual font
files and licenses are available. The type scale remains the same in light and dark
themes; only text colors change.

```kotlin
package com.example.dukkanapp.core.config.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val DukkanFontFamily = FontFamily.SansSerif

internal val DukkanTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = DukkanFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = DukkanFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = DukkanFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = DukkanFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = DukkanFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = DukkanFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = DukkanFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = DukkanFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = DukkanFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = DukkanFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = DukkanFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = DukkanFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = DukkanFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = DukkanFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = DukkanFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
)
```

Usage:

```kotlin
Text(
    text = product.name,
    style = MaterialTheme.typography.titleMedium,
    color = MaterialTheme.colorScheme.onSurface,
)
```

## 10. `Theme.kt`

Dynamic Android colors are disabled by default. Enabling them replaces Dukkan's brand
palette with colors generated from the user's wallpaper. Keep them optional if strict
brand identity is required.

```kotlin
package com.example.dukkanapp.core.config.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext

object DukkanTheme {
    val extendedColors: DukkanExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalDukkanExtendedColors.current
}

@Composable
fun DukkanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }
        darkTheme -> DukkanDarkColorScheme
        else -> DukkanLightColorScheme
    }

    val extendedColors = if (darkTheme) {
        DarkExtendedColors
    } else {
        LightExtendedColors
    }

    CompositionLocalProvider(
        LocalDukkanExtendedColors provides extendedColors,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = DukkanTypography,
            shapes = DukkanShapes,
            content = content,
        )
    }
}
```

How propagation works:

```text
DukkanTheme chooses light or dark values
    -> MaterialTheme provides ColorScheme/Typography/Shapes
    -> CompositionLocalProvider provides DukkanExtendedColors
    -> descendant composables read the nearest active values
    -> changing darkTheme recomposes only readers of those values
```

Do not keep colors in a mutable singleton. Theme values must be selected at the root
and provided down the composition tree.

## 11. Persisting light, dark, and system mode

Do not store a Boolean because it cannot represent all three modes.

```kotlin
package com.example.dukkanapp.features.settings.domain.model

enum class ThemeMode {
    System,
    Light,
    Dark,
}
```

The app root resolves the preference:

```kotlin
@Composable
fun DukkanApp(
    themeMode: ThemeMode,
) {
    val systemDark = isSystemInDarkTheme()
    val useDarkTheme = when (themeMode) {
        ThemeMode.System -> systemDark
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    DukkanTheme(darkTheme = useDarkTheme) {
        AppNavHost()
    }
}
```

Store `ThemeMode` with DataStore in the settings feature. A
`SettingsRepository` exposes `Flow<ThemeMode>`, and the root ViewModel converts it to
UI state. The theme composable should not read DataStore directly.

## 12. E-commerce component examples

### Product card

```kotlin
@Composable
fun ProductCard(
    product: ProductUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column {
            ProductImage(product.imageUrl)
            Column(
                modifier = Modifier.padding(AppDimens.SpaceMd),
                verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceXs),
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = product.formattedPrice,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
```

### Sale badge

```kotlin
@Composable
fun SaleBadge(
    discountText: String,
    modifier: Modifier = Modifier,
) {
    val colors = DukkanTheme.extendedColors

    Surface(
        modifier = modifier,
        color = colors.sale,
        contentColor = colors.onSale,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = discountText,
            modifier = Modifier.padding(
                horizontal = AppDimens.SpaceXs,
                vertical = AppDimens.Space2Xs,
            ),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
```

### Order status

```kotlin
@Composable
fun OrderStatusBadge(
    status: OrderStatus,
    modifier: Modifier = Modifier,
) {
    val extended = DukkanTheme.extendedColors

    val (container, content) = when (status) {
        OrderStatus.Delivered ->
            extended.successContainer to extended.onSuccessContainer
        OrderStatus.Processing ->
            extended.infoContainer to extended.onInfoContainer
        OrderStatus.Pending ->
            extended.warningContainer to extended.onWarningContainer
        OrderStatus.Cancelled ->
            MaterialTheme.colorScheme.errorContainer to
                MaterialTheme.colorScheme.onErrorContainer
    }

    Surface(
        modifier = modifier,
        color = container,
        contentColor = content,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = status.label,
            modifier = Modifier.padding(
                horizontal = AppDimens.SpaceSm,
                vertical = AppDimens.SpaceXs,
            ),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
```

## 13. Feature implementation template

Example for the cart feature:

```text
features/cart/
├── data/
│   ├── local/CartDao.kt
│   ├── mapper/CartMapper.kt
│   └── CartRepositoryImpl.kt
├── domain/
│   ├── model/Cart.kt
│   ├── repository/CartRepository.kt
│   └── usecase/
│       ├── AddToCartUseCase.kt
│       ├── ObserveCartUseCase.kt
│       └── UpdateCartQuantityUseCase.kt
└── presentation/
    ├── CartContract.kt
    ├── CartScreen.kt
    └── CartViewModel.kt
```

Contract:

```kotlin
@Immutable
data class CartUiState(
    val isLoading: Boolean = true,
    val items: List<CartItemUiModel> = emptyList(),
    val total: String = "",
    val errorMessage: String? = null,
)

sealed interface CartAction {
    data class ChangeQuantity(
        val productId: String,
        val quantity: Int,
    ) : CartAction

    data object CheckoutClicked : CartAction
    data object RetryClicked : CartAction
}

sealed interface CartEvent {
    data object NavigateToCheckout : CartEvent
    data class ShowMessage(val message: String) : CartEvent
}
```

ViewModel:

```kotlin
class CartViewModel(
    observeCart: ObserveCartUseCase,
    private val updateQuantity: UpdateCartQuantityUseCase,
) : ViewModel() {

    val uiState: StateFlow<CartUiState> = observeCart()
        .map(CartUiMapper::map)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CartUiState(),
        )

    fun onAction(action: CartAction) {
        when (action) {
            is CartAction.ChangeQuantity -> viewModelScope.launch {
                updateQuantity(action.productId, action.quantity)
            }
            CartAction.CheckoutClicked -> {
                // Emit a one-time navigation event.
            }
            CartAction.RetryClicked -> {
                // Retry the failed operation.
            }
        }
    }
}
```

The screen renders `CartUiState` and sends `CartAction`. It must not call a DAO,
Retrofit service, or repository directly.

## 14. Preview both themes

Every reusable component and important screen should have light and dark previews.

```kotlin
@Preview(name = "Light", showBackground = true)
@Composable
private fun ProductCardLightPreview() {
    DukkanTheme(darkTheme = false) {
        Surface {
            ProductCard(
                product = ProductUiModel.preview,
                onClick = {},
            )
        }
    }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun ProductCardDarkPreview() {
    DukkanTheme(darkTheme = true) {
        Surface {
            ProductCard(
                product = ProductUiModel.preview,
                onClick = {},
            )
        }
    }
}
```

## 15. Contrast verification

Color pairs should meet WCAG contrast guidance:

- normal text: at least `4.5:1`;
- large text: at least `3:1`;
- controls, icons, and visible boundaries: at least `3:1`.

The palette above intentionally uses strong foreground/container pairs. Still add a
test so future edits do not silently reduce contrast:

```kotlin
package com.example.dukkanapp.core.config.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeContrastTest {

    @Test
    fun light_primary_content_meets_AA() {
        assertContrast(
            foreground = DukkanLightColorScheme.onPrimary,
            background = DukkanLightColorScheme.primary,
            minimum = 4.5f,
        )
    }

    @Test
    fun dark_surface_content_meets_AA() {
        assertContrast(
            foreground = DukkanDarkColorScheme.onSurface,
            background = DukkanDarkColorScheme.surface,
            minimum = 4.5f,
        )
    }

    private fun assertContrast(
        foreground: Color,
        background: Color,
        minimum: Float,
    ) {
        val lighter = maxOf(foreground.luminance(), background.luminance())
        val darker = minOf(foreground.luminance(), background.luminance())
        val ratio = (lighter + 0.05f) / (darker + 0.05f)

        assertTrue(
            "Expected contrast >= $minimum, actual ratio was $ratio",
            ratio >= minimum,
        )
    }
}
```

Expand this into a parameterized test covering every `container`/`onContainer` pair.
Automated checks complement, but do not replace, previews on actual devices.

## 16. Migration from the generated starter theme

Current starter files live under:

```text
com.example.dukkanapp.ui.theme
```

Migrate in this order:

1. Create `core/utils/constants/AppDimens.kt`.
2. Create the five files under `core/config/theme/`.
3. Replace `DukkanAppTheme` with the new `DukkanTheme`.
4. Update the import in `MainActivity`.
5. Keep `enableEdgeToEdge()` in `MainActivity`; do not duplicate window handling in
   the theme.
6. Remove the generated purple palette only after no imports reference it.
7. Add light and dark previews.
8. Add contrast tests.
9. Add DataStore-backed `ThemeMode` when the settings feature is created.

Do not enable dynamic colors by default unless replacing the brand palette is an
explicit product decision.

## 17. Definition of done

The theme and architecture are correctly integrated when:

- no feature contains a `Color(0x...)` literal;
- standard roles come from `MaterialTheme.colorScheme`;
- app-specific status roles come from `DukkanTheme.extendedColors`;
- all text styles come from `MaterialTheme.typography`;
- all shared shapes come from `MaterialTheme.shapes`;
- reusable dimensions come from `AppDimens`;
- light, dark, and system modes all work;
- reusable UI has both light and dark previews;
- important foreground/background pairs pass contrast tests;
- ViewModels expose immutable state and receive user actions;
- domain code has no Android UI, Room, Retrofit, or DTO dependencies;
- features do not reach into each other's internal layers.

