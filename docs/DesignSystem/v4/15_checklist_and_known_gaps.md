# 15 — Checklist, QA and known gaps

**File:** none. Use this doc at the end of the migration and before every release.

## 1. Per-doc checklist

| Doc | Done when |
|---|---|
| 02 Palette | `AppPalette.kt` compiles; every hex matches doc 01 (blue, azure, slate, night, green, amber, red, orange, rose) |
| 03 Semantic colors | `AppColors` (with `sheet`), `LightAppColors`, `DarkAppColors` (Figma `01b` values), `LocalAppColors` compile |
| 04 Color scheme | `AppColors.toColorScheme(isDark)` compiles with named arguments; sheets/dialogs use `sheet`; no dynamic color |
| 05 Gradients / shadows | `AppGradients` (with `bottomFade`), `AppAurora` (3 blobs), `cardHighlight` + `Modifier.cardHighlight()`, `AppShadows` (with `screen`), `Modifier.appShadow` (draws the dark outline) compile; `dropShadow` resolves |
| 06 Typography | 5 Plus Jakarta Sans + 4 Cairo `.ttf` in `res/font`; `LatinTypography`, `ArabicTypography`, `LatinTextStyles`, `ArabicTextStyles` compile |
| 07 Dimens / shapes | old `iconSm` renamed to `icon` first; one radius scale (10/14/16/20/28/full); `navBar` 66 (`teamNavBar` 64), Figma `01c` size tokens added (`buttonLg`…`screenHeight`, `chip` 34, `iconMd` 18); `AppShapes` (10/14/16/20/28), `AppPillShape` moved and compile |
| 08 Theme | new `AppTheme(darkTheme, isArabic)` used in `MainActivity` and every preview; theme mode saved in DataStore; old theme files deleted |
| 09 Icons / logo | 76 Lucide icons (incl. eye-off, user-plus, star-filled, wifi-off, search-x, mic), `AppIcons` compiles, logo, adaptive launcher icon (`05b`), splash updated, Iconsax removed |
| 10 Buttons | primary gradient + glow; pressed = solid `brandPressed` with no glow; disabled = field + tertiary; loading = spinner replaces trailing icon |
| 11 Text fields | pill fields; 6 states match the doc 01 input table; disabled drawn with tokens (no alpha) |
| 12 Selection / chips / badges | checkbox, radio, switch, chips, badges, rating (filled gold star), empty states and shimmer skeletons match |
| 13 Aurora / scaffold | `AppScreenBackground` (Compact / Short / Tall) matches 02.1, 02.2 and 02.6, light and dark |
| 14 Auth refactor | Welcome, Log in, Sign up, Verify email (02.6 / 02.7) match Figma; Log in matches 13.2 (dark) and 13.4 (Arabic) |

## 2. Build commands

Run them in the project root (PowerShell or Git Bash):

```bash
./gradlew :app:assembleDebug          # compiles everything
./gradlew :app:lintDebug              # unused resources, missing translations
./gradlew :app:testDebugUnitTest      # unit tests (ThemeRepositoryImpl mapping, etc.)
```

On Windows PowerShell, use `.\gradlew.bat` instead of `./gradlew`.

## 3. Leak check: raw values in features

Tokens only help if features use them. These greps must return **nothing** (run them in Git Bash):

```bash
# Raw colors outside the theme package
grep -rn "Color(0x" app/src/main/java | grep -v "core/designsystem/theme/"

# Palette read outside the theme package
grep -rn "AppPalette\." app/src/main/java | grep -v "core/designsystem/theme/"

# Hard-coded dp / sp in features and components (previews excluded by hand)
grep -rnE "[0-9]+(\.[0-9]+)?\.(dp|sp)\b" app/src/main/java/com/example/dukkanapp/features app/src/main/java/com/example/dukkanapp/core/common

# Old theme packages still imported
grep -rnE "core\.config\.theme|core\.utils\.constants\.(AppDimens|AppShapes|AppExtendedColors|LocalAppExtendedColors)" app/src/main/java

# Removed radius names (only the deprecated aliases may remain)
grep -rnE "radiusField|radiusXl|radius2Xl" app/src/main/java | grep -v "AppDimens.kt"

# Disabled inputs/buttons drawn with alpha instead of tokens (selection controls may still use alpha)
grep -rn "alpha(if (enabled)" app/src/main/java/com/example/dukkanapp/core/designsystem/components/textfield app/src/main/java/com/example/dukkanapp/core/designsystem/components/button

# Old icon libraries
grep -rnE "Iconsax|Icons\.(Filled|Outlined|AutoMirrored)" app/src/main/java

# Old font
grep -rn "konnect" app/src
```

