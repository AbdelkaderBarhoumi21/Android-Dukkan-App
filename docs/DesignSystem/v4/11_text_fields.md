# 11: Text fields (`AppTextField`, `AppPasswordField`, `AppPasswordStrength`, `AppSearchBar`, `AppOtpField`)

**Files (new):**

- `core/designsystem/components/textfield/AppFieldStatus.kt`
- `core/designsystem/components/textfield/AppTextField.kt`
- `core/designsystem/components/textfield/AppPasswordField.kt`
- `core/designsystem/components/textfield/AppPasswordStrength.kt`
- `core/designsystem/components/textfield/AppSearchBar.kt`
- `core/designsystem/components/textfield/AppOtpField.kt`

**Replaces:** `core/common/components/TextField/AppTextField.kt` (`AppTextFiled`, typo, plus `AppTextFieldDefault`) and `core/common/components/TextField/AppPasswordTextField.kt`.

> **Fix while you are here.** The old folder is `TextField` (capital T), but its files declare `package ...components.textfield`. The folder and the package don't match, which is the same bug we fixed for `Login/login`. The new files live in `designsystem/components/textfield/` (all lowercase). Delete the old `TextField` folder once [14](14_auth_screens_refactor.md) is done. The function name typo `AppTextFiled` disappears with it.

---

## Why

Figma's field (`Input/Email`) is **not** a Material outlined field:

1. **The label sits above the box** ("Email", 12.5 sp Bold). An `OutlinedTextField` label floats inside the border.
2. **The box is exactly 52 dp tall and fully rounded** (a pill, "fully rounded like buttons" in Figma). `OutlinedTextField` has a 56 dp minimum and internal paddings you can't fully remove. The old code forced `height(45.dp)`, which clips descenders on large font scales.
3. **Focus adds a 3 dp soft blue ring *outside* the border**, also a pill (58 dp tall). Material has no ring.
4. **Error and success tint the fill** (red-soft and white) and show a helper with an icon.
5. **Disabled is drawn with tokens, not 50 % opacity** (Figma `03b`), "so it also works on dark".

So we build on `BasicTextField` plus `decorationBox`. `BasicTextField` gives us the editing engine (cursor, selection, IME, autofill) and nothing else. We draw the rest from tokens.

- **Single responsibility:** `appFieldColors()` maps (status, focused) to colors. `AppTextField` lays out the label, box and helper. `AppPasswordField`, `AppSearchBar` and `AppOtpField` compose or specialise it.
- **Composition over inheritance:** the password field *wraps* `AppTextField` and only adds the eye toggle.
- **Stateless (MVI friendly):** the value comes in, `onValueChange` goes out. The ViewModel owns the text, exactly like today.

---

## Figma spec: `Input/Email` (`03 · Atoms` + `03b · Atom states`)

Layout:

- Label (`labelMedium`, 12.5/16 Bold, `textPrimary`), then a gap of `space8`.
- Box: height `controlLg` (52), shape **`AppPillShape`** (radius/full → 26 dp corners), content padding start `space20` and end `space12` (the round end needs a little more room than a square corner).
- Leading icon at `icon` (20) with a gap of `space8` before the text.
- Text in `textStyles.fieldText` (Field/Text 14.5 SemiBold). Placeholder in `textStyles.fieldPlaceholder` (14.5 Medium, `textTertiary`).
- Helper: gap `space4`, a 14 dp icon, then the text in `textStyles.helper` (Helper 11.5 SemiBold).

| State | Fill | Border | Ring (outside) | Label | Value / icon | Helper |
|---|---|---|---|---|---|---|
| Default | `field` | none | none | `textPrimary` | `textPrimary` / `textTertiary` | none, or `textSecondary` text |
| Focus | `card` | `brand` 1.5 dp (`strokeFocus`) | `brandRing` 3 dp pill (`focusRing`) | `textPrimary` | `textPrimary` / `brand` | as Default |
| Filled (not focused) | `field` | none | none | `textPrimary` | `textPrimary` / `textTertiary` | none |
| Error | `errorSoft` | `error` 1.5 dp | none (error wins over focus) | `textPrimary` | `textPrimary` / `error` | `error` + `CircleAlert` icon |
| Success | `card` | `success` 1.5 dp | none | `textPrimary` | `textPrimary` / `textTertiary` | `success` + `CircleCheck` icon |
| **Disabled** | `field` | **none** | **none** | **`textTertiary`** | **`textTertiary` / `textTertiary`** | `textTertiary` |

