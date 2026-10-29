# 09 — Icons (Lucide), logo, launcher icon and splash

**Files:**

| File | Action |
|---|---|
| `res/drawable/ic_lucide_*.xml` (76 files) | new (Vector Asset import) |
| `res/drawable/ic_facebook.xml` | new (Figma shows Google + Facebook, not Apple) |
| `res/drawable/ic_dukkan_mark.xml` | new (replaces `ic_app_logo.xml`) |
| `core/designsystem/icon/AppIcons.kt` | new |
| `core/designsystem/components/logo/AppLogo.kt` | new |
| `res/drawable/ic_launcher_foreground.xml`, `ic_launcher_background.xml`, `ic_launcher_monochrome.xml` | rewrite (Figma `05b · Android app icon`) |
| `res/mipmap-anydpi-v26/ic_launcher.xml`, `ic_launcher_round.xml` | rewrite |
| `res/mipmap-*/ic_launcher.png` (legacy) | regenerate |
| `res/values/colors.xml`, `res/values/themes.xml` | edit: splash colors, remove purple/teal |
| `app/build.gradle.kts`, `gradle/libs.versions.toml` | remove `iconsax-compose` (last step) |

## Why

Figma chapter `02 · Icons`: **"76 icons from Lucide, used at 20–24 px with 2 px stroke."** The latest version adds six: **user-plus, eye-off, star-filled** ("ratings use the filled star in accent/gold"), **wifi-off, search-x, mic**.

Today the app mixes two icon sets: Iconsax (`Iconsax.Linear.Sms`) and Material (`Icons.AutoMirrored.Filled.ArrowBack`). They have different stroke widths and corner styles, so screens look slightly off next to Figma.
Importing the exact Lucide SVGs as vector drawables means:

- the pixels match Figma 1:1,
- no icon library dependency (Iconsax pulls hundreds of icons into the APK; we use 76),
- every icon is a plain resource that `Icon(painterResource(...))` can tint.

`AppIcons` gives each drawable a readable Kotlin name, so screens never type `R.drawable.ic_lucide_…` themselves. That's one place to swap an icon.

## Figma spec

| Property | Value |
|---|---|
| Set | Lucide (lucide.dev), ISC license |
| Grid | 24 × 24 |
| Stroke | 2 px, round caps, round joins |
| Display size | `AppDimens.icon` (20) in fields, buttons, rows; `AppDimens.iconLg` (24) in app bar and bottom nav; `AppDimens.iconSm` (16) in badges |
| Color | always tinted by the caller: `colors.textTertiary` idle, `colors.brand` focused/active, `colors.error` error |

### The 76 icons

