# 01 — Figma token reference (read only)

**File:** none. This doc is the lookup table you keep open while typing docs 02–14.

Source: Figma file **Dukkan App**, page **Design System**:
`01 · Foundations`, `01b · Dark mode`, `02 · Icons`, `03 · Atoms`, `03b · Atom states`, `04 · Molecules`, `05 · Logo`, `05b · Android app icon`, `06 · Compose tokens`.
The file has **no Figma variables or styles**. Every value below was read from the drawn swatches and specimens.
**Light and dark are both official Figma values now** (`01` and `01b`). Values marked *derived* are ours, for slots Figma doesn't define.

---

## 1. Primitive palette (`AppPalette`, doc 02)

Every color used anywhere in the file is now a named primitive.

| Kotlin      | Figma      | Hex       | Used for                                                    |
| ----------- | ---------- | --------- | ----------------------------------------------------------- |
| `Blue50`    | blue-50    | `#EFF5FF` | brand/soft (light)                                          |
| `Blue100`   | blue-100   | `#DBE7FE` | brand/ring (light)                                          |
| `Blue200`   | blue-200   | `#BFD4FE` |                                                             |
| `Blue300`   | blue-300   | `#93B4FD` |                                                             |
| `Blue400`   | blue-400   | `#6090FA` | brand/primary (dark)                                        |
| `Blue500`   | blue-500   | `#3B6FF6` | brand/pressed (dark)                                        |
| `Blue600`   | blue-600   | `#2563EB` | brand/primary (light), button glow                          |
| `Blue700`   | blue-700   | `#1D4ED8` | brand/pressed (light), gradient ends                        |
| `Blue800`   | blue-800   | `#1E40AF` |                                                             |
| `Blue900`   | blue-900   | `#1E3A8A` | navy gradient start, brand/ring (dark)                      |
| `Blue950`   | blue-950   | `#172554` | navy end, brand/soft (dark), dark screen tint               |
| `Azure100`  | azure-100  | `#DCE7FF` | screen tint start                                           |
| `Azure200`  | azure-200  | `#A9C4FF` | aurora sky blob                                             |
| `Azure300`  | azure-300  | `#7EA6FF` | aurora blue blob                                            |
| `Azure400`  | azure-400  | `#4F8BFF` | brand gradient start, dark button glow, launcher background |
| `Azure500`  | azure-500  | `#3B82F6` | button gradient start                                       |
| `Slate50`   | slate-50   | `#F7F9FB` | surface/background                                          |
| `Slate100`  | slate-100  | `#F1F5F9` | surface/field, dark text/primary                            |
| `Slate200`  | slate-200  | `#E2E8F0` | border/default                                              |
| `Slate300`  | slate-300  | `#CBD5E1` | radio off, dark text/label                                  |
| `Slate400`  | slate-400  | `#94A3B8` | text/tertiary, dark text/secondary                          |
| `Slate500`  | slate-500  | `#64748B` | text/secondary, dark text/tertiary                          |
| `Slate700`  | slate-700  | `#334155` | text/label                                                  |
| `Slate800`  | slate-800  | `#1E293B` |                                                             |
| `Slate900`  | slate-900  | `#0F172A` | text/primary, shadows, image scrim                          |
| `Night700`  | night-700  | `#24314F` | dark border, dark card outline                              |
| `Night800`  | night-800  | `#1A2540` | dark surface/field                                          |
| `Night850`  | night-850  | `#16213A` | dark surface/sheet                                          |
| `Night900`  | night-900  | `#111A2E` | dark surface/card                                           |
| `Night950`  | night-950  | `#0B1220` | dark surface/background                                     |
| `Green100`  | green-100  | `#DCFCE7` | success-soft                                                |
| `Green500`  | green-500  | `#22C55E` | dark success                                                |
| `Green600`  | green-600  | `#16A34A` | status/success                                              |
| `Green700`  | green-700  | `#15803D` | success text on soft                                        |
| `Green950`  | green-950  | `#0F2E1D` | dark success-soft                                           |
| `Amber100`  | amber-100  | `#FEF3C7` | warning-soft                                                |
| `Amber200`  | amber-200  | `#FDE68A` | aurora sun blob                                             |
| `Amber300`  | amber-300  | `#FCD34D` | gold gradient start, dark gold, dark warning-text           |
| `Amber500`  | amber-500  | `#F59E0B` | accent/gold, dark warning                                   |
| `Amber600`  | amber-600  | `#D97706` | status/warning                                              |
| `Amber700`  | amber-700  | `#B45309` | status/warning-text                                         |
| `Amber950`  | amber-950  | `#33260A` | dark warning-soft                                           |
| `Red50`     | red-50     | `#FEF2F2` | error-soft                                                  |
| `Red100`    | red-100    | `#FEE2E2` |                                                             |
| `Red200`    | red-200    | `#FECACA` | error-border                                                |
| `Red400`    | red-400    | `#F87171` | dark error                                                  |
| `Red600`    | red-600    | `#DC2626` | status/error                                                |
| `Red950`    | red-950    | `#3A1418` | dark error-soft / error-border                              |
| `Orange100` | orange-100 | `#FFEDD5` | deal tint                                                   |
| `Orange400` | orange-400 | `#FB923C` | deal gradient start, dark deal                              |
| `Orange500` | orange-500 | `#F97316` | accent/deal                                                 |
| `Orange950` | orange-950 | `#3A1E0B` | dark deal tint                                              |
| `Rose500`   | rose-500   | `#F43F5E` | deal gradient end                                           |
| `White`     | white      | `#FFFFFF` | on/primary                                                  |
| `Black`     | —          | `#000000` | dark shadows                                                |