Priority when several apply: **Disabled > Error > Success > Focused > Default.**

---

## Code

### `AppFieldStatus.kt`

```kotlin
package com.example.dukkanapp.core.designsystem.components.textfield

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.example.dukkanapp.core.designsystem.theme.AppTheme

/** Validation look of a field. The ViewModel decides it; the field only draws it. */
enum class AppFieldStatus { Default, Error, Success }

@Immutable
internal data class AppFieldColors(
    val fill: Color,
    val border: Color?,
    val showRing: Boolean,
    val label: Color,
    val text: Color,
    val leadingIcon: Color,
    val helper: Color,
)

@Composable
internal fun appFieldColors(
    status: AppFieldStatus,
    isFocused: Boolean,
    enabled: Boolean = true,
): AppFieldColors {
    val colors = AppTheme.colors
    return when {
        // Figma 03b: disabled is drawn with tokens, not 50 % opacity, so it also works on dark.
        !enabled -> AppFieldColors(
            fill = colors.field,
            border = null,
            showRing = false,
            label = colors.textTertiary,
            text = colors.textTertiary,
            leadingIcon = colors.textTertiary,
            helper = colors.textTertiary,
        )

        status == AppFieldStatus.Error -> AppFieldColors(
            fill = colors.errorSoft,
            border = colors.error,
            showRing = false,
            label = colors.textPrimary,
            text = colors.textPrimary,
            leadingIcon = colors.error,
            helper = colors.error,
        )

        status == AppFieldStatus.Success -> AppFieldColors(
            fill = colors.card,
            border = colors.success,
            showRing = false,
            label = colors.textPrimary,
            text = colors.textPrimary,
            leadingIcon = colors.textTertiary,
            helper = colors.success,
        )

        isFocused -> AppFieldColors(
            fill = colors.card,
            border = colors.brand,
            showRing = true,
            label = colors.textPrimary,
            text = colors.textPrimary,
            leadingIcon = colors.brand,
            helper = colors.textSecondary,
        )

        else -> AppFieldColors(
            fill = colors.field,
            border = null,
            showRing = false,
            label = colors.textPrimary,
            text = colors.textPrimary,
            leadingIcon = colors.textTertiary,
            helper = colors.textSecondary,
        )
    }
}
```

### `AppTextField.kt`