| Lucide name | Drawable | `AppIcons` |
|---|---|---|
| house | `ic_lucide_house` | `House` |
| layout-grid | `ic_lucide_layout_grid` | `LayoutGrid` |
| shopping-bag | `ic_lucide_shopping_bag` | `ShoppingBag` |
| percent | `ic_lucide_percent` | `Percent` |
| user-round | `ic_lucide_user_round` | `UserRound` |
| user-plus (**new**) | `ic_lucide_user_plus` | `UserPlus` |
| search | `ic_lucide_search` | `Search` |
| scan-line | `ic_lucide_scan_line` | `ScanLine` |
| sliders-horizontal | `ic_lucide_sliders_horizontal` | `SlidersHorizontal` |
| bell | `ic_lucide_bell` | `Bell` |
| heart | `ic_lucide_heart` | `Heart` |
| map-pin | `ic_lucide_map_pin` | `MapPin` |
| locate-fixed | `ic_lucide_locate_fixed` | `LocateFixed` |
| truck | `ic_lucide_truck` | `Truck` |
| package | `ic_lucide_package` | `Package` |
| package-check | `ic_lucide_package_check` | `PackageCheck` |
| receipt | `ic_lucide_receipt` | `Receipt` |
| credit-card | `ic_lucide_credit_card` | `CreditCard` |
| banknote | `ic_lucide_banknote` | `Banknote` |
| wallet | `ic_lucide_wallet` | `Wallet` |
| ticket-percent | `ic_lucide_ticket_percent` | `TicketPercent` |
| tag | `ic_lucide_tag` | `Tag` |
| gift | `ic_lucide_gift` | `Gift` |
| sparkles | `ic_lucide_sparkles` | `Sparkles` |
| zap | `ic_lucide_zap` | `Zap` |
| star | `ic_lucide_star` | `Star` |
| star-filled (**new**) | `ic_lucide_star_filled` | `StarFilled` |
| clock | `ic_lucide_clock` | `Clock` |
| calendar | `ic_lucide_calendar` | `Calendar` |
| navigation | `ic_lucide_navigation` | `Navigation` |
| route | `ic_lucide_route` | `Route` |
| store | `ic_lucide_store` | `Store` |
| bike | `ic_lucide_bike` | `Bike` |
| phone | `ic_lucide_phone` | `Phone` |
| message-circle | `ic_lucide_message_circle` | `MessageCircle` |
| mail | `ic_lucide_mail` | `Mail` |
| lock | `ic_lucide_lock` | `Lock` |
| eye | `ic_lucide_eye` | `Eye` |
| eye-off (**new**) | `ic_lucide_eye_off` | `EyeOff` |
| globe | `ic_lucide_globe` | `Globe` |
| languages | `ic_lucide_languages` | `Languages` |
| sun | `ic_lucide_sun` | `Sun` |
| moon | `ic_lucide_moon` | `Moon` |
| settings | `ic_lucide_settings` | `Settings` |
| log-out | `ic_lucide_log_out` | `LogOut` |
| circle-help | `ic_lucide_circle_help` | `CircleHelp` |
| info | `ic_lucide_info` | `Info` |
| circle-alert | `ic_lucide_circle_alert` | `CircleAlert` |
| circle-check | `ic_lucide_circle_check` | `CircleCheck` |
| check | `ic_lucide_check` | `Check` |
| x | `ic_lucide_x` | `X` |
| plus | `ic_lucide_plus` | `Plus` |
| minus | `ic_lucide_minus` | `Minus` |
| trash-2 | `ic_lucide_trash_2` | `Trash2` |
| pencil | `ic_lucide_pencil` | `Pencil` |
| camera | `ic_lucide_camera` | `Camera` |
| share-2 | `ic_lucide_share_2` | `Share2` |
| arrow-left | `ic_lucide_arrow_left` | `ArrowLeft` |
| arrow-right | `ic_lucide_arrow_right` | `ArrowRight` |
| chevron-right | `ic_lucide_chevron_right` | `ChevronRight` |
| chevron-down | `ic_lucide_chevron_down` | `ChevronDown` |
| refresh-cw | `ic_lucide_refresh_cw` | `RefreshCw` |
| award | `ic_lucide_award` | `Award` |
| flame | `ic_lucide_flame` | `Flame` |
| shirt | `ic_lucide_shirt` | `Shirt` |
| headphones | `ic_lucide_headphones` | `Headphones` |
| watch | `ic_lucide_watch` | `Watch` |
| smartphone | `ic_lucide_smartphone` | `Smartphone` |
| laptop | `ic_lucide_laptop` | `Laptop` |
| sofa | `ic_lucide_sofa` | `Sofa` |
| footprints | `ic_lucide_footprints` | `Footprints` |
| backpack | `ic_lucide_backpack` | `Backpack` |
| glasses | `ic_lucide_glasses` | `Glasses` |
| wifi-off (**new**) | `ic_lucide_wifi_off` | `WifiOff` |
| search-x (**new**) | `ic_lucide_search_x` | `SearchX` |
| mic (**new**) | `ic_lucide_mic` | `Mic` |

