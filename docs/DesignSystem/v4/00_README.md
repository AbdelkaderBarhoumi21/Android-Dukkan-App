# 00 — Dukkan Design System v4 (read me first)

This folder turns the Figma file **Dukkan App → Design System** page into Kotlin you type by hand.
Each doc creates or edits one or two small files. Follow the docs in order. Build after each one.

> One rule: if you type a hex color, a `dp`, or an `sp` inside a feature screen, it belongs in a theme file instead.

---

## 1. How a token reaches a pixel

```
Figma "01 · Foundations" + "01b · Dark mode"
        │
        ▼
AppPalette            raw hex, named like Figma          Blue600 = #2563EB, Night950 = #0B1220
(internal, doc 02)    nobody outside the theme package may read it
        │
        ▼
AppColors             semantic role, light + dark        brand = Blue600 (light) / Blue400 (dark)
(doc 03)              AppGradients / AppShadows (doc 05)
        │
        ├────────────► Material ColorScheme (doc 04)      primary = colors.brand
        │              Material Typography (doc 06)       labelLarge = Jakarta 15/20 Bold · Cairo 15/22 SemiBold
        │              Material Shapes (doc 07)           medium = 16.dp (radius/lg)
        ▼
AppTheme { }          composition root (doc 08)
                      provides LocalAppColors, LocalAppGradients,
                      LocalAppShadows, LocalAppTextStyles + MaterialTheme
        │
        ▼
Components (docs 10–14)   AppButton, AppTextField, AppCheckbox …
        │                 read AppTheme.colors / MaterialTheme.typography
        ▼
Feature screens           only call components and tokens, never hex
```

Two ways to read a token inside a composable:

```kotlin
MaterialTheme.colorScheme.primary      // Material slot (used by Material widgets)
AppTheme.colors.brand                  // Dukkan semantic token (everything Material has no slot for)
```

Both point to the same color. Prefer `AppTheme.colors` in our own components because the names match Figma.

---

## 2. Reading order

| # | Doc | Creates / changes |
|---|-----|-------------------|
| 00 | this file | nothing |
| 01 | `01_figma_token_reference.md` | nothing (reference tables only) |
| 02 | `02_palette.md` | `core/designsystem/theme/AppPalette.kt` |
| 03 | `03_semantic_colors.md` | `AppColors.kt` (replaces `AppExtendedColors.kt`) |
| 04 | `04_material_color_scheme.md` | `AppColorScheme.kt` (replaces `AppColors.kt` in `core/utils/constants`) |
| 05 | `05_gradients_and_shadows.md` | `AppGradients.kt`, `AppShadows.kt` |
| 06 | `06_typography.md` | `res/font/plus_jakarta_sans_*.ttf` + `cairo_*.ttf`, `AppTypography.kt`, `AppTextStyles.kt` (Latin + Arabic) |
| 07 | `07_dimens_and_shapes.md` | `AppDimens.kt`, `AppShapes.kt` (moved + rewritten, one radius scale) |
| 08 | `08_theme_and_theme_mode.md` | `AppTheme.kt` (dark + Arabic), `ThemeCatalogPreview.kt`, `features/settings` theme mode (DataStore), `MainActivity` |
| 09 | `09_icons_and_logo.md` | `res/drawable/ic_lucide_*.xml` (76), `AppIcons.kt`, logo, adaptive launcher icon |
| 10 | `10_buttons.md` | `AppButton` (7 variants, 3 sizes, pressed/disabled/loading), `AppSocialButton`, `AppIconButton` |
| 11 | `11_text_fields.md` | Pill text fields, password, strength, search, OTP |
| 12 | `12_selection_and_display.md` | Selection controls, chips, badges, prices, toasts, **empty states, skeleton shimmer** |
| 13 | `13_screen_background_and_scaffold.md` | `AppScreenBackground` (aurora), scaffold, top bar, auth header |
| 14 | `14_auth_screens_refactor.md` | Auth screens refactor (Welcome / Log in / Sign up / **Verify email** / Forgot / All set) |
| 15 | `15_checklist_and_known_gaps.md` | nothing (checklist, QA, gaps) |

Docs 10–14 use exactly the names defined in docs 02–09.

**Figma version.** These docs follow the updated Figma file: one radius scale, official dark mode (`01b`), Cairo Arabic type scale, atom states (`03b`), Android app icon (`05b`), email verification screens (02.6/02.7), dark & Arabic screens (13.x) and empty & loading states (14.x).