## 2. Semantic colors (`AppColors`, doc 03)

| Kotlin          | Figma                              | Light               | Dark                             |
| --------------- | ---------------------------------- | ------------------- | -------------------------------- |
| `background`    | surface/background                 | Slate50 `#F7F9FB`   | Night950 `#0B1220`               |
| `card`          | surface/card                       | White               | Night900 `#111A2E`               |
| `field`         | surface/field                      | Slate100 `#F1F5F9`  | Night800 `#1A2540`               |
| `sheet`         | surface/sheet                      | White               | Night850 `#16213A`               |
| `textPrimary`   | text/primary                       | Slate900 `#0F172A`  | Slate100 `#F1F5F9`               |
| `textLabel`     | text/label                         | Slate700 `#334155`  | Slate300 `#CBD5E1`               |
| `textSecondary` | text/secondary                     | Slate500 `#64748B`  | Slate400 `#94A3B8`               |
| `textTertiary`  | text/tertiary                      | Slate400 `#94A3B8`  | Slate500 `#64748B`               |
| `border`        | border/default                     | Slate200 `#E2E8F0`  | Night700 `#24314F`               |
| `borderStrong`  | *derived* (radio off)              | Slate300            | Slate700                         |
| `brand`         | brand/primary                      | Blue600 `#2563EB`   | Blue400 `#6090FA`                |
| `brandPressed`  | brand/pressed                      | Blue700 `#1D4ED8`   | Blue500 `#3B6FF6`                |
| `brandSoft`     | brand/soft                         | Blue50 `#EFF5FF`    | Blue950 `#172554`                |
| `brandSoftText` | brand/soft-text                    | Blue700 `#1D4ED8`   | Blue300 `#93B4FD`                |
| `brandRing`     | brand/ring                         | Blue100 `#DBE7FE`   | Blue900 `#1E3A8A`                |
| `onBrand`       | on/primary                         | White               | White                            |
| `success`       | status/success                     | Green600 `#16A34A`  | Green500 `#22C55E`               |
| `successText`   | status/success-text                | Green700 `#15803D`  | Green500 `#22C55E`               |
| `successSoft`   | status/success-soft                | Green100 `#DCFCE7`  | Green950 `#0F2E1D`               |
| `warning`       | status/warning                     | Amber600 `#D97706`  | Amber500 `#F59E0B`               |
| `warningText`   | status/warning-text                | Amber700 `#B45309`  | Amber300 `#FCD34D`               |
| `warningSoft`   | status/warning-soft                | Amber100 `#FEF3C7`  | Amber950 `#33260A`               |
| `error`         | status/error                       | Red600 `#DC2626`    | Red400 `#F87171`                 |
| `errorSoft`     | status/error-soft                  | Red50 `#FEF2F2`     | Red950 `#3A1418`                 |
| `errorBorder`   | status/error-border                | Red200 `#FECACA`    | Red400 · 50 % (`#F87171` · 50 %) |
| `deal`          | accent/deal                        | Orange500 `#F97316` | Orange400 `#FB923C`              |
| `dealSoft`      | *derived* (deal tint)              | Orange100           | Orange950                        |
| `gold`          | accent/gold                        | Amber500 `#F59E0B`  | Amber300 `#FCD34D`               |
| `inverse`       | *derived* (dark button, New badge) | Slate900            | Slate100                         |
| `onInverse`     | *derived*                          | White               | Slate900                         |
| `scrim`         | *derived* (sheet scrim)            | Slate900            | Black                            |
| `shadow`        | *derived* (shadow base)            | Slate900            | Black                            |