Where the new ones are used: `EyeOff` in the password field when the password is visible (Figma `03b`), `StarFilled` (tinted `colors.gold`) on every rating, `WifiOff` on the offline screens, `SearchX` on "No results" (05b.4), `Mic` in the search bar and the address voice note, `UserPlus` on the Sign up header tile.

## Step 1 — Import the SVGs

1. Go to **lucide.dev/icons**, search the name, then **Download SVG**. Keep the default 24 px, stroke 2.
2. Open each `.svg` in a text editor and replace `stroke="currentColor"` with `stroke="#000000"`. Vector Asset can't read `currentColor` and would drop the stroke. The color doesn't matter because `Icon` tints it.
3. Android Studio: right-click `res` → **New → Vector Asset** → *Local file (SVG, PSD)* → choose the file → name it `ic_lucide_<name>` (dashes become underscores) → size 24 × 24 → **Next → Finish**.
4. Open one generated XML and check that each `<path>` has `android:strokeWidth="2"`, `android:strokeLineCap="round"` and `android:strokeLineJoin="round"`.

Do the 10 auth/home icons first (`mail`, `lock`, `eye`, `eye-off`, `arrow-left`, `arrow-right`, `x`, `check`, `circle-alert`, `circle-check`). Import the rest when a screen needs them.

### Brand logos (Google, Facebook)

Brand marks are **not** Lucide and must keep their own colors.

- Keep the existing `ic_google.xml`.
- Add `ic_facebook.xml` from Facebook's brand resources (blue circle `#1877F2` with a white "f").
- Add `ic_whatsapp.xml` from WhatsApp's brand resources (green `#25D366` bubble with a white phone). Figma uses it at **22 dp** next to the opt-in checkbox (09.1, 11.2), the settings switch (11.3) and the courier's "WhatsApp" button (C03.2), and at **18 dp** in the "WhatsApp sent" attempt row.
- `ic_apple.xml` can be deleted once the auth screens follow Figma (doc 14 uses Google + Facebook).

Show them with `tint = Color.Unspecified`, so they're never recolored.

## Step 2 — `AppIcons.kt`

