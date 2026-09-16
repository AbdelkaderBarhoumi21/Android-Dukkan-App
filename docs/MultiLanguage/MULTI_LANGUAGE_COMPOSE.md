# Multi-Language (i18n) in Kotlin Jetpack Compose — Multi-Module Setup

Languages: English (default), French, Arabic (RTL) · Package root: adjust to your app id

---

## 0. The core problem, and how it maps from what you know

In Flutter, `l10n.yaml` + `.arb` files generate a single `AppLocalizations` class at build time — one strongly-typed object with a getter per key, and `context.locale` / `flutter_localizations` handles switching + RTL.

Android solved the *same* problem years earlier, but the mechanism is inverted: instead of one generated class, **the resource system itself is locale-aware**. `strings.xml` isn't just for i18n — it's the *general* "don't hardcode text" mechanism Android has always used, and localization is just resource qualifiers layered on top of it.

| Flutter concept | Android/Compose equivalent |
|---|---|
| `.arb` file per language | `strings.xml` per language, in a qualified folder |
| `l10n.yaml` config + codegen | Nothing to configure — it's built into the resource system, zero build step |
| `AppLocalizations.of(context).continueButton` | `stringResource(R.string.continue_button)` |
| `context.locale` / `Locale('fr')` | `Locale` + `AppCompatDelegate.setApplicationLocales(...)` |
| `flutter_localizations` RTL handling | `Modifier` start/end (not left/right) + `android:supportsRtl="true"` — automatic |
| Generated `AppLocalizations` class safety | `R.string.xxx` — also compile-time-safe, just not "one object" |

