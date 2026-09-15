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