```kotlin
package com.example.dukkanapp.core.designsystem.icon

import androidx.annotation.DrawableRes
import com.example.dukkanapp.R

/**
 * Dukkan icon set (Lucide, 24 grid, 2 px stroke). Tint is always applied by the caller.
 *
 * Usage: `Icon(painterResource(AppIcons.Mail), contentDescription = null, tint = AppTheme.colors.textTertiary)`
 */
object AppIcons {
    // Navigation and app bar
    @DrawableRes val House = R.drawable.ic_lucide_house
    @DrawableRes val LayoutGrid = R.drawable.ic_lucide_layout_grid
    @DrawableRes val ShoppingBag = R.drawable.ic_lucide_shopping_bag
    @DrawableRes val Percent = R.drawable.ic_lucide_percent
    @DrawableRes val UserRound = R.drawable.ic_lucide_user_round
    @DrawableRes val UserPlus = R.drawable.ic_lucide_user_plus
    @DrawableRes val ArrowLeft = R.drawable.ic_lucide_arrow_left
    @DrawableRes val ArrowRight = R.drawable.ic_lucide_arrow_right
    @DrawableRes val ChevronRight = R.drawable.ic_lucide_chevron_right
    @DrawableRes val ChevronDown = R.drawable.ic_lucide_chevron_down
    @DrawableRes val X = R.drawable.ic_lucide_x

    // Search and filters
    @DrawableRes val Search = R.drawable.ic_lucide_search
    @DrawableRes val ScanLine = R.drawable.ic_lucide_scan_line
    @DrawableRes val SlidersHorizontal = R.drawable.ic_lucide_sliders_horizontal
    @DrawableRes val SearchX = R.drawable.ic_lucide_search_x
    @DrawableRes val Mic = R.drawable.ic_lucide_mic

    // Auth and account
    @DrawableRes val Mail = R.drawable.ic_lucide_mail
    @DrawableRes val Lock = R.drawable.ic_lucide_lock
    @DrawableRes val Eye = R.drawable.ic_lucide_eye
    @DrawableRes val EyeOff = R.drawable.ic_lucide_eye_off
    @DrawableRes val Phone = R.drawable.ic_lucide_phone
    @DrawableRes val Bell = R.drawable.ic_lucide_bell
    @DrawableRes val Globe = R.drawable.ic_lucide_globe
    @DrawableRes val Languages = R.drawable.ic_lucide_languages
    @DrawableRes val Sun = R.drawable.ic_lucide_sun
    @DrawableRes val Moon = R.drawable.ic_lucide_moon
    @DrawableRes val Settings = R.drawable.ic_lucide_settings
    @DrawableRes val LogOut = R.drawable.ic_lucide_log_out
    @DrawableRes val CircleHelp = R.drawable.ic_lucide_circle_help
    @DrawableRes val Camera = R.drawable.ic_lucide_camera
    @DrawableRes val Pencil = R.drawable.ic_lucide_pencil

    // Status
    @DrawableRes val Info = R.drawable.ic_lucide_info
    @DrawableRes val CircleAlert = R.drawable.ic_lucide_circle_alert
    @DrawableRes val CircleCheck = R.drawable.ic_lucide_circle_check
    @DrawableRes val Check = R.drawable.ic_lucide_check
    @DrawableRes val RefreshCw = R.drawable.ic_lucide_refresh_cw
    @DrawableRes val WifiOff = R.drawable.ic_lucide_wifi_off

    // Commerce
    @DrawableRes val Heart = R.drawable.ic_lucide_heart
    @DrawableRes val Plus = R.drawable.ic_lucide_plus
    @DrawableRes val Minus = R.drawable.ic_lucide_minus
    @DrawableRes val Trash2 = R.drawable.ic_lucide_trash_2
    @DrawableRes val Share2 = R.drawable.ic_lucide_share_2
    @DrawableRes val Receipt = R.drawable.ic_lucide_receipt
    @DrawableRes val CreditCard = R.drawable.ic_lucide_credit_card
    @DrawableRes val Banknote = R.drawable.ic_lucide_banknote
    @DrawableRes val Wallet = R.drawable.ic_lucide_wallet
    @DrawableRes val TicketPercent = R.drawable.ic_lucide_ticket_percent
    @DrawableRes val Tag = R.drawable.ic_lucide_tag
    @DrawableRes val Gift = R.drawable.ic_lucide_gift
    @DrawableRes val Sparkles = R.drawable.ic_lucide_sparkles
    @DrawableRes val Zap = R.drawable.ic_lucide_zap
    @DrawableRes val Star = R.drawable.ic_lucide_star
    // Ratings: tint with AppTheme.colors.gold. Lucide's site only ships the outline star, so if
    // Figma's export isn't handy: copy ic_lucide_star.xml to ic_lucide_star_filled.xml and add
    // android:fillColor="#FF000000" to the path (the tint colours it, like every other icon).
    @DrawableRes val StarFilled = R.drawable.ic_lucide_star_filled
    @DrawableRes val Award = R.drawable.ic_lucide_award
    @DrawableRes val Flame = R.drawable.ic_lucide_flame
    @DrawableRes val Store = R.drawable.ic_lucide_store

    // Delivery and location
    @DrawableRes val MapPin = R.drawable.ic_lucide_map_pin
    @DrawableRes val LocateFixed = R.drawable.ic_lucide_locate_fixed
    @DrawableRes val Truck = R.drawable.ic_lucide_truck
    @DrawableRes val Package = R.drawable.ic_lucide_package
    @DrawableRes val PackageCheck = R.drawable.ic_lucide_package_check
    @DrawableRes val Clock = R.drawable.ic_lucide_clock
    @DrawableRes val Calendar = R.drawable.ic_lucide_calendar
    @DrawableRes val Navigation = R.drawable.ic_lucide_navigation
    @DrawableRes val Route = R.drawable.ic_lucide_route
    @DrawableRes val Bike = R.drawable.ic_lucide_bike
    @DrawableRes val MessageCircle = R.drawable.ic_lucide_message_circle

    // Categories
    @DrawableRes val Shirt = R.drawable.ic_lucide_shirt
    @DrawableRes val Headphones = R.drawable.ic_lucide_headphones
    @DrawableRes val Watch = R.drawable.ic_lucide_watch
    @DrawableRes val Smartphone = R.drawable.ic_lucide_smartphone
    @DrawableRes val Laptop = R.drawable.ic_lucide_laptop
    @DrawableRes val Sofa = R.drawable.ic_lucide_sofa
    @DrawableRes val Footprints = R.drawable.ic_lucide_footprints
    @DrawableRes val Backpack = R.drawable.ic_lucide_backpack
    @DrawableRes val Glasses = R.drawable.ic_lucide_glasses

    // Brand logos: never tint (use Color.Unspecified)
    @DrawableRes val Google = R.drawable.ic_google
    @DrawableRes val Facebook = R.drawable.ic_facebook
    @DrawableRes val WhatsApp = R.drawable.ic_whatsapp
}
```