## 3. Typography (doc 06)

Format in Figma: `Weight · size/lineHeight`. Latin = Plus Jakarta Sans, Arabic = Cairo (same names, switched by locale).

| Kotlin                        | Figma                | Latin                   | Arabic (Cairo)                      | Sample                                   |
| ----------------------------- | -------------------- | ----------------------- | ----------------------------------- | ---------------------------------------- |
| `displayLarge`                | Display/Large        | ExtraBold 40/48         | Bold 40/56 *derived*                | Shop smart, delivered fast               |
| `displayMedium`               | Display/Medium       | ExtraBold 32/40         | Bold 32/46 *derived*                |                                          |
| `displaySmall`                | Display/Small        | ExtraBold 28/36         | Bold 28/40 *derived*                | My addresses                             |
| `headlineLarge`               | Headline/Large       | ExtraBold 24/32         | **Bold 24/36**                      | Welcome back · ماذا تريد أن تتسوق اليوم؟ |
| `headlineMedium`              | Headline/Medium      | ExtraBold 22/28         | Bold 22/32 *derived*                | Order placed!                            |
| `headlineSmall`               | Headline/Small       | ExtraBold 20/26         | Bold 20/30 *derived*                | Choose your language                     |
| `titleLarge`                  | Title/Large          | Bold 18/24              | SemiBold 18/28 *derived*            | Flash deals                              |
| `titleMedium`                 | Title/Medium         | Bold 16/22              | **SemiBold 16/26**                  | Order #DK-2048 · تفاصيل الطلب            |
| `titleSmall`                  | Title/Small          | Bold 14/20              | SemiBold 14/22 *derived*            | Air Runner 2                             |
| `bodyLarge`                   | Body/Large           | Medium 15/22            | Regular 15/24 *derived*             | Free delivery on orders over 150 TND.    |
| `bodyMedium`                  | Body/Medium          | Medium 14/20            | **Regular 14/22**                   | توصيل مجاني للطلبات فوق 150 د.ت          |
| `bodySmall`                   | Body/Small           | Medium 13/18            | **Regular 13/20**                   |                                          |
| `labelLarge`                  | Label/Large          | Bold 15/20              | **SemiBold 15/22**                  | Add to cart · أضف إلى السلة              |
| `labelMedium`                 | Label/Medium         | Bold 12.5/16            | SemiBold 12.5/18 *derived*          | Email (input label)                      |
| `labelSmall`                  | Label/Small          | Bold 12/16              | SemiBold 12/18 *derived*            | See all                                  |
| `textStyles.fieldText`        | Field/Text           | SemiBold 14.5/20        | Medium 14.5/22 *derived*            | amira.bensalah@gmail.com                 |
| `textStyles.fieldPlaceholder` | (Field/Text, Medium) | Medium 14.5/20          | Regular 14.5/22 *derived*           | name@example.com                         |
| `textStyles.helper`           | Helper               | SemiBold 11.5/14        | Medium 11.5/16 *derived*            | Verified with Google                     |
| `textStyles.caption`          | Caption              | SemiBold 11/14          | **Medium 11/16**                    | Delivered in 25–35 min                   |
| `textStyles.overline`         | Overline             | ExtraBold 10.5/12, +0.9 | Bold 10.5/16, no tracking *derived* | NEW ARRIVAL                              |
| `textStyles.priceLarge`       | Price/Large          | ExtraBold 24/28         | Bold 24/32 *derived*                | 279.000 TND                              |
| `textStyles.priceSmall`       | Price/Small          | ExtraBold 15/20         | Bold 15/22 *derived*                | 39.900 TND                               |