Allowed exceptions: `android.graphics.Color.TRANSPARENT` in `MainActivity` (system bars), `res/values/colors.xml` (splash) and the launcher vectors in `res/drawable/ic_launcher_*.xml`.

## 4. Manual QA matrix

Check every screen you touched in all 6 combinations:

| | English | Français | العربية (RTL) |
|---|---|---|---|
| **Light** | ☐ | ☐ | ☐ |
| **Dark** | ☐ | ☐ | ☐ |

For each cell:

- [ ] Background `#F7F9FB` (light) / `#0B1220` (dark), with no white flash at launch.
- [ ] Primary button: horizontal blue gradient, glow below (blue in light, azure in dark), 52 dp tall, pill shape. Pressed: solid darker blue, no glow.
- [ ] Input: pill, grey fill at rest; white fill, blue 1.5 border and soft 3 dp pill ring on focus; red fill and border on error; green border on success; disabled grey and fully opaque.
- [ ] Dark cards have a thin `#24314F` outline next to their shadow.
- [ ] Latin text in Plus Jakarta Sans. Arabic text in **Cairo**, right-aligned, with taller lines; arrows mirrored (back arrow points right). Compare with Figma 13.4.
- [ ] Long French strings ("Mot de passe oublié ?") don't overflow buttons.
- [ ] Font scale 200 % (Settings → Display → Font size): nothing is clipped, and fields grow instead of cutting text.
- [ ] TalkBack reads every icon button (back, eye toggle) and none of the decorative icons.
- [ ] Status bar icons readable in both modes.
- [ ] Theme mode Light / Dark / System on screen 11.3 applies instantly and survives a restart.
- [ ] Lists show the empty state when empty and the shimmer skeleton while loading (Figma 14).
- [ ] Launcher icon in circle, squircle and themed modes (Figma 05b).

## 5. Contrast spot checks