```kotlin
package com.example.dukkanapp.core.designsystem.components.textfield

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.dukkanapp.core.designsystem.icon.AppIcons
import com.example.dukkanapp.core.designsystem.theme.AppDimens
import com.example.dukkanapp.core.designsystem.theme.AppPillShape
import com.example.dukkanapp.core.designsystem.theme.AppTheme

/** Figma: "fully rounded like buttons" (radius/full). */
private val FieldShape = AppPillShape

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: Painter? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    helperText: String? = null,
    status: AppFieldStatus = AppFieldStatus.Default,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    interactionSource: MutableInteractionSource? = null,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isFocused by source.collectIsFocusedAsState()
    val fieldColors = appFieldColors(status, isFocused, enabled)
    val ringColor = AppTheme.colors.brandRing
    val textStyles = AppTheme.textStyles

    Column(modifier = modifier.fillMaxWidth()) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = fieldColors.label,
            )
            Spacer(Modifier.height(AppDimens.space8))
        }

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(AppDimens.controlLg)
                // Focus ring drawn OUTSIDE the box: no layout shift when focus changes.
                // The box is a pill, so the ring is a pill too: radius = half the ring height.
                .drawBehind {
                    if (fieldColors.showRing) {
                        val ring = AppDimens.focusRing.toPx()
                        val radius = size.height / 2 + ring
                        drawRoundRect(
                            color = ringColor,
                            topLeft = Offset(-ring, -ring),
                            size = Size(size.width + ring * 2, size.height + ring * 2),
                            cornerRadius = CornerRadius(radius, radius),
                        )
                    }
                }
                .background(fieldColors.fill, FieldShape)
                .then(
                    if (fieldColors.border != null) {
                        Modifier.border(AppDimens.strokeFocus, fieldColors.border, FieldShape)
                    } else {
                        Modifier
                    }
                )
                // TalkBack reads "Error: <helper>" on the field itself.
                .semantics {
                    if (status == AppFieldStatus.Error && helperText != null) error(helperText)
                },
            enabled = enabled,
            readOnly = readOnly,
            singleLine = singleLine,
            textStyle = textStyles.fieldText.copy(color = fieldColors.text),
            cursorBrush = SolidColor(AppTheme.colors.brand),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            interactionSource = source,
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier.padding(start = AppDimens.space20, end = AppDimens.space12),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (leadingIcon != null) {
                        Icon(
                            painter = leadingIcon,
                            contentDescription = null,
                            tint = fieldColors.leadingIcon,
                            modifier = Modifier.size(AppDimens.icon),
                        )
                        Spacer(Modifier.width(AppDimens.space8))
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(
                                text = placeholder,
                                style = textStyles.fieldPlaceholder,
                                color = AppTheme.colors.textTertiary,
                                maxLines = 1,
                            )
                        }
                        innerTextField()
                    }
                    trailingIcon?.invoke()
                }
            },
        )

        if (helperText != null) {
            Spacer(Modifier.height(AppDimens.space4))
            AppFieldHelper(text = helperText, status = status, color = fieldColors.helper)
        }
    }
}

@Composable
private fun AppFieldHelper(text: String, status: AppFieldStatus, color: Color) {
    Row(
        modifier = Modifier.padding(start = AppDimens.space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.space4),
    ) {
        val icon = when (status) {
            AppFieldStatus.Error -> AppIcons.CircleAlert
            AppFieldStatus.Success -> AppIcons.CircleCheck
            AppFieldStatus.Default -> null
        }
        if (icon != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(text = text, style = AppTheme.textStyles.helper, color = color)
    }
}
```

> `14.dp` for the helper icon is the only raw size here. Figma draws it at 14, which is between `iconSm` (16) and nothing smaller. If you use it twice, add `iconXs = 14.dp` to `AppDimens` and replace it.

### `AppPasswordField.kt`

```kotlin
package com.example.dukkanapp.core.designsystem.components.textfield

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.layout.size
import com.example.dukkanapp.R
import com.example.dukkanapp.core.designsystem.theme.AppDimens
import com.example.dukkanapp.core.designsystem.icon.AppIcons
import com.example.dukkanapp.core.designsystem.theme.AppTheme

@Composable
fun AppPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    helperText: String? = null,
    status: AppFieldStatus = AppFieldStatus.Default,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Done,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    // UI-only state: survives rotation, never goes to the ViewModel.
    var isVisible by rememberSaveable { mutableStateOf(false) }

    AppTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        leadingIcon = painterResource(AppIcons.Lock),
        helperText = helperText,
        status = status,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        keyboardActions = keyboardActions,
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { isVisible = !isVisible }) {
                Icon(
                    painter = painterResource(if (isVisible) AppIcons.EyeOff else AppIcons.Eye),
                    contentDescription = stringResource(
                        if (isVisible) R.string.cd_hide_password else R.string.cd_show_password
                    ),
                    tint = AppTheme.colors.textTertiary,
                    modifier = Modifier.size(AppDimens.icon),
                )
            }
        },
    )
}
```

> **Icon note.** Figma `03b → Password field`: "eye toggles to eye-off when the password is visible". Both are on the Figma icon sheet now (`AppIcons.Eye`, `AppIcons.EyeOff`, doc 09). `IconButton` gives the 48 dp touch target, and the visible icon stays 20 dp.

### `AppPasswordStrength.kt`

In sign-up (Figma 02.3) the password field is followed by **4 segments** and a helper line: "Strong · 8+ characters, a number and a symbol".