All auth titles in the screens are **24 ExtraBold** (`headlineLarge`); the Arabic log in (13.4) uses Cairo Bold 24.

## 4. Gradients (doc 05)

| Kotlin       | Figma       | Stops                  | Direction  | Dark                  | Used by                                                       |
| ------------ | ----------- | ---------------------- | ---------- | --------------------- | ------------------------------------------------------------- |
| `brand`      | Brand       | azure-400 → blue-700   | 135°       | same                  | logo mark, brand icon button, icon tiles, launcher background |
| `button`     | Button      | azure-500 → blue-700   | horizontal | same                  | primary / checkout / slide buttons                            |
| `screenTint` | Screen tint | azure-100 → slate-50   | vertical   | blue-950 → night-950  | top of auth, home header                                      |
| `bottomFade` | Bottom fade | background 0 % → 100 % | vertical   | night-950 0 % → 100 % | end of the aurora band                                        |
| `deal`       | Deal        | orange-400 → rose-500  | 135°       | same                  | deal button, −15 % badge, flash banner                        |
| `gold`       | Gold        | amber-300 → amber-500  | 135°       | same                  | points, Gold member                                           |
| `navy`       | Navy        | blue-900 → blue-950    | 135°       | same                  | collection banner                                             |
| `imageScrim` | Image scrim | slate-900 0 % → 72 %   | vertical   | same                  | text over photos                                              |

Aurora blobs (header). Exactly **3 blobs**, same positions in light and dark (Figma `13.1b · Header glow · spec`). Centers and radii are in screen px of the 393 × 852 frame:

| Blob         | Light            | Dark                | Center · radius                                        |
| ------------ | ---------------- | ------------------- | ------------------------------------------------------ |
| blue         | azure-300 · 62 % | **blue-500 · 35 %** | c (60, 20) · r 230                                     |
| sky          | azure-200 · 55 % | **blue-800 · 45 %** | c (360, 60) · r 190                                    |
| warm (`sun`) | amber-200 · 42 % | **amber-300 · 8 %** | c (300, 250) · r 150 on Log in (Welcome: c (300, 380)) |

Dark band: `screenTint` blue-950 → night-950, then fade to night-950.

**Card highlight** (not an aurora blob): white · 35 %, centered top-right, r 90, drawn **only inside gradient cards** (promo banners, wallet card, earnings card). Same in light and dark. Code: `AppGradients.cardHighlight` + `Modifier.cardHighlight()` (doc 05).

Band heights: Welcome 520, Log in 300, Verify email 260; last 90 dp = bottom fade.

## 5. Elevation (doc 05)

