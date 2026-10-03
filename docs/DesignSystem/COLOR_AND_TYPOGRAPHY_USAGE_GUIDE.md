# Dukkan App — MaterialTheme ColorScheme & Typography Guide

This document is a visual and practical reference guide for developers working on **Dukkan App**. It explains the exact purpose, naming conventions, screen placement, and code usage for every **Typography** and **ColorScheme** token in the project.

---

## 1. Core Concepts & Naming Rules

### Typography Rule of Thumb
| Family | Intended Use | Scale Range |
|---|---|---|
| **Display** | Massive hero text, splash screens, giant banners | 36sp – 57sp (Bold) |
| **Headline** | Main screen titles & major section headers | 24sp – 32sp (Bold / SemiBold) |
| **Title** | Cards, product names, dialog headers, list item titles | 14sp – 22sp (SemiBold) |
| **Body** | Long-form reading, product descriptions, input text | 12sp – 16sp (Normal) |
| **Label** | Buttons, text field labels, chips, badges, timestamps | 11sp – 14sp (Medium / SemiBold) |

---

### Color Naming Convention ("Role-Based Colors")
* **`[Color]`** (e.g., `primary`, `surface`, `error`): The background color of a component or element.
* **`on[Color]`** (e.g., `onPrimary`, `onSurface`, `onError`): The content color (text, icons) drawn **ON TOP OF** `[Color]`.
* **`[Color]Container`**: A lighter, lower-emphasis background variant (e.g., for chips, badges, cards).
* **`on[Color]Container`**: The text/icon color drawn **ON TOP OF** `[Color]Container`.

> [!IMPORTANT]
> **Always pair colors correctly!**
> If your container background is `MaterialTheme.colorScheme.primary`, the text on top of it MUST be `MaterialTheme.colorScheme.onPrimary`.

---

## 2. Visual Screen Diagram (E-Commerce Screen Example)

Here is a visual map of a typical **Product Details / Home Screen** showing where each **Color** and **Typography** token is applied:

```
┌────────────────────────────────────────────────────────────────────────┐
│ [ Top App Bar ]                                                        │
│ Background: colorScheme.surface                                        │
│ Text ("Product Details"): typography.titleLarge + colorScheme.onSurface │
├────────────────────────────────────────────────────────────────────────┤
│ ┌────────────────────────────────────────────────────────────────────┐ │
│ │ [ Hero Image Banner / Product Preview ]                            │ │
│ │                                                                    │ │
│ │  [ SALE -20% Badge ]                                 [ ❤ Favorite ]│ │
│ │  Bg: extendedColors.sale                             Bg: surface   │ │
│ │  Text: extendedColors.onSale                         Icon: favorite│ │
│ │  Type: typography.labelSmall                                       │ │
│ └────────────────────────────────────────────────────────────────────┘ │
│                                                                        │
│ "Wireless Noise-Canceling Headphones"   <-- typography.headlineSmall   │
│                                             Color: colorScheme.onSurface │
│                                                                        │
│ "Electronics / Audio"                   <-- typography.bodyMedium      │
│                                             Color: onSurfaceVariant    │
│                                                                        │
│ $199.99   ~~$249.99~~                   <-- Price: typography.titleLarge│
│ Color:    Color:                            Color: colorScheme.primary │
│ primary   outline                       Original: typography.bodySmall │
│                                                                        │
│ ┌────────────────────────────────────────────────────────────────────┐ │
│ │ ⚡ Only 3 items left in stock!  (Low Stock Warning Banner)          │ │
│ │ Container: extendedColors.warningContainer                         │ │
│ │ Text: extendedColors.onWarningContainer | Type: typography.bodySmall│ │
│ └────────────────────────────────────────────────────────────────────┘ │
│                                                                        │
│ Product Description                      <-- typography.titleMedium    │
│ "Experience premium sound with active..." <-- typography.bodyMedium    │
│                                             Color: colorScheme.onSurface │
│                                                                        │
│ ┌────────────────────────────────────────────────────────────────────┐ │
│ │ [ Text Input Field - e.g., Promo Code or Order Note ]              │ │
│ │ Container Fill: colorScheme.surfaceVariant                         │ │
│ │ Border / Outline: colorScheme.outline                              │ │
│ │ Label ("Promo Code"): typography.labelMedium + onSurfaceVariant    │ │
│ │ Input Value: typography.bodyMedium + onSurface                     │ │
│ └────────────────────────────────────────────────────────────────────┘ │
│                                                                        │
│ ┌──────────────────────────────┐  ┌──────────────────────────────────┐ │
│ │ [ Add to Cart ]              │  │ [ Buy Now ]                      │ │
│ │ Bg: colorScheme.secondary    │  │ Bg: colorScheme.primary          │ │
│ │ Text: colorScheme.onSecondary│  │ Text: colorScheme.onPrimary      │ │
│ │ Type: typography.labelLarge  │  │ Type: typography.labelLarge      │ │
│ └──────────────────────────────┘  └──────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Typography Tokens Reference (`MaterialTheme.typography`)

All typography tokens in Dukkan App use the custom font family **Konnect**.

| Token | Size / Weight | Line Height | Primary Use Case | Example Code |
|---|---|---|---|---|
| **`displayLarge`** | 57sp / Bold | 64sp | Giant splash titles, mega promotional numbers (e.g. "70% OFF") | `Text("70% OFF", style = MaterialTheme.typography.displayLarge)` |
| **`displayMedium`** | 45sp / Bold | 52sp | Hero section numbers, big onboarding headers | `Text("Welcome", style = MaterialTheme.typography.displayMedium)` |
| **`displaySmall`** | 36sp / Bold | 44sp | Large promotional banners, splash screen sub-titles | `Text("Flash Sale", style = MaterialTheme.typography.displaySmall)` |
| **`headlineLarge`** | 32sp / Bold | 40sp | Top-level screen title when no app bar is present | `Text("My Cart", style = MaterialTheme.typography.headlineLarge)` |
| **`headlineMedium`** | 28sp / Bold | 36sp | Major screen headings, feature section title | `Text("Categories", style = MaterialTheme.typography.headlineMedium)` |
| **`headlineSmall`** | 24sp / SemiBold | 32sp | Product detail titles, bottom sheet main headers | `Text(product.title, style = MaterialTheme.typography.headlineSmall)` |
| **`titleLarge`** | 22sp / SemiBold | 28sp | TopAppBar title, product price, dialog header | `Text("$199.99", style = MaterialTheme.typography.titleLarge)` |
| **`titleMedium`** | 16sp / SemiBold | 24sp | Section titles ("Description", "Reviews"), card titles | `Text("Description", style = MaterialTheme.typography.titleMedium)` |
| **`titleSmall`** | 14sp / SemiBold | 20sp | Sub-headers inside cards, compact list item titles | `Text("Shipping Address", style = MaterialTheme.typography.titleSmall)` |
| **`bodyLarge`** | 16sp / Normal | 24sp | Long reading paragraphs, detailed product specs | `Text(product.details, style = MaterialTheme.typography.bodyLarge)` |
| **`bodyMedium`** | 14sp / Normal | 20sp | Default body text, text field typed values, list body | `Text(description, style = MaterialTheme.typography.bodyMedium)` |
| **`bodySmall`** | 12sp / Normal | 16sp | Helper text under input fields, secondary details, timestamps | `Text("Includes taxes", style = MaterialTheme.typography.bodySmall)` |
| **`labelLarge`** | 14sp / SemiBold | 20sp | Primary & Secondary Button text, main tab labels | `Text("Add to Cart", style = MaterialTheme.typography.labelLarge)` |
| **`labelMedium`** | 12sp / Medium | 16sp | Text field floating labels, chip text, filter tag text | `Text("Category", style = MaterialTheme.typography.labelMedium)` |
| **`labelSmall`** | 11sp / Medium | 16sp | Small badges, discount tags, caption notes | `Text("-20% OFF", style = MaterialTheme.typography.labelSmall)` |

---

## 4. Color Scheme Tokens Reference (`MaterialTheme.colorScheme`)

### Primary Colors (Brand Identity - Blue)
* **`primary`**: Main interactive elements, primary action button backgrounds, active switches, progress indicators.
* **`onPrimary`**: Text and icons placed on top of `primary` (e.g. text inside a primary button).
* **`primaryContainer`**: Soft container background for selected items, highlighted chips, active tab pills.
* **`onPrimaryContainer`**: Text and icons placed inside a `primaryContainer`.

### Secondary Colors (Commerce Accent - Orange)
* **`secondary`**: Secondary call-to-action buttons (e.g., "Add to Cart"), promotional highlights, special action floating buttons.
* **`onSecondary`**: Text/icons placed on top of `secondary`.
* **`secondaryContainer`**: Soft orange background for promo banners, coupon tags, offer cards.
* **`onSecondaryContainer`**: Text/icons placed inside `secondaryContainer`.

### Tertiary Colors (Secondary Accent - Teal)
* **`tertiary`**: Third accent color for dynamic status, user badges, rating stars, dynamic filter highlights.
* **`onTertiary`**: Text/icons placed on top of `tertiary`.
* **`tertiaryContainer`**: Soft teal container background.
* **`onTertiaryContainer`**: Text/icons placed inside `tertiaryContainer`.

### Neutral & Surface Colors (Layout Structure)
* **`background`**: Root screen background color (`Slate50` in light theme, `Slate950` in dark theme).
* **`onBackground`**: Default primary text color drawn directly on the screen background.
* **`surface`**: Elevated containers like Cards, Sheets, Top App Bars, Bottom Navigation Bars, Dialogs.
* **`onSurface`**: Primary text and icons inside Cards, Sheets, and Top App Bars.
* **`surfaceVariant`**: Secondary surface background (e.g., filled text input background, unselected card fills, table headers).
* **`onSurfaceVariant`**: Secondary / muted text color (e.g., input field placeholders, category subtitles, inactive icons).
* **`surfaceTint`**: Tint color applied to elevated surfaces in Material 3.

### Borders & Overlays
* **`outline`**: High-contrast borders (e.g., OutlinedTextField border, card borders, active dividers).
* **`outlineVariant`**: Low-contrast borders and horizontal dividers between list items.
* **`scrim`**: Semi-transparent dark overlay behind modal bottom sheets and dialogs.

### Error & Validation Colors
* **`error`**: Destructive actions (e.g., "Delete", "Remove"), validation error borders, error icons.
* **`onError`**: Text/icons placed on top of `error`.
* **`errorContainer`**: Soft red background for error banner cards and form validation error boxes.
* **`onErrorContainer`**: Text/icons placed inside `errorContainer`.

---

## 5. Dukkan Extended Colors (`AppTheme.extendedColors`)

Dukkan App defines custom domain colors for e-commerce states using `AppTheme.extendedColors`:

| Token | Use Case | Example Element |
|---|---|---|
| **`success` / `onSuccess`** | Positive states, completed orders | "Order Placed" status badge |
| **`successContainer` / `onSuccessContainer`** | Soft green background for success messages | Checkout success banner |
| **`warning` / `onWarning`** | Cautionary status, low stock alerts | "Only 2 left" text |
| **`warningContainer` / `onWarningContainer`** | Soft yellow/amber background for warnings | Pending payment alert banner |
| **`info` / `onInfo`** | Neutral informational notices | Delivery tracking status banner |
| **`infoContainer` / `onInfoContainer`** | Soft blue background for info cards | "Free shipping on orders over $50" |
| **`sale` / `onSale`** | E-commerce discount & flash sale tags | "-30% OFF" badge background |
| **`favorite`** | Wishlist / Favorite heart icon | Heart icon button on product cards |

---

## 6. Code Examples (Practical Usage in Jetpack Compose)

### Example 1: Product Card Item
```kotlin
@Composable
fun ProductCard(
    productName: String,
    category: String,
    price: String,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Favorite Icon
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) AppTheme.extendedColors.favorite else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Product Name (Title)
            Text(
                text = productName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Category (Muted Subtitle)
            Text(
                text = category,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Price (Primary Accent)
            Text(
                text = price,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
```

---

### Example 2: Status / Alert Banner
```kotlin
@Composable
fun LowStockWarningBanner(itemsRemaining: Int) {
    Surface(
        color = AppTheme.extendedColors.warningContainer,
        contentColor = AppTheme.extendedColors.onWarningContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = AppTheme.extendedColors.warning
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Hurry! Only $itemsRemaining left in stock",
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.extendedColors.onWarningContainer
            )
        }
    }
}
```

---

### Example 3: Primary vs Secondary CTA Buttons
```kotlin
@Composable
fun ProductActionButtons(
    onAddToCart: () -> Unit,
    onBuyNow: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = arrangement.spacedBy(12.dp)
    ) {
        // Secondary Action (Orange Accent)
        Button(
            onClick = onAddToCart,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            )
        ) {
            Text(
                text = "Add to Cart",
                style = MaterialTheme.typography.labelLarge
            )
        }

        // Primary Action (Brand Blue)
        Button(
            onClick = onBuyNow,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = "Buy Now",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
```

---

## 7. Quick Summary / Cheat Sheet

| I want to style... | Color Token to use | Typography Token to use |
|---|---|---|
| Main Screen Title | `colorScheme.onBackground` or `onSurface` | `typography.headlineMedium` / `headlineSmall` |
| Section Header ("Description") | `colorScheme.onSurface` | `typography.titleMedium` |
| Product Title on Card | `colorScheme.onSurface` | `typography.titleMedium` |
| Product Price | `colorScheme.primary` | `typography.titleLarge` |
| Category / Subtitle / Muted text | `colorScheme.onSurfaceVariant` | `typography.bodyMedium` |
| Paragraph / Description Body | `colorScheme.onSurface` | `typography.bodyLarge` / `bodyMedium` |
| Primary Button ("Submit", "Buy Now") | Bg: `colorScheme.primary`, Text: `onPrimary` | `typography.labelLarge` |
| Secondary Button ("Add to Cart") | Bg: `colorScheme.secondary`, Text: `onSecondary` | `typography.labelLarge` |
| Text Field Typed Input Text | `colorScheme.onSurface` | `typography.bodyMedium` |
| Text Field Placeholder / Label | `colorScheme.onSurfaceVariant` | `typography.labelMedium` |
| Sale / Discount Tag | Bg: `extendedColors.sale`, Text: `onSale` | `typography.labelSmall` |
| Wishlist Heart Icon | `extendedColors.favorite` | N/A |
| Low Stock / Warning Banner | Bg: `extendedColors.warningContainer`, Text: `onWarningContainer` | `typography.bodySmall` |
| Order Completed / Success | Bg: `extendedColors.successContainer`, Text: `onSuccessContainer` | `typography.bodyMedium` |