---

## 3. Final folder tree

```
app/src/main/java/com/example/dukkanapp/
└── core/
    ├── designsystem/
    │   ├── theme/                       ← package com.example.dukkanapp.core.designsystem.theme
    │   │   ├── AppPalette.kt            02  internal primitives
    │   │   ├── AppColors.kt             03  semantic colors + LocalAppColors
    │   │   ├── AppColorScheme.kt        04  Material ColorScheme built from AppColors
    │   │   ├── AppGradients.kt          05  brushes + LocalAppGradients
    │   │   ├── AppShadows.kt            05  shadows + Modifier.appShadow + LocalAppShadows
    │   │   ├── AppTypography.kt         06  AppFontFamily + Material Typography
    │   │   ├── AppTextStyles.kt         06  caption, overline, price… + LocalAppTextStyles
    │   │   ├── AppDimens.kt             07  spacing, radius, sizes
    │   │   ├── AppShapes.kt             07  Material Shapes + AppPillShape
    │   │   ├── AppTheme.kt              08  composition root + accessor object
    │   │   └── ThemeCatalogPreview.kt   08  previews of every token
    │   ├── icon/
    │   │   └── AppIcons.kt              09  Lucide icon ids
    │   └── components/                  ← reusable UI built only from tokens
    │       ├── logo/                    09  AppLogo, AppLogoMark
    │       ├── button/                  10  AppButton, AppSocialButton, AppIconButton
    │       ├── textfield/               11  AppTextField, AppPasswordField, AppSearchBar, AppOtpField…
    │       ├── selection/               12  checkbox, radio, switch, chip
    │       ├── display/                 12  badge, price, stepper, avatar, rating, pager…
    │       ├── feedback/                12  toast, AppMessageBanner, AppEmptyState, AppSkeleton
    │       └── layout/                  13  AppScreenBackground, AppScaffold
    ├── common/components/               old components: delete each one once doc 14 replaces it
    └── …
features/auth/presentation/components/common/   13  AppAuthHeader, AppAuthFooterLink, AppOrDivider
features/settings/                       08  theme mode (ThemeMode, ThemeRepository, use cases, DataStore)
app/src/main/res/
    ├── font/plus_jakarta_sans_{regular,medium,semibold,bold,extrabold}.ttf
    ├── font/cairo_{regular,medium,semibold,bold}.ttf
    ├── drawable/ic_lucide_*.xml, ic_dukkan_mark.xml, ic_launcher_{foreground,background,monochrome}.xml
    └── mipmap-anydpi-v26/ic_launcher.xml (adaptive + monochrome)
```

Files that **disappear** once you are done:

| Old file | Replaced by |
|---|---|
| `core/config/theme/AppTheme.kt` | `core/designsystem/theme/AppTheme.kt` |
| `core/config/theme/AppTypography.kt` | `core/designsystem/theme/AppTypography.kt` |
| `core/config/theme/ThemePreview.kt` | `core/designsystem/theme/ThemeCatalogPreview.kt` |
| `core/utils/constants/AppColors.kt` (palette + schemes) | `AppPalette.kt` + `AppColorScheme.kt` |
| `core/utils/constants/AppExtendedColors.kt` | `AppColors.kt` |
| `core/utils/constants/AppDimens.kt` | `core/designsystem/theme/AppDimens.kt` |
| `core/utils/constants/AppShapes.kt` | `core/designsystem/theme/AppShapes.kt` |
| `res/font/konnect_*.otf` (18 files) | `res/font/plus_jakarta_sans_*.ttf` (5) + `cairo_*.ttf` (4) |

Tip: move files with Android Studio **Refactor → Move** (F6). It rewrites imports in every screen for you. Then rename/rewrite the contents following each doc.

---

## 4. The rules

1. **Only `AppPalette` holds hex.** `AppPalette` is `internal object`, and every other file reads it only inside `core/designsystem/theme`.
2. **Features never write `Color(0x…)`, `12.dp` or `14.sp`.** They use `AppTheme.colors`, `AppDimens`, `MaterialTheme.typography` / `AppTheme.textStyles`.
3. **Widgets never branch on `isSystemInDarkTheme()`.** Light and dark are two instances of the same `AppColors` class. The theme picks one.
4. **One name per meaning.** If Figma calls it `brand/soft`, Kotlin calls it `brandSoft`. No synonyms.
5. **No dynamic color.** Dukkan is a brand; wallpaper colors would replace the Dukkan blue. Doc 08 removes `dynamicColor`.
6. **Composition locals fail loud.** A missing `AppTheme` crashes with a clear message instead of silently showing default purple.