| Kotlin   | Figma         | Light                         | Dark                                                     |
| -------- | ------------- | ----------------------------- | -------------------------------------------------------- |
| `card`   | Card          | slate-900 6 %, y 4, blur 14   | **black 45 %**, y 4, blur 14 **+ 1 dp night-700 border** |
| `float`  | Float / sheet | slate-900 12 %, y 10, blur 28 | **black 55 %**, y 10, blur 28 **+ border**               |
| `button` | Button        | blue-600 30 %, y 8, blur 20   | **azure-400 25 %**, y 8, blur 20                         |
| `deal`   | Deal          | orange-500 30 %, y 8, blur 20 | **orange-400 25 %**, y 8, blur 20                        |
| `screen` | Screen        | slate-900 10 %, y 18, blur 44 | **black 60 %**, y 18, blur 44 **+ border**               |

All dark **surfaces** (card, float/sheet, screen) add a 1 dp night-700 border. Button and Deal keep their coloured glow and get no border. Every dark value is official (Figma `01b`).

## 6. Spacing, radius, sizes (doc 07)

| Kotlin                                  | Figma                                                 | Value                                      |
| --------------------------------------- | ----------------------------------------------------- | ------------------------------------------ |
| `space4` … `space48`                    | space/4 … space/48                                    | 4, 8, 12, 16, 20, 24, 32, 48               |
| `space2`, `space64`                     | — (kept from code)                                    | 2, 64                                      |
| `radiusSm`                              | radius/sm                                             | 10 (checkbox, swatch, small thumb)         |
| `radiusMd`                              | radius/md                                             | 14 (icon tile, info row, coupon)           |
| `radiusLg`                              | radius/lg                                             | 16 (photo, tile, OTP box)                  |
| `radiusCard`                            | radius/card                                           | 20 (card, product card)                    |
| `radiusSheet`                           | radius/sheet                                          | 28 (bottom sheet, dialog)                  |
| `radiusFull` / `AppPillShape`           | radius/full                                           | 999 (button, **input**, option, chip, nav) |
| `controlLg` / `controlMd` / `controlSm` | Button L·input / Button M·icon button / Button S | 52 / 44 / 36 |
| `buttonLg` / `buttonCompact` / `buttonMd` / `buttonSm` / `buttonXs` / `buttonSlide` | size/button-* | 52 / 48 / 44 / 36 / 26 / 58 |
| `input` / `inputSearch` | size/input · input-search | 52 / 44 |
| `iconButton` / `iconButtonSm` | size/icon-button · icon-button-sm | 44 / 36 |
| `chip` / `badge` | size/chip · badge | **34** / 20 |
| `optionLg` / `optionMd` / `optionSm` | size/option-* | 66 / 52 / 48 |
| `stepper` / `addButton` / `wishlist` / `checkbox` | size/stepper · add-button · wishlist · checkbox | 32 / 32 / 28 / 20 |
| `switchHeight` × `switchWidth` | size/switch-* | 28 × 46 |
| `toast` | size/toast | 58 |
| `screenWidth` × `screenHeight` | size/screen-* (design frame) | 393 × 852 |
| `radiusScreen` | radius/screen (Figma only) | 44 |
| `iconSm` / `iconMd` / `icon` / `iconLg` | icon/sm · md · lg · xl | 16 / **18** / 20 / 24 |
| `navBar` / `teamNavBar` | size/nav-bar · team-nav-bar | **66** (customer) / 64 (staff) |
| `screenPadding`                         | screen padding                                        | 20                                         |

## 7. Component specs summary (docs 10–12)

### Buttons (pill, `AppPillShape`)