**The most recommended, most-used approach today (2025+ Jetpack, confirmed current in Android's official guidance) is:**
1. `strings.xml` + locale-qualified resource folders (`values-fr/`, `values-ar/`) — not a 3rd-party i18n library. This is what 95%+ of production Android apps use.
2. `stringResource()` in Compose to read them.
3. **`AppCompatDelegate.setApplicationLocales()`** (androidx.appcompat 1.6.0+) for in-app language switching — this superseded the old manual `Context`-wrapping/`attachBaseContext` hacks you'll still find in older tutorials. It's backed natively by Android 13's per-app language feature, with automatic backward-compat down to API 21.

No `.arb`-equivalent library, no codegen step — that's a deliberate simplification versus Flutter, not a missing feature.

---

## 1. Multi-module strategy

### 1.1 Where strings live

Android resources **merge globally at build time** — every module's `res/values/strings.xml` gets combined into one resource table for the final APK. This has one direct consequence you must design around: **string resource names (`R.string.xxx`) are a single global namespace across all modules.** Two modules both defining `<string name="continue_button">` is a **build error** (duplicate resource), not a module-scoped override like you might expect from Dart's per-file imports.

Two viable strategies:

| Strategy | When to use |
|---|---|
| **A. Centralized `:core:localization` module** — all `strings.xml` for the whole app live in one module; every feature module depends on it and reads `R.string.*` from there. | ✅ Recommended for your case (3 languages, learning project, want one source of truth) |
| **B. Per-module strings, prefixed by module** — `feature_auth_continue`, `feature_profile_continue`, etc. | Large teams, many feature modules owned by different people, want to avoid one shared bottleneck file |

Given your project scale, go with **A**. Structure:

```
core/
└── localization/
    ├── build.gradle.kts
    └── src/main/
        ├── res/
        │   ├── values/strings.xml       ← English (default — no qualifier = fallback)
        │   ├── values-fr/strings.xml     ← French
        │   └── values-ar/strings.xml     ← Arabic
        └── kotlin/.../localization/
            ├── SupportedLanguage.kt
            └── LocaleManager.kt
```

Every feature module adds `implementation(project(":core:localization"))` and calls `stringResource(R.string.xxx)` — same `R` class, resolved from the merged resource table, regardless of which module the Composable lives in.

---

## 2. The `strings.xml` files

### 2.1 `core/localization/src/main/res/values/strings.xml` (English — default)

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="language_selection_title">Choose the language</string>
    <string name="language_selection_subtitle">Select your preferred language below. This helps us serve you better.</string>
    <string name="language_selection_you_selected">You Selected</string>
    <string name="language_selection_all_languages">All Languages</string>
    <string name="language_selection_search_hint">Search</string>
    <string name="action_continue">Continue</string>
    <string name="cd_back">Back</string>
</resources>
```

### 2.2 `core/localization/src/main/res/values-fr/strings.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="language_selection_title">Choisissez la langue</string>
    <string name="language_selection_subtitle">Sélectionnez votre langue préférée ci-dessous. Cela nous aide à mieux vous servir.</string>
    <string name="language_selection_you_selected">Vous avez sélectionné</string>
    <string name="language_selection_all_languages">Toutes les langues</string>
    <string name="language_selection_search_hint">Rechercher</string>
    <string name="action_continue">Continuer</string>
    <string name="cd_back">Retour</string>
</resources>
```

### 2.3 `core/localization/src/main/res/values-ar/strings.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="language_selection_title">اختر اللغة</string>
    <string name="language_selection_subtitle">اختر لغتك المفضلة أدناه. هذا يساعدنا على خدمتك بشكل أفضل.</string>
    <string name="language_selection_you_selected">لقد اخترت</string>
    <string name="language_selection_all_languages">جميع اللغات</string>
    <string name="language_selection_search_hint">بحث</string>
    <string name="action_continue">متابعة</string>
    <string name="cd_back">رجوع</string>
</resources>
```

`values/` (no suffix) is the **fallback** — same idea as `.arb`'s `template-arb-file`, it's what's used if a qualified folder is missing a key, or for any locale you haven't explicitly translated (Spanish, German, etc. in your mockup would fall back to English until you add `values-es/`, `values-de/`).

**Naming convention note** (this is the "why," not just the rule): prefix by *screen/feature*, not by *literal English text*. `language_selection_title` survives a copy change; `choose_the_language` doesn't, and you'd be renaming the key (breaking every reference) every time a designer tweaks a sentence.

---

## 3. Reading strings in Compose

```kotlin
Text(text = stringResource(R.string.language_selection_title))
```

That's the entire API surface for the simple case — no `AppLocalizations.of(context)` indirection needed, `stringResource` is a top-level `@Composable` function that resolves against the current `Configuration`'s locale automatically.

With arguments (equivalent to `.arb` placeholders):

```xml
<!-- values/strings.xml -->
<string name="greeting">Hello, %1$s!</string>
```
```kotlin
Text(text = stringResource(R.string.greeting, userName))
```

Plurals (Android's ICU-backed plural rules — more powerful than most `.arb` plural setups since it's locale-aware out of the box for languages with complex plural forms, which **Arabic has 6 plural categories**, not just singular/plural):

```xml
<plurals name="items_count">
    <item quantity="one">%d item</item>
    <item quantity="other">%d items</item>
</plurals>
```
```kotlin
val text = pluralStringResource(R.plurals.items_count, count, count)
```

---

## 4. RTL — why Arabic "just works" if you follow one rule

Compose mirrors layout automatically for RTL locales via `LayoutDirection`, **but only if you use direction-agnostic modifiers.** This is the one rule that matters:

| ❌ Don't use (physical) | ✅ Use instead (logical) |
|---|---|
| `Modifier.padding(start = ...)` — wait, this is already correct | |
| `Alignment.CenterStart` / `CenterEnd` | ✅ already logical, keep using these |
| `Arrangement.Start` / `End` | ✅ already logical, keep using these |
| `Modifier.padding(left = 16.dp)` | `Modifier.padding(start = 16.dp)` |
| Manually flipping icons/arrows with `scaleX = -1f` yourself | Let Compose mirror automatically, or wrap only genuinely directional icons (like a back-chevron) in `Modifier.mirror()`/check `LocalLayoutDirection` |

Compose's own `padding`, `Arrangement.Start/End`, `TextAlign.Start/End`, and `Alignment.CenterStart/CenterEnd` were designed logical-first — Android's `left/right`-style APIs are the legacy View-system ones. As long as you stick to Compose's own vocabulary (which you likely already are), RTL mirroring is **automatic**, zero extra code.

Two things you must still do manually:

**1. Manifest:**
```xml
<application
    android:supportsRtl="true"
    ...>
```

**2. Preview both directions during development** (see §7) — don't just trust it, verify it, especially for the back-chevron icon which should visually flip to point right in Arabic.

---

## 5. Runtime language switching — the MVVM way

### 5.1 `SupportedLanguage.kt`

```kotlin
package com.example.dukkanapp.core.localization

data class SupportedLanguage(
    val code: String,       // BCP-47 tag, e.g. "en", "fr", "ar"
    val displayName: String,
    val flagAssetName: String, // maps to a drawable, see §6
)

val SupportedLanguages = listOf(
    SupportedLanguage("ar", "Arabic", "flag_ar"),
    SupportedLanguage("en", "English", "flag_en"),
    SupportedLanguage("fr", "French", "flag_fr"),
)
```

### 5.2 `LocaleManager.kt` — wraps the platform API

```kotlin
package com.example.dukkanapp.core.localization

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocaleManager @Inject constructor() {

    /** Applying a locale here is enough — AppCompat + the system handle
     *  recreating the active Activity's resources with the new language.
     *  No manual Context-wrapping, no attachBaseContext override needed. */
    fun setAppLocale(languageCode: String) {
        val localeList = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    fun currentLocaleTag(): String? =
        AppCompatDelegate.getApplicationLocales().toLanguageTags().takeIf { it.isNotBlank() }
}
```

**Why this is "the recommended way" and not the old `Context`-wrapping approach**: pre-2022, every tutorial had you override `attachBaseContext`, build a new `Configuration`, wrap the base `Context` per-Activity, and manually persist the choice yourself. `AppCompatDelegate.setApplicationLocales()` (stable since appcompat 1.6.0) replaces all of that — it persists the choice for you (survives process death, before Android 13 it uses an AppCompat-internal service to remember it; from Android 13 it's backed by the actual OS per-app language setting, visible in system Settings → Apps → Your App → Language), and it triggers the correct Activity recreation automatically. You just call one function.

### 5.3 Persisting the *user's explicit choice* (for your own UI state / "You Selected" card)

`AppCompatDelegate` already persists the applied locale at the OS level — you don't need DataStore just to *reapply* it on next launch. You *do* still want your own persisted state if your UI needs to know "what did the user pick" reactively as a `StateFlow` (e.g. to pre-select the radio button on this screen, matching `state.copy(...)` patterns from TaskPulse):

```kotlin
// core/localization/LanguagePreferenceRepository.kt
package com.example.dukkanapp.core.localization

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LanguagePreferenceRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val localeManager: LocaleManager,
) {
    private val languageKey = stringPreferencesKey("selected_language_code")

    val selectedLanguageCode: Flow<String> = dataStore.data.map { prefs ->
        prefs[languageKey] ?: localeManager.currentLocaleTag() ?: "en"
    }

    suspend fun selectLanguage(code: String) {
        dataStore.edit { it[languageKey] = code }
        localeManager.setAppLocale(code)
    }
}
```

### 5.4 `LanguageSelectionViewModel.kt`

```kotlin
package com.example.dukkanapp.feature.language

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dukkanapp.core.localization.LanguagePreferenceRepository
import com.example.dukkanapp.core.localization.SupportedLanguage
import com.example.dukkanapp.core.localization.SupportedLanguages
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LanguageSelectionUiState(
    val query: String = "",
    val selectedCode: String = "en",
    val languages: List<SupportedLanguage> = SupportedLanguages,
) {
    val filteredLanguages: List<SupportedLanguage>
        get() = if (query.isBlank()) languages
                else languages.filter { it.displayName.contains(query, ignoreCase = true) }

    val selectedLanguage: SupportedLanguage?
        get() = languages.firstOrNull { it.code == selectedCode }
}

sealed interface LanguageSelectionEvent {
    data class OnQueryChanged(val query: String) : LanguageSelectionEvent
    data class OnLanguageClicked(val code: String) : LanguageSelectionEvent
    data object OnContinueClicked : LanguageSelectionEvent
}

@HiltViewModel
class LanguageSelectionViewModel @Inject constructor(
    private val repository: LanguagePreferenceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LanguageSelectionUiState())
    val state: StateFlow<LanguageSelectionUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.selectedLanguageCode.collect { code ->
                _state.update { it.copy(selectedCode = code) }
            }
        }
    }

    fun onEvent(event: LanguageSelectionEvent) {
        when (event) {
            is LanguageSelectionEvent.OnQueryChanged ->
                _state.update { it.copy(query = event.query) }

            is LanguageSelectionEvent.OnLanguageClicked ->
                _state.update { it.copy(selectedCode = event.code) } // optimistic UI, applied on Continue

            LanguageSelectionEvent.OnContinueClicked -> viewModelScope.launch {
                repository.selectLanguage(_state.value.selectedCode)
                // NavHost recreates Activity/content due to setApplicationLocales — navigate after, if needed
            }
        }
    }
}
```

Note the deliberate choice: tapping a row only updates `selectedCode` locally (feels instant, matches the mockup showing a selected state before you commit) — the actual locale change + Activity recreation only fires on **Continue**. Applying it on every tap would recreate the Activity mid-scroll, which is jarring.

---

## 6. The flag icons — why VectorDrawable won't work here, and the fix

### 6.1 The actual constraint

Android's `VectorDrawable` format (what an SVG "becomes" when you import via Android Studio's Vector Asset tool) supports paths, solid fills, and **simple linear/radial gradients** — but it does **not** support:
- Blur / drop-shadow filters (`feGaussianBlur`, `filter="url(#blur)"` in SVG)
- Complex multi-layer gradient blends, opacity masks, or SVG `<filter>` elements in general

Your flag icons in the mockup are circular, with a soft gradient sheen and what looks like a subtle blur/glow — exactly the feature set VectorDrawable can't render. Android Studio's SVG-to-Vector importer will either **silently drop** the blur/filter (flag renders flat, wrong) or **reject the file outright** with an "unsupported node" error. This isn't a bug to work around — it's a hard format limitation, so don't spend time fighting the importer.

### 6.2 The fix: rasterize instead of vectorize

For any icon using filter effects, the standard Android approach is a **raster image** (PNG or WebP), not a vector. WebP over PNG because it's 25–35% smaller at equal visual quality, and Android has had first-class WebP support since API 18.

**Pipeline:**

1. **Export the flag SVGs from Figma** at your largest realistically-needed size. Since these render as small circular avatars (~32–40dp in your mockup), export at **4x** that: `32dp × 4 = 128px` per flag is plenty — you don't need 512px source files for a UI element this small.

2. **Convert SVG → PNG** (rasterizing, so blur/gradients render correctly) using a real rendering engine — not Android's importer. Two good free options:
   - **Figma itself**: right-click the flag component → Export → PNG @4x. Simplest if you already have the file open — Figma renders blur/gradients correctly natively.
   - **CLI (if you're scripting a batch export from raw `.svg` files)**: [`resvg`](https://github.com/RazrFalcon/resvg) is the most accurate open-source SVG renderer (handles filters correctly, unlike `rsvg-convert`):
     ```bash
     resvg flag_ar.svg flag_ar.png --width 128 --height 128
     ```

3. **Convert PNG → WebP** using Android Studio's built-in tool (right-click the PNG in `res/drawable` → **Convert to WebP**), or via `cwebp` CLI for batch/scripted conversion:
   ```bash
   cwebp -q 90 flag_ar.png -o flag_ar.webp
   ```

4. **Drop into a single density bucket.** For small, non-scaling decorative icons like this, you do **not** need the full `mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi` density pyramid — that's for icons that need to look crisp across a wide size range. Put the 128px WebP straight into `res/drawable/` (density-independent default bucket) and size it in Compose with `Modifier.size(32.dp)`; Compose/the image loader downscales it, and 128px source gives you enough headroom for xxxhdpi screens without maintaining 5 separate files.

```
core/localization/src/main/res/drawable/
├── flag_ar.webp
├── flag_en.webp
└── flag_fr.webp
```

### 6.3 Alternative worth knowing: skip the asset pipeline entirely with emoji flags

Since Android ships full-color emoji support system-wide (`NotoColorEmoji`), Unicode regional-indicator flag sequences render natively with zero assets, zero pipeline:

```kotlin
Text("🇫🇷", fontSize = 24.sp)  // renders as an actual flag glyph, no drawable needed
```

This won't match your Figma design pixel-for-pixel (no blur/gradient sheen, and emoji flag style varies slightly by OEM), but it's worth knowing as the "good enough for an MVP" fallback if you ever want to add a 10th language without commissioning a new icon.

---

## 7. Screen implementation — `LanguageSelectionScreen.kt`

```kotlin
package com.example.dukkanapp.feature.language

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.dukkanapp.core.localization.R as L10nR