Comment out the lines whose drawable you haven't imported yet, otherwise the build fails with "Unresolved reference".

### Usage

```kotlin
Icon(
    painter = painterResource(AppIcons.Mail),
    contentDescription = null,                 // decorative next to a label
    tint = AppTheme.colors.textTertiary,
    modifier = Modifier.size(AppDimens.icon),
)
```

### RTL arrows

Back and forward arrows must flip in Arabic. Add this to `ic_lucide_arrow_left.xml`, `ic_lucide_arrow_right.xml` and `ic_lucide_chevron_right.xml`, on the root `<vector>` tag:

```xml
android:autoMirrored="true"
```

Never mirror icons that aren't directional (search, heart, clock).

## Step 3 — Logo

Figma `05 · Logo`: **"A shop awning with a smile — 'dukkan' means 'shop'. 4.4 px stroke on a 48 grid, Brand gradient. No background. Use the mark alone at 48 px and below."**

| Asset | Spec |
|---|---|
| Mark | awning + smile, 48 × 48 grid, 4.4 stroke, `brand` gradient (Azure400 → Blue700, 135°) |
| Horizontal lockup | mark + "dukkan" wordmark, ExtraBold, −3.5 % tracking |
| Arabic lockup | mark + "دكّان" |
| On colour / mono | white mark on brand or dark backgrounds |
| App icon | launcher only: full spec in `05b · Android app icon` (Step 4) |

### Export the mark

1. In Figma, select the `Mark` layer in `05 · Logo` → **Export → SVG**.
2. Import with Vector Asset as `ic_dukkan_mark` (the gradient stroke becomes an `<aapt:attr>` gradient, supported on our `minSdk 24`).
3. Also export a **white** version as `ic_dukkan_mark_white` (for the launcher foreground and dark backgrounds).
4. Delete `ic_app_logo.xml` once nothing references it.

### Code — `AppLogo.kt`

The wordmark is live text, not an image, so it stays sharp and follows the font.

```kotlin
package com.example.dukkanapp.core.designsystem.components.logo

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.em
import com.example.dukkanapp.R
import com.example.dukkanapp.core.designsystem.theme.AppDimens
import com.example.dukkanapp.core.designsystem.theme.AppTheme

/** The Dukkan mark alone. Use it at 48 dp and below (Figma rule). */
@Composable
fun AppLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = AppDimens.logoMark,
) {
    Image(
        painter = painterResource(R.drawable.ic_dukkan_mark),
        contentDescription = stringResource(R.string.app_name),
        modifier = modifier.size(size),
    )
}

/** Mark + "dukkan" wordmark (or "دكّان" in Arabic, from strings.xml). */
@Composable
fun AppLogo(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.space8),
    ) {
        AppLogoMark(size = AppDimens.controlSm)
        Text(
            text = stringResource(R.string.logo_wordmark),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.035).em,
            ),
            color = AppTheme.colors.textPrimary,
        )
    }
}
```