| Variant                    | Size      | Fill              | Border            | Label                   | Shadow   |
| -------------------------- | --------- | ----------------- | ----------------- | ----------------------- | -------- |
| primary L                  | 52 h      | gradient `button` | —                 | 15 Bold White           | `button` |
| primary M                  | 44 h      | gradient `button` | —                 | 14 Bold White           | `button` |
| primary S                  | 36 h      | gradient `button` | —                 | 12.5 Bold White         | `button` |
| deal                       | 52 h      | gradient `deal`   | —                 | 15 Bold White           | `deal`   |
| dark                       | 52 h      | `inverse`         | —                 | 15 Bold `onInverse`     | —        |
| tonal                      | 52 h      | `brandSoft`       | —                 | 15 Bold `brandSoftText` | —        |
| outline                    | 52 h      | `card`            | 1.2 `border`      | 15 Bold `textPrimary`   | —        |
| ghost                      | text only | —                 | —                 | 15 Bold `brand`         | —        |
| danger                     | 44 h      | `errorSoft`       | 1.2 `errorBorder` | 13.5 Bold `error`       | —        |
| social (Google / Facebook) | 52 h      | `card`            | 1.2 `border`      | 14.5 Bold `textPrimary` | —        |
| slide to place order       | 58 h      | gradient `button` | —                 | 15 Bold White           | `button` |

Button states (`03b`, Primary / Tonal / Outline):

| State    | Rule                                            |
| -------- | ----------------------------------------------- |
| Pressed  | `brandPressed` fill, no lift (no shadow)        |
| Disabled | `field` fill + `textTertiary` label, no shadow  |
| Loading  | spinner replaces the trailing icon, label stays |

### Icon buttons (44 × 44, circle)

white (`card` + shadow card), glass (white over photos), soft (`brandSoft`), field (`field`), brand (gradient `brand`), dark (`inverse`).

### Input (52 h, **pill**, label 12.5 Bold above, gap 8)

| State    | Fill        | Border        | Extra                                                                   |
| -------- | ----------- | ------------- | ----------------------------------------------------------------------- |
| Default  | `field`     | none          | placeholder `textTertiary` Medium 14.5, icon `textTertiary`             |
| Focus    | `card`      | 1.5 `brand`   | 3 dp pill ring `brandRing` (58 h outside), icon `brand`, cursor `brand` |
| Filled   | `field`     | none          | value `textPrimary` SemiBold 14.5                                       |
| Error    | `errorSoft` | 1.5 `error`   | icon + helper `error` (SemiBold 11.5)                                   |
| Success  | `card`      | 1.5 `success` | helper `success`                                                        |
| Disabled | `field`     | none, no ring | label, value and icon `textTertiary` (tokens, **not** opacity)          |

Password field: eye ↔ eye-off when the password is visible.

### Search bar

pill. elevated: 52 h, `card`, shadow `float`; field: 48 h, `field`. Placeholder `textTertiary` 14 / 13.5 Medium.

### Chips, badges, controls

| Item            | Spec                                                                                                                                                                                                                                                     |
| --------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Chip            | 34 h pill, `card`, 1.2 `border`, label 12.5 Bold `textLabel`; selected = `brandSoft` + `brand` border                                                                                                                                                    |
| Badge           | 20 h pill, 10.5 Bold: −15 % (deal gradient, White), Hot deal (`deal`, White), New (`inverse`, White), 25 min / Bestseller (`brandSoft`, `brandSoftText`), Only 2 left / 4.8 (`warningSoft`, `warningText`), Free delivery (`successSoft`, `successText`) |
| Rating star     | `star-filled` in `gold`                                                                                                                                                                                                                                  |
| Checkbox        | 20 × 20, `radiusSm`, on = `brand`, off = `card` + 1.6 `border`                                                                                                                                                                                           |
| Radio           | 22 on (`brand`), 20 off (`card` + 1.6 `borderStrong`)                                                                                                                                                                                                    |
| Switch          | 46 × 28, on `brand`, off `border`                                                                                                                                                                                                                        |
| OTP box         | 52 × 58, `radiusLg` 16, `card`, 1.5 `border`, digit 22 ExtraBold                                                                                                                                                                                         |
| Pager dot       | 22 × 6 active (`brand`)                                                                                                                                                                                                                                  |
| Qty stepper     | 88 × 32, `brand`, 13.5 ExtraBold White                                                                                                                                                                                                                   |
| Add button      | 32 × 32 circle (white or `brand`) + shadow `card`                                                                                                                                                                                                        |
| Count bubble    | 30 × 30 `brand`, 2.5 White border                                                                                                                                                                                                                        |
| Avatar initials | 36 × 36 `brandRing` fill, 14.4 Bold `brandSoftText`                                                                                                                                                                                                      |
| Bottom nav      | 64 h floating white bar, active tab = blue pill + bold blue label, cart total with 3 decimals ("39.900 TND")                                                                                                                                             |