| Pair | Ratio (approx.) | Result |
|---|---|---|
| `textPrimary` on `background` (light) | 17 : 1 | AA |
| `textSecondary` on `background` (light) | 4.6 : 1 | AA |
| `textTertiary` on `field` (light, placeholder) | 2.6 : 1 | Below AA. Fine for placeholders (WCAG exempts them), never for real text |
| White on `button` gradient end (Blue700) | 6.7 : 1 | AA |
| White on gradient start (Azure500 #3B82F6) | 3.7 : 1 | AA for bold 15 sp only as "large-ish". Keep labels Bold |
| `brand` text on `background` (light) | 5.0 : 1 | AA |
| `brand` (Blue400) on `background` (dark, Night950) | ≈ 5 : 1 | AA (Figma: "blue-400 for 4.5:1 contrast on night-950") |
| `textPrimary` (Slate100) on `background` (dark) | ≈ 17 : 1 | AA |
| `textSecondary` (Slate400) on `card` (dark, Night900) | ≈ 7 : 1 | AA |
| `brandSoftText` (Blue700) on `brandSoft` (light tonal, badges) | 7.6 : 1 | AA |
| `brandSoftText` (Blue300) on `brandSoft` (Blue950), dark | ≈ 7 : 1 | AA, also for the 10.5 sp badges |
| `warningText` on `warningSoft` | 5.6 : 1 | AA |

Measure with any WCAG contrast checker if you change a token.

## 6. Known gaps: Figma vs code

### Closed by the latest Figma update

| Was | Now |
|---|---|
| Light mode only | Official dark tokens in `01b · Dark mode` (doc 03, 05) |
| Two radius scales (10/14/18/22/28 vs 10/16/20/28) | One scale 10 / 14 / 16 / 20 / 28 / full (doc 07) |
| No specimen for Display/Medium, Headline/Medium, Label/Medium, Field/Text, Helper | All defined in `01 · Foundations` (doc 06) |
| Auth titles drawn at 26 / 25 / 24 | All 24 ExtraBold (`headlineLarge`) |
| Arabic font not named | Cairo with its own Arabic type scale (doc 06) |
| eye-off, user-plus, filled star missing from the icon sheet | On the sheet (76 icons, doc 09) |
| No pressed / disabled / loading button states | `03b · Atom states` (doc 10) |
| Disabled input = 50 % opacity | Drawn with tokens (doc 11) |
| No Android adaptive icon spec | `05b · Android app icon` (doc 09) |
| Email verification not designed | 02.6 / 02.7 (doc 14) |
| Bottom-nav total "39.1 TND" | "39.900 TND" (3 decimals) |
| No dark `status/success-text`, no `brand/soft-text` | Both defined in `01` + `01b`: `successText`, `brandSoftText` (doc 03) |
| Dark `status/error-border` = `status/error-soft` (invisible border) | Dark error-border = red-400 at 50 % (doc 03) |
| Dark text on `brandSoft` ≈ 3.4 : 1 | `brandSoftText` (blue-300 in dark) ≈ 7 : 1 |
| Dark `float`, `deal`, `screen` shadows missing | All 5 dark shadows in `01b`; all dark surfaces add the night-700 border (doc 05) |
| Dark aurora blob colors missing, white blob position unknown | `13.1b · Header glow · spec`: 3 blobs, dark blue-500 35 % / blue-800 45 % / amber-300 8 %; white 35 % is the **card highlight** inside gradient cards (docs 05, 13) |
| Compose chapter mapped Caption to `bodySmall` | Chapter now maps `bodySmall` = Body/Small 13/18 (Arabic Cairo Regular 13/20) and Caption is a separate style, same as our code (doc 06) |

### Still open

| # | Figma | Code | Why / how to close |
|---|---|---|---|
| 1 | No semantic token for `borderStrong`, `dealSoft`, `inverse`, `onInverse`, `scrim`, `shadow` | Derived values (doc 03 table) | Components need them; values follow Figma's dark rules. Ask the designer if they want official tokens |
| 2 | Only 5 Arabic styles specified (+ bodySmall and Caption in the Compose chapter) | Other slots derived with Figma's rule (one weight lighter, ≈ 1.5× line height) | Ask for the full Arabic scale, or confirm the rule |
| 3 | Figma blur values | `Modifier.dropShadow` radius = Figma blur | Compose and Figma blur may differ slightly; compare the catalog preview and tweak `blur` if needed. If `dropShadow` is unavailable, the `Modifier.shadow` fallback won't match |
| 4 | 135° gradients | `Offset.Zero → Offset.Infinite` (corner to corner) | Equals 135° only on square shapes; on wide banners the angle is flatter. Acceptable visually |
| 5 | Focus ring (3 px soft blue pill) | Drawn by `AppTextField` (doc 11) outside the box | Material has no focus-ring slot |
| 6 | Tonal / Outline pressed state colors | `brandRing` / `field` (doc 10) | `03b` shows them; values read from the frames, confirm if they look off |
| 7 | Splash screen text "Shop smart. Delivered fast." | System splash shows only the mark | Put the slogan on the first Compose screen if needed |
| 8 | Figma colors for Material widgets that don't exist in Figma (snackbar, menus, date picker) | Derived through `toColorScheme` | Review the first time you use such a widget |
| 9 | Pager dot 22 × 6, onboarding illustration size | Feature sizes in `AppDimens` | Move into the onboarding feature when you refactor it |
| 10 | Bottom nav selected pill is blue | Material `NavigationBar` uses `secondaryContainer` (orange tint) | Build a custom bottom bar instead of Material's |
| 11 | Empty-state tile (72) and icon (32) sizes, skeleton band width | Local constants in doc 12 | Promote to `AppDimens` if reused |

## 7. Definition of done

- [ ] All greps in section 3 return nothing (except the allowed exceptions).
- [ ] The QA matrix in section 4 is fully ticked for Welcome, Log in, Sign up, Verify email and Language.
- [ ] `ThemeCatalog` previews (light, dark, Arabic) look like Figma `01 · Foundations` and `01b · Dark mode`.
- [ ] Every open gap in section 6 is either closed or has a note in the PR description.