```kotlin
package com.example.dukkanapp.core.designsystem.components.textfield

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.dukkanapp.R
import com.example.dukkanapp.core.designsystem.theme.AppDimens
import com.example.dukkanapp.core.designsystem.theme.AppPillShape
import com.example.dukkanapp.core.designsystem.theme.AppTheme

enum class PasswordStrength(val segments: Int, @StringRes val labelRes: Int) {
    Weak(1, R.string.password_strength_weak),
    Fair(2, R.string.password_strength_fair),
    Good(3, R.string.password_strength_good),
    Strong(4, R.string.password_strength_strong);

    companion object {
        /** UI hint only. The real rule stays in AuthFormValidator.validateNewPassword. */
        fun of(password: String): PasswordStrength {
            var score = 0
            if (password.length >= 8) score++
            if (password.any(Char::isDigit)) score++
            if (password.any { !it.isLetterOrDigit() }) score++
            if (password.any(Char::isUpperCase) && password.any(Char::isLowerCase)) score++
            return entries[(score - 1).coerceAtLeast(0)]
        }
    }
}

@Composable
fun AppPasswordStrength(password: String, modifier: Modifier = Modifier) {
    if (password.isEmpty()) return
    val strength = PasswordStrength.of(password)
    val colors = AppTheme.colors
    val activeColor by animateColorAsState(
        targetValue = when (strength) {
            PasswordStrength.Weak -> colors.error
            PasswordStrength.Fair -> colors.warning
            PasswordStrength.Good, PasswordStrength.Strong -> colors.success
        },
        label = "strengthColor",
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.space4)) {
            repeat(PasswordStrength.entries.size) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(
                            color = if (index < strength.segments) activeColor else colors.border,
                            shape = AppPillShape,
                        ),
                )
            }
        }
        Text(
            text = stringResource(R.string.password_strength_format, stringResource(strength.labelRes)),
            style = AppTheme.textStyles.helper,
            color = activeColor,
            modifier = Modifier.padding(top = AppDimens.space8),
        )
    }
}
```