Add `logo_wordmark` to the strings files: `dukkan` in `values/` and `values-fr/`, `دكّان` in `values-ar/`.
`(-0.035).em` is Figma's −3.5 % tracking: `em` scales with the font size, just like a Figma percentage.

## Step 4 — Launcher icon (adaptive), Figma `05b · Android app icon`

### Figma spec

| Item | Value |
|---|---|
| Canvas | **108 × 108 dp** per layer |
| Visible area | inner **72 dp** (the launcher masks the rest) |
| Safe zone | inner **66 dp**: the mark must stay inside it |
| Foreground | `ic_launcher_foreground`: **white** mark, ≈ 40 dp wide, centered |
| Background | `ic_launcher_background`: gradient **azure-400 `#4F8BFF` → blue-700 `#1D4ED8`** (135°, the `brand` gradient) |
| Monochrome | `ic_launcher_monochrome`: one color, same geometry as the foreground (Android 13 themed icons) |
| Masks | circle, squircle, rounded square, square (legacy): same layers, the OEM picks the shape |
| Legacy PNG | mdpi 48 · hdpi 72 · xhdpi 96 · xxhdpi 144 · xxxhdpi 192 px (Android < 8) |
| Play Store | 512 × 512 px PNG |

"Export as vector drawables" (Figma): all three layers are XML vectors, so they stay sharp at every density.

### 4.1 Background — `res/drawable/ic_launcher_background.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path android:pathData="M0,0h108v108h-108z">
        <aapt:attr name="android:fillColor">
            <!-- Figma Gradient/Brand: azure-400 → blue-700, 135° (top-left → bottom-right) -->
            <gradient
                android:type="linear"
                android:startX="0"
                android:startY="0"
                android:endX="108"
                android:endY="108">
                <item android:offset="0" android:color="#FF4F8BFF" />
                <item android:offset="1" android:color="#FF1D4ED8" />
            </gradient>
        </aapt:attr>
    </path>
</vector>
```

`aapt:attr` lets a vector use a gradient fill. It works on all our supported Android versions (`minSdk 24`).

### 4.2 Foreground — `res/drawable/ic_launcher_foreground.xml`

1. In Figma, select the white mark in `05b → Foreground` → **Export → SVG**.
2. Android Studio: **New → Vector Asset** → *Local file* → name `ic_launcher_foreground_mark`.
3. Create `ic_launcher_foreground.xml` that places the 40 dp mark in the middle of the 108 dp canvas with an `<inset>`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- 108 dp canvas, 40 dp mark centered: (108 - 40) / 2 = 34 dp on each side -->
<inset xmlns:android="http://schemas.android.com/apk/res/android"
    android:drawable="@drawable/ic_launcher_foreground_mark"
    android:inset="34dp" />
```

The 40 dp mark sits well inside the 66 dp safe zone, so no launcher shape ever cuts it.

### 4.3 Monochrome — `res/drawable/ic_launcher_monochrome.xml`

Same geometry, one color. The system recolors it for themed icons, so the color in the file doesn't matter:

```xml
<?xml version="1.0" encoding="utf-8"?>
<inset xmlns:android="http://schemas.android.com/apk/res/android"
    android:drawable="@drawable/ic_launcher_foreground_mark"
    android:inset="34dp" />
```

(If the mark's vector uses a white fill/stroke, that's fine: Android only uses the alpha of a monochrome layer.)