### Empty & loading (customer board 14)

| Item        | Spec                                                                                         |
| ----------- | -------------------------------------------------------------------------------------------- |
| Empty state | icon tile + title + body + optional button (14.1 orders, 14.2 notifications, 14.3 addresses) |
| Skeleton    | `field` blocks with a left → right shimmer, 1.2 s loop (14.4 Home, 14.5 product grid)        |

### Android app icon (`05b`)

108 × 108 dp layers, 72 dp visible, 66 dp safe zone. Foreground white mark ≈ 40 dp, background gradient azure-400 → blue-700, monochrome layer. Legacy 48 / 72 / 96 / 144 / 192 px, Play Store 512 px.

## Check

- [ ] I know where each value comes from before I type it.
- [ ] I will not invent a hex that is not in section 1.

A small version of the flow, using real Dukkan token names.

1. Palette (raw cans, `AppPalette.kt`)

--------------------------------------

    internal object AppPalette {
        val Blue600 = Color(0xFF2563EB)
        val Blue400 = Color(0xFF60A5FA)
        val Slate50 = Color(0xFFF7F9FB)
        val Night950 = Color(0xFF0B1220)
    }

2. Semantic colors (labels, `AppColors.kt`)

-------------------------------------------

    @Immutable
    data class AppColors(
        val brand: Color,
        val background: Color,
    )
    
    val LightAppColors = AppColors(
        brand = AppPalette.Blue600,
        background = AppPalette.Slate50,
    )
    
    val DarkAppColors = AppColors(
        brand = AppPalette.Blue400,
        background = AppPalette.Night950,
    )
    
    val LocalAppColors = staticCompositionLocalOf<AppColors> {
        error("AppColors not provided. Wrap your screen in AppTheme { }")
    }

3. Theme (picks light or dark, `AppTheme.kt`)

---------------------------------------------

    object AppTheme {
        val colors: AppColors
            @Composable get() = LocalAppColors.current
    }
    
    @Composable
    fun AppTheme(
        darkTheme: Boolean = isSystemInDarkTheme(),
        content: @Composable () -> Unit,
    ) {
        val colors = if (darkTheme) DarkAppColors else LightAppColors
    
        CompositionLocalProvider(LocalAppColors provides colors) {
            MaterialTheme(content = content)
        }
    }

This is the only place that decides light or dark.

4. Screen (only uses labels)

----------------------------

    @Composable
    fun WelcomeScreen() {
        Box(Modifier.fillMaxSize().background(AppTheme.colors.background)) {
            Button(
                onClick = { },
                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.brand),
            ) {
                Text("Get started")
            }
        }
    }

The screen never mentions `Blue600` or `Night950`, and it has no if/else for dark mode.
What happens at runtime
-----------------------

    Light mode: AppTheme(darkTheme = false)
      → LightAppColors → brand = Blue600 (#2563EB), background = #F7F9FB
    
    Dark mode: AppTheme(darkTheme = true)
      → DarkAppColors → brand = Blue400 (#60A5FA), background = #0B1220

The screen code is identical in both modes. Only the theme in step 3 changes which colors it receives.

The real files have about 31 fields in `AppColors` (`card`, `textPrimary`, `error`, `brandSoft` and so on), plus gradients, shadows and text styles. The pattern is the same as above.