The 4 dp segment height is drawn once, here, so it stays local. Strings to add: see the table in [14](14_auth_screens_refactor.md#strings) (`password_strength_*`).

### `AppSearchBar.kt`

Figma has two variants: `Search/elevated` (on the home aurora, 52 dp, `card`, `shadows.float`) and `Search/field` (inside pages, 48 dp, `field`, no shadow). Both use a leading `search` icon in `textTertiary`, text at 14 Medium, and an optional brand filter button on the end (home header). Like every input, the search bar is a **pill** (radius/full).

```kotlin
package com.example.dukkanapp.core.designsystem.components.textfield

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.dukkanapp.core.designsystem.components.button.AppIconButton
import com.example.dukkanapp.core.designsystem.components.button.AppIconButtonVariant
import com.example.dukkanapp.core.designsystem.icon.AppIcons
import com.example.dukkanapp.core.designsystem.theme.AppDimens
import com.example.dukkanapp.core.designsystem.theme.AppPillShape
import com.example.dukkanapp.core.designsystem.theme.AppTheme
import com.example.dukkanapp.core.designsystem.theme.appShadow

enum class AppSearchBarVariant { Elevated, Field }

@Composable
fun AppSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    variant: AppSearchBarVariant = AppSearchBarVariant.Field,
    onSearch: () -> Unit = {},
    onFilterClick: (() -> Unit)? = null,
    filterContentDescription: String? = null,
) {
    val colors = AppTheme.colors
    val shape = AppPillShape
    val isElevated = variant == AppSearchBarVariant.Elevated
    val textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.textPrimary)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(if (isElevated) AppDimens.controlLg else 48.dp)
            .then(if (isElevated) Modifier.appShadow(AppTheme.shadows.float, shape) else Modifier)
            .background(if (isElevated) colors.card else colors.field, shape)
            .padding(start = AppDimens.space20, end = AppDimens.space8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(AppIcons.Search),
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(AppDimens.icon),
        )
        Spacer(Modifier.width(AppDimens.space8))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = textStyle,
            cursorBrush = SolidColor(colors.brand),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(placeholder, style = textStyle.copy(color = colors.textTertiary), maxLines = 1)
                    }
                    inner()
                }
            },
        )
        if (onFilterClick != null) {
            AppIconButton(
                icon = painterResource(AppIcons.SlidersHorizontal),
                contentDescription = filterContentDescription,
                onClick = onFilterClick,
                variant = AppIconButtonVariant.Brand,
                size = AppDimens.controlSm,
            )
        }
    }
}
```

The field-variant height (48) is the only size not in `AppDimens`. It equals `touchTargetMin`, so you may write `AppDimens.touchTargetMin` instead.

### `AppOtpField.kt`

This is the 4-digit delivery code (team app C02.4 "Enter delivery code", and later phone OTP). Each box is 52 × 58, radius **`radiusLg` (16)** (Figma radius table: "OTP box → lg"), `card` fill, `border` 1.5 dp, and the digit is 22 sp ExtraBold. The focused box gets a `brand` border, and on error every box gets `error`.

```kotlin
package com.example.dukkanapp.core.designsystem.components.textfield

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dukkanapp.core.designsystem.theme.AppDimens
import com.example.dukkanapp.core.designsystem.theme.AppTheme

@Composable
fun AppOtpField(
    code: String,
    onCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 4,
    isError: Boolean = false,
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(AppDimens.radiusLg)
    val digitStyle = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp, color = colors.textPrimary)

    // Codes are read left to right in every language, including Arabic.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BasicTextField(
            value = code,
            onValueChange = { input -> onCodeChange(input.filter(Char::isDigit).take(length)) },
            modifier = modifier,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            decorationBox = {
                Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.space12)) {
                    repeat(length) { index ->
                        val isCurrent = index == code.length
                        val borderColor = when {
                            isError -> colors.error
                            isCurrent -> colors.brand
                            else -> colors.border
                        }
                        Box(
                            modifier = Modifier
                                .size(width = AppDimens.controlLg, height = 58.dp)
                                .background(colors.card, shape)
                                .border(AppDimens.strokeFocus, borderColor, shape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = code.getOrNull(index)?.toString() ?: "", style = digitStyle)
                        }
                    }
                }
            },
        )
    }
}
```

---

## Notes and rules

- **What the ViewModel sends:** the value, the error `AppUiText?` and (on sign-up) whether the field is valid. The screen maps these to a status:

  ```kotlin
  status = when {
      state.emailError != null -> AppFieldStatus.Error
      state.isValidEmail -> AppFieldStatus.Success
      else -> AppFieldStatus.Default
  }
  ```

  On **login** don't show Success. Figma never turns login fields green, and old accounts may predate the password rule.
- **Keyboard:** pass `KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)` for email. Use `ImeAction.Done` plus a submit in `keyboardActions` on the last field.
- **Autofill (recommended):** add `Modifier.semantics { contentType = ContentType.EmailAddress }` (and `ContentType.Password` / `ContentType.NewPassword`) at the call site. Google Password Manager then offers saved credentials. This is available from Compose UI 1.8, so the BOM 2026 has it.
- **The ring is drawn, not laid out.** `drawBehind` paints 3 dp outside the box, so leave at least `space4` around fields (screens already use 16–20 dp). Don't put a field inside a parent that `clip`s tightly, or the ring gets cut.
- **Disabled uses tokens, not alpha** (Figma `03b`): `field` fill, `textTertiary` label, value and icon, no border, no ring. With alpha, the dark `field` (`#1A2540`) would blend into the background and the field would almost disappear.

---

## Check

- [ ] The field is a pill (fully round ends), 52 dp tall, like the buttons.
- [ ] Tap the email field: the fill turns white, a blue 1.5 dp border and a soft blue pill ring appear, and the mail icon turns blue. Nothing below moves.
- [ ] Disabled field: grey fill, grey label/value/icon, no border, no ring, fully opaque (also readable in dark mode).
- [ ] Type an invalid email and submit: the field turns red-soft with a red border, and "Enter a valid email address" appears with the alert icon.
- [ ] Sign-up valid email: white field, green border, "Looks good" with a check icon.
- [ ] The password eye toggles between hidden and visible. TalkBack reads "Show password" / "Hide password".
- [ ] Font scale 200 %: text isn't clipped. The box stays 52 dp and the text scrolls horizontally.
- [ ] Arabic: the label is right-aligned, the icon is on the right and the eye is on the left. The OTP boxes still read left to right.
- [ ] `grep -rn "OutlinedTextField" app/src/main/java` returns nothing.