---

## 5. What changes vs the current code

| Area | Today | v4 (Figma) | Why |
|---|---|---|---|
| Brand blue | `primary = Blue700 #1D4ED8` | `brand = Blue600 #2563EB`, pressed `Blue700` | Figma `brand/primary` is #2563EB |
| Background | `Slate50 #F8FAFC` | `#F7F9FB` | Figma `surface/background` |
| Field fill | `surfaceVariant = Slate100` | `field = #F1F5F9` (same value, real name) | `surface/field` |
| Secondary | Orange700 commerce accent | `deal = #F97316` + deal gradient | Figma `accent/deal` |
| Tertiary | Teal | `gold = #F59E0B` (ratings, points) | No teal in Figma |
| Extended colors | `success/warning/info/sale/favorite` | `AppColors` with 31 fields named after Figma (incl. `sheet`) | One semantic layer, not two |
| Gradients | none | 8 brushes (brand, button, screenTint, bottomFade, deal, gold, navy, imageScrim) + 3 aurora blobs + a card highlight | Primary buttons and headers are gradients |
| Shadows | Material elevation | 5 colored shadows (card, float, button, deal, screen); dark cards add a 1 dp outline | Figma uses soft, tinted shadows |
| Font | Konnect, 18 files | Plus Jakarta Sans (5 weights) + Cairo for Arabic (4 weights) | Figma type scale (Latin + Arabic) |
| Type scale | M3 defaults (57/45/36…) | Figma scale (40/32/28/24/22/20/18/16…) | Mobile-sized headings |
| Extra styles | none | caption, overline, prices, field text, helper | Figma has styles Material lacks |
| Spacing | `spaceXs…space5Xl` (t-shirt) | `space4…space64` (by value) | Figma names `space/16` → `space16` |
| Radius | 2…32 + 999 | one scale: 10 / 14 / 16 / 20 / 28 / full | Figma "Radius · one scale" |
| Inputs | rounded 24 dp, 45 dp tall | **pill**, 52 dp tall, focus ring, token-based disabled | Figma atoms + `03b` |
| Control heights | `buttonHeight = 45` | 52 / 44 / 36 | Figma sizes |
| Screen padding | 16 | 20 | Figma |
| Shapes | xs 6, sm 8, md 12, lg 16, xl 24 | xs 10, sm 14, md 16, lg 20, xl 28 + pill | Figma Compose-tokens chapter |
| Icons | Iconsax + Material icons | 76 Lucide SVGs → Vector Asset | Figma chapter 02 |
| Dark mode | hand-made dark scheme | **official Figma dark tokens** (`night` palette) | Figma `01b` |
| Launcher icon | default template | adaptive icon with gradient background + monochrome | Figma `05b` |
| Theme mode | always follows system | Light / Dark / System saved in DataStore | Figma screen 11.3 |

---

## 6. Architecture fit (MVI + clean architecture)

The design system is **pure presentation**. It has no ViewModel, no repository, no Hilt module.
The only exception is the theme mode setting (doc 08), which follows the same layers as the language feature:

```
DataStore (THEME_MODE key) → ThemePreferenceLocalDataSource → ThemeRepository(Impl)
  → ObserveThemeModeUseCase / SetThemeModeUseCase → AppStartViewModel / SettingsViewModel → AppTheme(darkTheme)
```

## 7. Patterns used

| Pattern / principle | Where |
|---|---|
| Two-tier tokens (primitive → semantic) | `AppPalette` → `AppColors` |
| Single source of truth | `AppColors` feeds both `ColorScheme` and our components |
| Dependency inversion | Components read `LocalAppColors`, not `AppPalette` |
| Open/closed | New brand role = new field in `AppColors`, no widget change |
| Value objects | `AppShadow`, `AppColors`, `AppTextStyles` are `@Immutable` data classes |
| Composition root | `AppTheme { }` wires everything once |
| Fail fast | `staticCompositionLocalOf { error(...) }` |

## Check

- [ ] I read docs in order and build after each one (`./gradlew :app:assembleDebug`).
- [ ] I know the difference between `AppPalette` (raw) and `AppColors` (meaning).
- [ ] I know docs 10–14 depend on 02–09.