### 4.4 Adaptive icon — `res/mipmap-anydpi-v26/ic_launcher.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
```

Copy the same file to `ic_launcher_round.xml`. `<monochrome>` is read on Android 13+ and ignored before, so it's safe everywhere.

### 4.5 Legacy PNGs and the Play Store icon

1. Right-click `res` → **New → Image Asset** → *Launcher Icons (Legacy only)*.
2. Foreground: *Image* → `ic_launcher_foreground_mark`, background: *Image* → `ic_launcher_background`.
3. It writes `mipmap-mdpi … xxxhdpi/ic_launcher.png` at **48 / 72 / 96 / 144 / 192 px**. Don't let it overwrite the `mipmap-anydpi-v26` files above (uncheck them if asked).
4. Export a **512 × 512 px** PNG from Figma `05b → Legacy & store sizes` for the Play Console.

## Step 5 — Splash screen and XML colors

XML resources can't read Compose tokens, so `colors.xml` is the **second and last** place hex is allowed. Keep it tiny and name the colors after the tokens.

`res/values/colors.xml` (full file, purple/teal removed):

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Mirrors AppPalette for XML-only places (splash, launcher). Keep in sync. -->
    <color name="dukkan_background">#FFF7F9FB</color>
    <color name="dukkan_brand">#FF2563EB</color>
    <color name="dukkan_background_dark">#FF0B1220</color> <!-- night-950 -->
</resources>
```

`res/values/themes.xml`: add the splash theme (keep `Theme.DukkanApp`):

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.DukkanApp" parent="Theme.AppCompat.Light.NoActionBar" />

    <style name="Theme.DukkanApp.Splash" parent="Theme.SplashScreen">
        <item name="windowSplashScreenBackground">@color/dukkan_background</item>
        <item name="windowSplashScreenAnimatedIcon">@drawable/ic_dukkan_mark</item>
        <item name="postSplashScreenTheme">@style/Theme.DukkanApp</item>
    </style>
</resources>
```

Create `res/values-night/themes.xml` with the same splash style, but with `@color/dukkan_background_dark` as the background.
In `AndroidManifest.xml`, the `<activity android:name=".MainActivity">` must use `android:theme="@style/Theme.DukkanApp.Splash"`. If your project already defines a splash style, edit that one instead of adding a second.

Figma's splash (01.1) also shows "Shop smart. Delivered fast." and "Made in Tunisia · v1.0". The system splash can only show an icon, so put those lines on the first Compose screen if you want them. Doc 15 lists it.

## Step 6 — Remove Iconsax (last)

When `grep -rn "Iconsax" app/src` returns nothing:

1. Delete `implementation(libs.iconsax.compose)` from `app/build.gradle.kts`.
2. Delete `iconsax = "1.0.1"` and the `iconsax-compose` line from `gradle/libs.versions.toml`.
3. Keep `material-icons-core` only if something still uses `Icons.*`. The goal is zero.

| Old | New |
|---|---|
| `Iconsax.Linear.Sms` | `AppIcons.Mail` |
| `Iconsax.Linear.PasswordCheck` | `AppIcons.Lock` |
| `Iconsax.Linear.Eye` / `EyeSlash` | `AppIcons.Eye` / `AppIcons.EyeOff` |
| `Icons.AutoMirrored.Filled.ArrowBack` | `AppIcons.ArrowLeft` (auto-mirrored) |
| `R.drawable.ic_email` | `AppIcons.Mail` |
| `R.drawable.ic_back_button` | `AppIcons.ArrowLeft` |
| `R.drawable.ic_app_logo` | `AppLogoMark()` |

## Check

- [ ] Each imported icon has stroke 2, round caps, and shows in the Vector Asset preview.
- [ ] `ic_lucide_arrow_left` flips in Arabic.
- [ ] `AppIcons` compiles (missing drawables commented out).
- [ ] Google and Facebook logos keep their colors (`tint = Color.Unspecified`).
- [ ] Launcher icon: blue gradient background, white mark, looks right in circle, squircle, rounded square and themed (light + dark) modes, like Figma `05b`.
- [ ] Legacy PNGs exist at 48 / 72 / 96 / 144 / 192 px; a 512 px store icon is exported.
- [ ] Splash background is `#F7F9FB` in light and `#0B1220` in dark.
- [ ] Iconsax is removed from Gradle once unused.