@Composable
fun LanguageSelectionRoute(
    onBack: () -> Unit,
    onContinue: () -> Unit,
    viewModel: LanguageSelectionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LanguageSelectionScreen(
        state = state,
        onBack = onBack,
        onEvent = viewModel::onEvent,
        onContinue = onContinue,
    )
}

@Composable
fun LanguageSelectionScreen(
    state: LanguageSelectionUiState,
    onBack: () -> Unit,
    onEvent: (LanguageSelectionEvent) -> Unit,
    onContinue: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Button(
                onClick = {
                    onEvent(LanguageSelectionEvent.OnContinueClicked)
                    onContinue()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .height(56.dp),
                shape = MaterialTheme.shapes.large,
            ) {
                Text(stringResource(id = L10nR.string.action_continue))
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 24.dp)
                .fillMaxSize(),
        ) {
            Spacer(Modifier.height(16.dp))
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack, // auto-flips for RTL — see §4
                    contentDescription = stringResource(id = L10nR.string.cd_back),
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(id = L10nR.string.language_selection_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(id = L10nR.string.language_selection_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(id = L10nR.string.language_selection_you_selected),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            state.selectedLanguage?.let { selected ->
                LanguageRow(language = selected, selected = true, onClick = {}, outlined = true)
            }

            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(id = L10nR.string.language_selection_all_languages),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = state.query,
                onValueChange = { onEvent(LanguageSelectionEvent.OnQueryChanged(it)) },
                placeholder = { Text(stringResource(id = L10nR.string.language_selection_search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(state.filteredLanguages, key = { it.code }) { language ->
                    LanguageRow(
                        language = language,
                        selected = language.code == state.selectedCode,
                        onClick = { onEvent(LanguageSelectionEvent.OnLanguageClicked(language.code)) },
                        outlined = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageRow(
    language: com.example.dukkanapp.core.localization.SupportedLanguage,
    selected: Boolean,
    onClick: () -> Unit,
    outlined: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .then(
                if (selected && !outlined) Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        AsyncImage( // or `Image(painterResource(...))` — see note below
            model = flagDrawableRes(language.flagAssetName),
            contentDescription = null,
            modifier = Modifier.size(32.dp).clip(CircleShape),
        )
        Spacer(Modifier.width(12.dp))
        Text(language.displayName, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (selected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .padding(2.dp),
            )
        }
    }
}
```

**On `AsyncImage` vs `painterResource`**: since your flags are static, bundled `.webp` drawables (not remote URLs), prefer plain `Image(painterResource(id = R.drawable.flag_ar), ...)` — no need for Coil's `AsyncImage` when there's no network fetch involved; it adds a caching layer you don't need for a bundled asset. I used `AsyncImage` above only if you're loading flags from a CDN — swap to `painterResource` for the bundled-WebP case, which is what §6 sets you up for.

---

## 8. Previewing every locale + direction during development

```kotlin
@Preview(name = "English", locale = "en")
@Preview(name = "French", locale = "fr")
@Preview(name = "Arabic (RTL)", locale = "ar")
@Composable
private fun LanguageSelectionScreenPreview() {
    DukkanTheme {
        LanguageSelectionScreen(
            state = LanguageSelectionUiState(selectedCode = "ar"),
            onBack = {}, onEvent = {}, onContinue = {},
        )
    }
}
```

The `locale` parameter on `@Preview` (stable Compose tooling feature) renders each variant side-by-side in Android Studio's Split/Design view — this is your fastest way to catch a string that overflows in French (French strings run ~15–20% longer than English) or a layout that doesn't mirror correctly in Arabic, without installing on a device and changing system language each time.

---

## 9. File checklist

| File | Module |
|---|---|
| `res/values/strings.xml`, `values-fr/strings.xml`, `values-ar/strings.xml` | `core/localization` |
| `res/drawable/flag_ar.webp`, `flag_en.webp`, `flag_fr.webp` | `core/localization` |
| `SupportedLanguage.kt` | `core/localization` |
| `LocaleManager.kt` | `core/localization` |
| `LanguagePreferenceRepository.kt` | `core/localization` |
| `AndroidManifest.xml` → `android:supportsRtl="true"` | `app` |
| `build.gradle.kts` → `implementation("androidx.appcompat:appcompat:1.7.0")` | `core/localization` (or wherever `LocaleManager` lives) |
| `LanguageSelectionViewModel.kt`, `LanguageSelectionScreen.kt` | `feature/language` (depends on `core/localization`) |
