# 14: Refactor the auth screens to the Figma design

This doc rebuilds **Welcome (AuthOptions)**, **Log in**, **Sign up** and **Email verification** (Figma 02.6 / 02.7) with full code, and gives the layout and changes for **Forgot password** and **All set**.

All auth titles are **24 ExtraBold** (`headlineLarge`) in Figma now, and all fields are **pills** (doc 11). For RTL, the reference screen is Figma `13.4 · Log in · Arabic (RTL)`; for dark mode, `13.2 · Log in · dark`.

It changes only the **presentation layer** plus a few validation additions. The Firebase wiring stays exactly as in [auth_feature_implementation_guide.md](../../auth/auth_feature_implementation_guide.md). Where this doc changes a use-case signature (full name, marketing opt-in), it says which files to touch.

**Before you start:** docs 00–09 (tokens and theme) build, and docs 10, 11, 12 (12.1, 12.9, 12.12) and 13 are typed.

---

## 0. Decisions to confirm before typing

| # | Figma says | Current code / auth guide | Recommendation |
|---|---|---|---|
| 1 | Welcome offers **Google + Facebook** + email | **Google + Apple** + email | **Decided: follow Figma, Google + Facebook.** Apple is dropped (delete `ic_apple.xml` and the Apple strings). Both social logins stay **TODO** (as in the auth guide). Facebook needs a Meta developer app and the Facebook SDK. |
| 2 | Sign up has **no Confirm password** field. It shows an eye toggle plus a strength meter | `confirmPassword` + `validateConfirmPassword` | Follow Figma. The eye toggle removes typos, and fewer fields means more sign-ups. Delete the confirm-password state, intent and validator (listed in §4). |
| 3 | Sign up has **no "Keep me logged in"** | `SignUpUiState.rememberMe` | New accounts are remembered (`rememberMe = true` sent to the use case). Only Log in shows the checkbox. |
| 4 | Sign up adds **Full name**, **Terms (required)** and **"Send me deals by email"** | not present | Add them. The full name becomes `displayName` (Auth profile and `users/{uid}`). The opt-in becomes `users/{uid}.marketingNotificationsEnabled`. |
| 5 | Sign up shows **"1/2"** | the flow is Sign up, then Email verification | Step 1 = Sign up, step 2 = Email verification. After verification, show **All set** once, then Home. |

The code below follows the recommendations. If you choose otherwise, only the parts marked **(Decision n)** change.

---

## 1. Refactor order

1. **Assets:** add the Lucide icons you need as vector drawables:
   - `mail`, `lock`, `eye`, `eye-off`, `arrow-left`, `arrow-right`, `check`, `circle-check`, `circle-alert`, `info`, `x`, `user-round`, `user-plus`
   - `clock`, `banknote`, `refresh-cw`, `gift`, `map-pin`, `search`, `sliders-horizontal`, `plus`, `minus`, `star`, `chevron-right`

   Register them in `AppIcons` (`core.designsystem.icon`, doc 08), which already exposes `EyeOff`, `Google` and `Facebook` as `@DrawableRes Int` ids. The Dukkan mark comes from `AppLogoMark` (doc 09).
2. **Components:** type doc 10 → 11 → 12 (12.1, 12.9, 12.12) → 13.
3. **Shared auth bits (§2):** `AuthFormColumn`, `legalText`, `AppLinks`.
4. **Screens, one per commit, building after each:**
   1. Welcome (§3)
   2. Log in (§4)
   3. Sign up (§5)
   4. Forgot password (§6)
   5. Email verification (§7, full code)
   6. All set (§8)
5. **Strings (§9):** add or update them in `values`, `values-ar` and `values-fr` together.
6. **Delete old code (§10)**, but only after the grep in §10 returns nothing.

---

## 2. Shared auth pieces

### `core/utils/constants/AppLinks.kt`

```kotlin
package com.example.dukkanapp.core.utils.constants

/** Public web pages opened from the app. Replace with the real URLs before release. */
object AppLinks {
    const val TERMS = "https://dukkan.tn/terms"
    const val PRIVACY = "https://dukkan.tn/privacy"
}
```

### `features/auth/presentation/components/common/AuthFormColumn.kt`

Every auth screen needs the same layout: **scrollable** (small phones, large fonts, keyboard open), yet with the footer **pinned to the bottom** when there is room. The trick: give the scrolling column a *minimum* height equal to the viewport. `Spacer(Modifier.weight(1f))` then fills the free space, and when the content is taller, it simply scrolls.

```kotlin
package com.example.dukkanapp.features.auth.presentation.components.common

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.dukkanapp.core.designsystem.theme.AppDimens
import com.example.dukkanapp.core.utils.extension.clearFocusOnTap

@Composable
fun AuthFormColumn(
    padding: PaddingValues,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(padding)
            .clearFocusOnTap(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(horizontal = AppDimens.screenPadding)
                .padding(bottom = AppDimens.space16),
            horizontalAlignment = horizontalAlignment,
            content = content,
        )
    }
}
```

### `features/auth/presentation/components/common/LegalText.kt`

"By continuing you accept the **Terms** and **Privacy Policy**" and "I agree to the **Terms** and **Privacy Policy**": two words are links inside a translated sentence. We use `LinkAnnotation.Clickable` (Compose 1.7+), so TalkBack also lists them as links.

```kotlin
package com.example.dukkanapp.features.auth.presentation.components.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.example.dukkanapp.R
import com.example.dukkanapp.core.designsystem.theme.AppTheme

/**
 * Builds "<template with %1$s = Terms, %2$s = Privacy Policy>" where both
 * placeholders are tappable brand-colored links.
 */
@Composable
fun legalText(
    @StringRes templateRes: Int,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
): AnnotatedString {
    val terms = stringResource(R.string.auth_terms)
    val privacy = stringResource(R.string.auth_privacy)
    val sentence = stringResource(templateRes, terms, privacy)
    val linkStyles = TextLinkStyles(style = SpanStyle(color = AppTheme.colors.brand, fontWeight = FontWeight.Bold))

    return buildAnnotatedString {
        append(sentence)
        val termsStart = sentence.indexOf(terms)
        if (termsStart >= 0) {
            addLink(LinkAnnotation.Clickable("terms", linkStyles) { onTermsClick() }, termsStart, termsStart + terms.length)
        }
        val privacyStart = sentence.indexOf(privacy)
        if (privacyStart >= 0) {
            addLink(LinkAnnotation.Clickable("privacy", linkStyles) { onPrivacyClick() }, privacyStart, privacyStart + privacy.length)
        }
    }
}
```

---

## 3. Welcome (Figma 02.1), our `AuthOptionsScreen`

### Layout (393 × 852 frame, measured)

| y | Element | Component and tokens |
|---|---|---|
| 0–520 | aurora tall | `AppScaffold(background = AuroraTall)` |
| ~150 | Dukkan mark | `AppAuthHeader(logo = Mark(56.dp))`, centered |
| 222 | "Welcome to Dukkan" | `headlineLarge`, `textPrimary` |
| 263 | subtitle (2 lines) | `bodySmall`, `textSecondary`, centered |
| 322 | perks: 30 min · Cash on delivery · Easy returns | 14 dp icon `brand` + `helper` in `textLabel` |
| 446 | Continue with Google | `AppSocialButton`, full width |
| 510 | Continue with Facebook | `AppSocialButton`, gap `space12` **(Decision 1)** |
| 582 | "or" | `AppOrDivider`, gap `space16` above and below |
| 618 | Continue with email | `AppButton(Primary)`, leading `mail` |
| 689 | terms notice | `caption`, `textSecondary`, links `brand` |
| 777 | Already have an account? **Log in** | `AppAuthFooterLink` |

### `AuthOptionsScreenContent.kt` (rewrite)

```kotlin
package com.example.dukkanapp.features.auth.presentation.components.authoptions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dukkanapp.R
import com.example.dukkanapp.core.designsystem.components.button.AppButton
import com.example.dukkanapp.core.designsystem.components.button.AppSocialButton
import com.example.dukkanapp.core.designsystem.components.layout.AppScaffold
import com.example.dukkanapp.core.designsystem.theme.AppDimens
import com.example.dukkanapp.core.designsystem.icon.AppIcons
import com.example.dukkanapp.core.designsystem.components.layout.AppScreenBackground
import com.example.dukkanapp.core.designsystem.theme.AppTheme
import com.example.dukkanapp.features.auth.presentation.components.common.AppAuthFooterLink
import com.example.dukkanapp.features.auth.presentation.components.common.AppAuthHeader
import com.example.dukkanapp.features.auth.presentation.components.common.AppOrDivider
import com.example.dukkanapp.features.auth.presentation.components.common.AuthFormColumn
import com.example.dukkanapp.features.auth.presentation.components.common.AuthHeaderLogo
import com.example.dukkanapp.features.auth.presentation.components.common.legalText

@Composable
fun AuthOptionsScreenContent(
    onGoogleClick: () -> Unit,
    onFacebookClick: () -> Unit, // (Decision 1) was onAppleClick
    onEmailClick: () -> Unit,
    onAlreadyHaveAccountClick: () -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
) {
    AppScaffold(background = AppScreenBackground.AuroraTall) { padding ->
        AuthFormColumn(padding = padding, horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(AppDimens.space64))
            AppAuthHeader(
                title = stringResource(R.string.auth_welcome_title),
                subtitle = stringResource(R.string.auth_welcome_subtitle),
                logo = AuthHeaderLogo.Mark(size = 56.dp),
            )
            Spacer(Modifier.height(AppDimens.space16))
            AuthPerksRow()

            Spacer(Modifier.weight(1f).heightIn(min = AppDimens.space32))

            AppSocialButton(
                text = stringResource(R.string.auth_continue_google),
                logo = painterResource(AppIcons.Google),
                onClick = onGoogleClick,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(AppDimens.space12))
            AppSocialButton(
                text = stringResource(R.string.auth_continue_facebook),
                logo = painterResource(AppIcons.Facebook),
                onClick = onFacebookClick,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(AppDimens.space16))
            AppOrDivider(text = stringResource(R.string.auth_or))
            Spacer(Modifier.height(AppDimens.space16))
            AppButton(
                text = stringResource(R.string.auth_continue_email),
                onClick = onEmailClick,
                leadingIcon = painterResource(AppIcons.Mail),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(AppDimens.space16))
            Text(
                text = legalText(R.string.auth_terms_notice, onTermsClick, onPrivacyClick),
                style = AppTheme.textStyles.caption,
                color = AppTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.weight(1f).heightIn(min = AppDimens.space24))

            AppAuthFooterLink(
                promptText = stringResource(R.string.auth_already_have_account),
                actionText = stringResource(R.string.action_log_in),
                onActionClick = onAlreadyHaveAccountClick,
            )
        }
    }
}

@Composable
private fun AuthPerksRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.space16)) {
        AuthPerk(icon = AppIcons.Clock, text = stringResource(R.string.auth_perk_fast))
        AuthPerk(icon = AppIcons.Banknote, text = stringResource(R.string.auth_perk_cash))
        AuthPerk(icon = AppIcons.RefreshCw, text = stringResource(R.string.auth_perk_returns))
    }
}

@Composable
private fun AuthPerk(icon: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppDimens.space4)) {
        Icon(painterResource(icon), null, tint = AppTheme.colors.brand, modifier = Modifier.size(14.dp))
        Text(text, style = AppTheme.textStyles.helper, color = AppTheme.colors.textLabel)
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun AuthOptionsPreview() {
    AppTheme {
        AuthOptionsScreenContent({}, {}, {}, {}, {}, {})
    }
}
```

Add `import androidx.compose.foundation.layout.heightIn`.

### `AuthOptionsScreen.kt` changes

- Rename `onAppleClick` to `onFacebookClick` (still a `// TODO: Facebook sign-in`).
- Open the legal pages with `LocalUriHandler`. It's a pure navigation side effect, so this screen doesn't need a ViewModel:

```kotlin
val uriHandler = LocalUriHandler.current
AuthOptionsScreenContent(
    onGoogleClick = { /* TODO: Google sign-in (auth guide §15) */ },
    onFacebookClick = { /* TODO: Facebook sign-in */ },
    onEmailClick = onNavigateToSignUp,
    onAlreadyHaveAccountClick = onNavigateToLogin,
    onTermsClick = { uriHandler.openUri(AppLinks.TERMS) },
    onPrivacyClick = { uriHandler.openUri(AppLinks.PRIVACY) },
)
```

`AuthOptionsSection.kt` is no longer used, so delete it.

---

## 4. Log in (Figma 02.2)

### Layout

| y | Element | Component |
|---|---|---|
| 0–300 | aurora short | `AppScaffold(background = AuroraShort)` |
| 58 | back | `onNavigateBack` gives a 44 dp White icon button |
| ~120 | mark 48 | `AppAuthHeader(logo = Mark(AppDimens.logoMark))` |
| 175 / 211 | "Welcome back" / subtitle | centered |
| 254 → 276 | Email label → 52 dp field (focused in Figma, with a clear "x") | `AppTextField` |
| 346 → 368 | Password | `AppPasswordField` |
| 439 | Keep me logged in · Forgot password? | `AppCheckbox` + `AppButton(Ghost, Small)` |
| 484 | Log in → | `AppButton(Primary)` + trailing `arrow-right` |
| 566 | "or continue with" | `AppOrDivider` |
| 600 | Google · Facebook (170 × 52 each, gap 13) | two `AppSocialButton` with `weight(1f)` |
| 777 | New to Dukkan? **Create account** | `AppAuthFooterLink` |

Gaps: header to first label `space24`, between field groups `space16`, password to remember row `space4` (the row is 48 dp tall), remember row to button `space12`, button to "or" `space24`, "or" to social `space16`.

### State and intent changes

```kotlin
// LoginUiState: add if you haven't from the auth guide
val generalError: AppUiText? = null,

// LoginIntent: add
data object GoogleClicked : LoginIntent   // TODO: Google sign-in
data object FacebookClicked : LoginIntent // TODO (Decision 1)
```

In `LoginViewModel.onIntent`, handle both with `Unit // TODO`. The clear "x" sends `LoginIntent.EmailChanged("")`, so it needs no new intent.

### `LoginScreenContent.kt` (rewrite)

```kotlin
package com.example.dukkanapp.features.auth.presentation.components.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.example.dukkanapp.R
import com.example.dukkanapp.core.common.text.asString
import com.example.dukkanapp.core.designsystem.components.button.AppButton
import com.example.dukkanapp.core.designsystem.components.button.AppButtonSize
import com.example.dukkanapp.core.designsystem.components.button.AppButtonVariant
import com.example.dukkanapp.core.designsystem.components.button.AppSocialButton
import com.example.dukkanapp.core.designsystem.components.feedback.AppMessageBanner
import com.example.dukkanapp.core.designsystem.components.layout.AppScaffold
import com.example.dukkanapp.core.designsystem.components.selection.AppCheckbox
import com.example.dukkanapp.core.designsystem.components.textfield.AppFieldStatus
import com.example.dukkanapp.core.designsystem.components.textfield.AppPasswordField
import com.example.dukkanapp.core.designsystem.components.textfield.AppTextField
import com.example.dukkanapp.core.designsystem.theme.AppDimens
import com.example.dukkanapp.core.designsystem.icon.AppIcons
import com.example.dukkanapp.core.designsystem.components.layout.AppScreenBackground
import com.example.dukkanapp.core.designsystem.theme.AppTheme
import com.example.dukkanapp.features.auth.presentation.components.common.AppAuthFooterLink
import com.example.dukkanapp.features.auth.presentation.components.common.AppAuthHeader
import com.example.dukkanapp.features.auth.presentation.components.common.AppOrDivider
import com.example.dukkanapp.features.auth.presentation.components.common.AuthFormColumn
import com.example.dukkanapp.features.auth.presentation.components.common.AuthHeaderLogo
import com.example.dukkanapp.features.auth.presentation.logic.login.LoginIntent
import com.example.dukkanapp.features.auth.presentation.logic.login.LoginUiState

@Composable
fun LoginScreenContent(
    state: LoginUiState,
    onIntent: (LoginIntent) -> Unit,
) {
    val focusManager = LocalFocusManager.current

    AppScaffold(
        background = AppScreenBackground.AuroraShort,
        onNavigateBack = { onIntent(LoginIntent.BackClicked) },
    ) { padding ->
        AuthFormColumn(padding = padding) {
            Spacer(Modifier.height(AppDimens.space8))
            AppAuthHeader(
                title = stringResource(R.string.login_title),
                subtitle = stringResource(R.string.login_subtitle),
                logo = AuthHeaderLogo.Mark(size = AppDimens.logoMark),
            )
            Spacer(Modifier.height(AppDimens.space24))

            state.generalError?.let { error ->
                AppMessageBanner(message = error.asString())
                Spacer(Modifier.height(AppDimens.space16))
            }

            AppTextField(
                value = state.email,
                onValueChange = { onIntent(LoginIntent.EmailChanged(it)) },
                label = stringResource(R.string.label_email),
                placeholder = stringResource(R.string.auth_email_placeholder),
                leadingIcon = painterResource(AppIcons.Mail),
                helperText = state.emailError?.asString(),
                // Login never shows green: no Success status here.
                status = if (state.emailError != null) AppFieldStatus.Error else AppFieldStatus.Default,
                enabled = !state.isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                trailingIcon = if (state.email.isNotEmpty()) {
                    {
                        IconButton(onClick = { onIntent(LoginIntent.EmailChanged("")) }) {
                            Icon(
                                painter = painterResource(AppIcons.X),
                                contentDescription = stringResource(R.string.cd_clear_text),
                                tint = AppTheme.colors.textTertiary,
                                modifier = Modifier.size(AppDimens.iconSm),
                            )
                        }
                    }
                } else {
                    null
                },
            )
            Spacer(Modifier.height(AppDimens.space16))
            AppPasswordField(
                value = state.password,
                onValueChange = { onIntent(LoginIntent.PasswordChanged(it)) },
                label = stringResource(R.string.label_password),
                placeholder = stringResource(R.string.auth_password_placeholder),
                helperText = state.passwordError?.asString(),
                status = if (state.passwordError != null) AppFieldStatus.Error else AppFieldStatus.Default,
                enabled = !state.isLoading,
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onIntent(LoginIntent.LoginClicked)
                    }
                ),
            )
            Spacer(Modifier.height(AppDimens.space4))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                AppCheckbox(
                    checked = state.rememberMe,
                    onCheckedChange = { onIntent(LoginIntent.RememberMeChanged(it)) },
                    label = stringResource(R.string.login_remember_me),
                )
                AppButton(
                    text = stringResource(R.string.login_forgot_password),
                    onClick = { onIntent(LoginIntent.ForgotPasswordClicked) },
                    variant = AppButtonVariant.Ghost,
                    size = AppButtonSize.Small,
                )
            }
            Spacer(Modifier.height(AppDimens.space12))
            AppButton(
                text = stringResource(R.string.action_log_in),
                onClick = {
                    focusManager.clearFocus()
                    onIntent(LoginIntent.LoginClicked)
                },
                trailingIcon = painterResource(AppIcons.ArrowRight),
                isLoading = state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(AppDimens.space24))
            AppOrDivider(text = stringResource(R.string.auth_or_continue_with))
            Spacer(Modifier.height(AppDimens.space16))
            Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.space12)) {
                AppSocialButton(
                    text = stringResource(R.string.auth_google),
                    logo = painterResource(AppIcons.Google),
                    onClick = { onIntent(LoginIntent.GoogleClicked) },
                    modifier = Modifier.weight(1f),
                )
                AppSocialButton(
                    text = stringResource(R.string.auth_facebook),
                    logo = painterResource(AppIcons.Facebook),
                    onClick = { onIntent(LoginIntent.FacebookClicked) },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.weight(1f).heightIn(min = AppDimens.space24))

            AppAuthFooterLink(
                promptText = stringResource(R.string.login_no_account),
                actionText = stringResource(R.string.login_sign_up),
                onActionClick = { onIntent(LoginIntent.SignUpClicked) },
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun LoginPreview() {
    AppTheme {
        LoginScreenContent(state = LoginUiState(email = "amira.bensalah@gmail.com"), onIntent = {})
    }
}
```

What disappeared compared with today:

- the `iconsax` icons (`Iconsax.Linear.Sms`, `PasswordCheck`) are replaced by `AppIcons`
- `AppTextFiled`, `AppPrimaryButton`, `AppTextButton` and `AppDivider` are replaced by the new components
- `isSuccess = state.isValidEmail` is gone, because login doesn't show green

---

## 5. Sign up (Figma 02.3)

### Layout

| y | Element | Component |
|---|---|---|
| 0 | plain background | `AppScaffold(background = Plain)` |
| 58 | back · progress bar · "1/2" | `onNavigateBack` + `topBarActions = { AppProgressBar(0.5f, weight 1f); Text("1/2") }` |
| 124 | 48 dp tile with `user-plus` | `AppAuthHeader(logo = Tile(AppIcons.UserPlus))`, start aligned |
| 190 / 225 | "Create your account" / "It's free and takes less than a minute." | |
| 266 | Full name (`user-round`) | `AppTextField` |
| 356 | Email (`mail`), green + "Looks good" when valid | `AppTextField`, Success |
| 466 | Password + strength bar + "Strong · 8+ characters, a number and a symbol" | `AppPasswordField` + `AppPasswordStrength` |
| 603 | ☑ I agree to the **Terms** and **Privacy Policy** (required) | `AppCheckbox(AnnotatedString)` |
| 635 | ☐ Send me deals by email | `AppCheckbox` |
| 688 | Create account → | `AppButton(Primary)` |
| 777 | Already have an account? **Log in** | `AppAuthFooterLink` |

### Validation additions (domain)

`core/error/validation/ValidationError.kt`: add

```kotlin
data object FullNameEmpty : ValidationError
data object TermsNotAccepted : ValidationError
```

and map them in `core/error/ui/ValidationErrorUiText.kt`:

```kotlin
ValidationError.FullNameEmpty -> AppUiText.Res(R.string.error_full_name_empty)
ValidationError.TermsNotAccepted -> AppUiText.Res(R.string.error_terms_not_accepted)
```

`features/auth/domain/validation/AuthFormValidator.kt`: add these, and **delete `validateConfirmPassword` (Decision 2)**:

```kotlin
fun validateFullName(fullName: String): ValidationError? =
    if (fullName.isBlank()) ValidationError.FullNameEmpty else null

fun validateTermsAccepted(accepted: Boolean): ValidationError? =
    if (accepted) null else ValidationError.TermsNotAccepted
```

Remove `ValidationError.ConfirmPasswordEmpty` and `PasswordsMismatch` (and their strings) if nothing else uses them.

### `SignUpUiState.kt` (rewrite)

```kotlin
package com.example.dukkanapp.features.auth.presentation.logic.signup

import com.example.dukkanapp.core.common.text.AppUiText
import com.example.dukkanapp.core.utils.extension.isValidEmail
import com.example.dukkanapp.core.utils.extension.isValidPassword

data class SignUpUiState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val acceptedTerms: Boolean = false,
    val marketingOptIn: Boolean = false,
    val fullNameError: AppUiText? = null,
    val emailError: AppUiText? = null,
    val passwordError: AppUiText? = null,
    val termsError: AppUiText? = null,
    val generalError: AppUiText? = null,
    val isLoading: Boolean = false,
) {
    val isValidEmail: Boolean get() = emailError == null && email.trim().isValidEmail()
    val isValidPassword: Boolean get() = passwordError == null && password.isValidPassword()
}
```

### `SignUpIntent.kt` (rewrite)

```kotlin
package com.example.dukkanapp.features.auth.presentation.logic.signup

sealed interface SignUpIntent {
    data class FullNameChanged(val fullName: String) : SignUpIntent
    data class EmailChanged(val email: String) : SignUpIntent
    data class PasswordChanged(val password: String) : SignUpIntent
    data class AcceptTermsChanged(val checked: Boolean) : SignUpIntent
    data class MarketingOptInChanged(val checked: Boolean) : SignUpIntent

    data object TermsClicked : SignUpIntent
    data object PrivacyClicked : SignUpIntent
    data object SignUpClicked : SignUpIntent
    data object LoginClicked : SignUpIntent
    data object BackClicked : SignUpIntent
}
```

### `SignUpEffect.kt`: add

```kotlin
data class OpenUrl(val url: String) : SignUpEffect
```

### `SignUpViewModel.kt`: the changed parts

```kotlin
override fun onIntent(intent: SignUpIntent) {
    when (intent) {
        is SignUpIntent.FullNameChanged -> setState { copy(fullName = intent.fullName, fullNameError = null) }
        is SignUpIntent.EmailChanged -> setState { copy(email = intent.email, emailError = null, generalError = null) }
        is SignUpIntent.PasswordChanged -> setState { copy(password = intent.password, passwordError = null) }
        is SignUpIntent.AcceptTermsChanged -> setState { copy(acceptedTerms = intent.checked, termsError = null) }
        is SignUpIntent.MarketingOptInChanged -> setState { copy(marketingOptIn = intent.checked) }

        SignUpIntent.TermsClicked -> sendEffect(SignUpEffect.OpenUrl(AppLinks.TERMS))
        SignUpIntent.PrivacyClicked -> sendEffect(SignUpEffect.OpenUrl(AppLinks.PRIVACY))
        SignUpIntent.SignUpClicked -> onSignUpClicked()
        SignUpIntent.LoginClicked -> sendEffect(SignUpEffect.NavigateToLogin)
        SignUpIntent.BackClicked -> sendEffect(SignUpEffect.NavigateBack)
    }
}

private fun onSignUpClicked() {
    val current = currentState
    if (current.isLoading) return

    val fullName = current.fullName.trim()
    val email = current.email.trim()
    val password = current.password

    val fullNameError = AuthFormValidator.validateFullName(fullName)
    val emailError = AuthFormValidator.validateEmail(email)
    val passwordError = AuthFormValidator.validateNewPassword(password)
    val termsError = AuthFormValidator.validateTermsAccepted(current.acceptedTerms)

    if (listOf(fullNameError, emailError, passwordError, termsError).any { it != null }) {
        setState {
            copy(
                fullNameError = fullNameError?.toUiText(),
                emailError = emailError?.toUiText(),
                passwordError = passwordError?.toUiText(),
                termsError = termsError?.toUiText(),
            )
        }
        return
    }

    setState { copy(isLoading = true, generalError = null) }
    viewModelScope.launch {
        // Auth guide's SignUpWithEmailUseCase, extended (see below).
        signUpWithEmail(
            email = email,
            password = password,
            fullName = fullName,
            marketingOptIn = current.marketingOptIn,
            rememberMe = true, // (Decision 3)
        )
            .onSuccess { sendEffect(SignUpEffect.NavigateToEmailVerification(emailJustSent = true)) }
            .onFailure { failure -> setState { copy(generalError = failure.toUiText()) } }
        setState { copy(isLoading = false) }
    }
}
```

Delete `liveMismatchError` (Decision 2). Use the `onSuccess`/`onFailure` helpers your `AppResult` already has from the auth guide. If yours are named differently, keep their names.

**Use case and data changes (auth guide files) for Decision 4:**

| File | Change |
|---|---|
| `SignUpWithEmailUseCase` | add `fullName: String` and `marketingOptIn: Boolean` parameters |
| `AuthRepository.signUp(...)` / `AuthRemoteDataSource` | after `createUserWithEmailAndPassword`, call `user.updateProfile(userProfileChangeRequest { displayName = fullName }).await()` so `AuthUser.displayName` is set |
| `UserRepository.syncProfile(...)` / `toNewUserDto(...)` | write `displayName = fullName` and `marketingNotificationsEnabled = marketingOptIn` on **create only** (the transaction already only creates once) |
| Firestore rules | no change: both fields are already in the `hasOnly` list |

### Screen effect handling (`SignUpScreen.kt`)

```kotlin
val uriHandler = LocalUriHandler.current
AppCollectEffect(viewModel.effect) { effect ->
    when (effect) {
        is SignUpEffect.OpenUrl -> uriHandler.openUri(effect.url)
        // ...existing navigation effects
    }
}
```

### `SignUpScreenContent.kt` (rewrite)

```kotlin
package com.example.dukkanapp.features.auth.presentation.components.signup

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.example.dukkanapp.R
import com.example.dukkanapp.core.common.text.asString
import com.example.dukkanapp.core.designsystem.components.button.AppButton
import com.example.dukkanapp.core.designsystem.components.display.AppProgressBar
import com.example.dukkanapp.core.designsystem.components.feedback.AppMessageBanner
import com.example.dukkanapp.core.designsystem.components.layout.AppScaffold
import com.example.dukkanapp.core.designsystem.components.selection.AppCheckbox
import com.example.dukkanapp.core.designsystem.components.textfield.AppFieldStatus
import com.example.dukkanapp.core.designsystem.components.textfield.AppPasswordField
import com.example.dukkanapp.core.designsystem.components.textfield.AppPasswordStrength
import com.example.dukkanapp.core.designsystem.components.textfield.AppTextField
import com.example.dukkanapp.core.designsystem.theme.AppDimens
import com.example.dukkanapp.core.designsystem.icon.AppIcons
import com.example.dukkanapp.core.designsystem.theme.AppTheme
import com.example.dukkanapp.features.auth.presentation.components.common.AppAuthFooterLink
import com.example.dukkanapp.features.auth.presentation.components.common.AppAuthHeader
import com.example.dukkanapp.features.auth.presentation.components.common.AuthFormColumn
import com.example.dukkanapp.features.auth.presentation.components.common.AuthHeaderLogo
import com.example.dukkanapp.features.auth.presentation.components.common.legalText
import com.example.dukkanapp.features.auth.presentation.logic.signup.SignUpIntent
import com.example.dukkanapp.features.auth.presentation.logic.signup.SignUpUiState

private const val SIGN_UP_STEP = 1
private const val SIGN_UP_TOTAL_STEPS = 2

@Composable
fun SignUpScreenContent(
    state: SignUpUiState,
    onIntent: (SignUpIntent) -> Unit,
) {
    val focusManager = LocalFocusManager.current

    AppScaffold(
        onNavigateBack = { onIntent(SignUpIntent.BackClicked) },
        topBarActions = {
            AppProgressBar(
                progress = SIGN_UP_STEP.toFloat() / SIGN_UP_TOTAL_STEPS,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.signup_step, SIGN_UP_STEP, SIGN_UP_TOTAL_STEPS),
                style = MaterialTheme.typography.labelSmall,
                color = AppTheme.colors.textSecondary,
            )
        },
    ) { padding ->
        AuthFormColumn(padding = padding) {
            Spacer(Modifier.height(AppDimens.space24))
            AppAuthHeader(
                title = stringResource(R.string.signup_title),
                subtitle = stringResource(R.string.signup_subtitle),
                logo = AuthHeaderLogo.Tile(icon = AppIcons.UserPlus),
            )
            Spacer(Modifier.height(AppDimens.space24))

            state.generalError?.let { error ->
                AppMessageBanner(message = error.asString())
                Spacer(Modifier.height(AppDimens.space16))
            }

            AppTextField(
                value = state.fullName,
                onValueChange = { onIntent(SignUpIntent.FullNameChanged(it)) },
                label = stringResource(R.string.label_full_name),
                placeholder = stringResource(R.string.placeholder_full_name),
                leadingIcon = painterResource(AppIcons.UserRound),
                helperText = state.fullNameError?.asString(),
                status = if (state.fullNameError != null) AppFieldStatus.Error else AppFieldStatus.Default,
                enabled = !state.isLoading,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            )
            Spacer(Modifier.height(AppDimens.space16))
            AppTextField(
                value = state.email,
                onValueChange = { onIntent(SignUpIntent.EmailChanged(it)) },
                label = stringResource(R.string.label_email),
                placeholder = stringResource(R.string.auth_email_placeholder),
                leadingIcon = painterResource(AppIcons.Mail),
                helperText = state.emailError?.asString()
                    ?: if (state.isValidEmail) stringResource(R.string.helper_email_ok) else null,
                status = when {
                    state.emailError != null -> AppFieldStatus.Error
                    state.isValidEmail -> AppFieldStatus.Success
                    else -> AppFieldStatus.Default
                },
                enabled = !state.isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            )
            Spacer(Modifier.height(AppDimens.space16))
            AppPasswordField(
                value = state.password,
                onValueChange = { onIntent(SignUpIntent.PasswordChanged(it)) },
                label = stringResource(R.string.label_password),
                placeholder = stringResource(R.string.auth_password_placeholder),
                helperText = state.passwordError?.asString(),
                status = if (state.passwordError != null) AppFieldStatus.Error else AppFieldStatus.Default,
                enabled = !state.isLoading,
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            )
            if (state.passwordError == null) {
                AppPasswordStrength(
                    password = state.password,
                    modifier = Modifier.padding(top = AppDimens.space12),
                )
            }
            Spacer(Modifier.height(AppDimens.space16))

            AppCheckbox(
                checked = state.acceptedTerms,
                onCheckedChange = { onIntent(SignUpIntent.AcceptTermsChanged(it)) },
                label = legalText(
                    templateRes = R.string.signup_accept_terms,
                    onTermsClick = { onIntent(SignUpIntent.TermsClicked) },
                    onPrivacyClick = { onIntent(SignUpIntent.PrivacyClicked) },
                ),
            )
            state.termsError?.let { error ->
                Text(
                    text = error.asString(),
                    style = AppTheme.textStyles.helper,
                    color = AppTheme.colors.error,
                    modifier = Modifier.padding(start = AppDimens.space32),
                )
            }
            AppCheckbox(
                checked = state.marketingOptIn,
                onCheckedChange = { onIntent(SignUpIntent.MarketingOptInChanged(it)) },
                label = stringResource(R.string.signup_marketing_opt_in),
            )
            Spacer(Modifier.height(AppDimens.space16))
            AppButton(
                text = stringResource(R.string.signup_create_account),
                onClick = {
                    focusManager.clearFocus()
                    onIntent(SignUpIntent.SignUpClicked)
                },
                trailingIcon = painterResource(AppIcons.ArrowRight),
                isLoading = state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.weight(1f).heightIn(min = AppDimens.space24))

            AppAuthFooterLink(
                promptText = stringResource(R.string.auth_already_have_account),
                actionText = stringResource(R.string.action_log_in),
                onActionClick = { onIntent(SignUpIntent.LoginClicked) },
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun SignUpPreview() {
    AppTheme {
        SignUpScreenContent(
            state = SignUpUiState(
                fullName = "Amira Ben Salah",
                email = "amira.bensalah@gmail.com",
                password = "Dukkan#2026",
                acceptedTerms = true,
            ),
            onIntent = {},
        )
    }
}
```

> **Why is the terms checkbox required but not disabling the button?** A disabled "Create account" gives no reason. Keeping it enabled and showing "Please accept the Terms to continue" under the checkbox tells the user what to do. The same rule applies to every form: **validate on submit and say why**.

---

## 6. Forgot password (Figma 02.4)

**Background:** `AuroraShort` + back. **Header:** `Tile(AppIcons.Lock)`, start aligned, "Reset your password" / "We'll email you a secure link to set a new one."

| State | Content |
|---|---|
| Form (`isLinkSent = false`) | Email `AppTextField` (`mail`). `AppButton(Primary)` "Send reset link" |
| Sent (`isLinkSent = true`) | The email field stays (read-only, Default). Below it a **success card**: `successSoft` fill, radius `radiusLg`, padding `space16`, `circle-check` in `success`, title "Check your inbox" (`titleSmall`, `successText`), "Link sent to {email}" + "It expires in 1 hour." (`bodySmall`, `textSecondary`). Then `AppButton(Primary)` "Open email app" with leading `mail`. Then a centered row "Didn't get it? **Resend in 0:42**": a Ghost button that is disabled while the countdown runs, and enabled with the text "Resend" when it reaches 0 |

**State additions:** `isLinkSent: Boolean`, `resendSecondsLeft: Int`. **Intents:** `SendLinkClicked`, `ResendClicked`, `OpenEmailAppClicked`. **Effect:** `OpenEmailApp`.

The countdown is the same 60 s pattern as the auth guide's verification screen. Reuse it: a `Job` in the ViewModel that ticks `resendSecondsLeft` down with `delay(1_000)`.

Open the user's mail app (screen side, because it is an Android `Intent`):

```kotlin
is ForgetPasswordEffect.OpenEmailApp -> {
    val intent = Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_APP_EMAIL)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // No mail app installed: show the info banner instead of crashing.
    }
}
```

`AppAccountRecoveryHeader` and `AppAccountRecoveryFooter` are replaced by `AppAuthHeader(Tile(Lock))` and `AppAuthFooterLink`.

---

## 7. Email verification (Figma 02.6 + 02.7, sign-up step 2/2)

Figma now designs this screen in two states: **02.6 Verify your email** (just sent) and **02.7 Not verified yet** (the user tapped "I've verified" too early). Figma's note: "Firebase `sendEmailVerification()` → `user.reload()` on 'I've verified'; resend unlocks after 60 s." That's exactly the auth guide's `EmailVerificationViewModel` (guide §11), so we keep its logic and add three small things.

### Layout (393 × 852 frame, measured)

| y | Element | Component |
|---|---|---|
| 0–260 | aurora compact | `AppScaffold(background = AuroraCompact)` |
| 58 | back · progress · "2/2" (12 Bold `textSecondary`, at the end) | `onNavigateBack` + `topBarActions = { AppProgressBar(1f, weight 1f); Text("2/2") }` |
| 124 | 48 dp brand tile with white `mail` | `AppAuthHeader(logo = Tile(AppIcons.Mail))`, start aligned |
| 190 / 225 | "Verify your email" (24 ExtraBold) / "We sent a verification link to your inbox." (13 Medium `textSecondary`) | (header) |
| 262 | **Sent-to card** 353 × 60, `field` fill, radius `radiusMd`: 36 dp white icon tile (`mail`), "Sent to" (11 SemiBold `textSecondary`) over the email (14 Bold `textPrimary`), "Change" (12.5 Bold `brand`) at the end | `SentToCard` |
| 342 | **02.6 Steps card** 353 × 184, `card` fill, radius `radiusCard`, `shadows.card`: three rows, each a 28 dp `brandSoft` circle with the number (12.5 ExtraBold `brandSoftText`), a title (13.5 Bold) and a subtitle (11.5 Medium `textSecondary`) | `StepsCard` |
| 350 | **02.7 Not-verified card** 353 × 58, `card`, `shadows.card`: 28 dp `errorSoft` circle with `circle-alert` in `error`, "Your email isn't verified yet" (13 Bold) / "Open the link we sent, then tap I've verified." (11.5 Medium) | `NotVerifiedCard` |
| 428 | **02.7 Help card** 353 × 98, `card`, `shadows.card`: "Can't find it?" (13.5 Bold) / "Check Spam or Promotions, or search “Dukkan”." / "Links expire after 24 hours." (12 Medium `textSecondary`) | `HelpCard` |
| 640 | "Open email app" | `AppButton(Primary, leadingIcon = Mail)` |
| 704 | "I've verified" | `AppButton(Outline, isLoading = state.isChecking)` |
| 777 | "Didn't get it?" (13 Medium `textSecondary`) + "Resend in 0:60" (13 Bold `textTertiary`, while counting) / "Resend link" (13 Bold `brand`, when available) | `ResendRow` |

Figma's own steps: link valid **24 hours**, "We'll take you to Home". Firebase's default verification link lifetime is set in the console; keep the copy in sync if you change it.

### State, intent and effect changes (auth guide §11)

Only additions. Keep everything else in the guide's files as it is.

| Directive / Figma action | Guide name | Change |
|---|---|---|
| Open email app | `OpenEmailAppClicked` | none |
| I've verified | `CheckClicked` | when not verified, set `isNotVerified = true` instead of showing a message banner |
| Resend | `ResendClicked` | also clears `isNotVerified` |
| Change (email) | **new** `ChangeEmailClicked` | signs out, then `NavigateToSignUp` (a typo in the email can only be fixed by signing up again) |
| Back | **new** `BackClicked` | same as `UseAnotherAccountClicked`: sign out, go to Welcome |
| resend countdown | `resendCooldownSeconds` | none (it's the "resendSecondsLeft" of Figma) |

`EmailVerificationUiState.kt`: add one field:

```kotlin
/** Figma 02.7: the user tapped "I've verified" but Firebase still says no. */
val isNotVerified: Boolean = false,
```

`EmailVerificationIntent.kt`: add two objects:

```kotlin
data object ChangeEmailClicked : EmailVerificationIntent
data object BackClicked : EmailVerificationIntent
```

`EmailVerificationEffect.kt`: add one object:

```kotlin
data object NavigateToSignUp : EmailVerificationEffect
```

`EmailVerificationViewModel.kt`: the changed lines only.

```kotlin
// onIntent: add the two new branches
EmailVerificationIntent.ChangeEmailClicked -> signOutAndLeave(toSignUp = true)
EmailVerificationIntent.BackClicked -> signOutAndLeave()

// checkVerification: replace the "!silent -> showMessage(...)" line with
!silent -> setState { copy(isNotVerified = true) }

// resend(): when it starts, clear the 02.7 state
setState { copy(isResending = true, message = null, isNotVerified = false) }

// signOutAndLeave: add the parameter and pick the effect
private fun signOutAndLeave(toSignUp: Boolean = false) {
    if (currentState.isSigningOut) return
    setState { copy(isSigningOut = true) }

    viewModelScope.launch {
        signOut()
        sendEffect(
            if (toSignUp) EmailVerificationEffect.NavigateToSignUp
            else EmailVerificationEffect.NavigateToAuthOptions
        )
    }
}
```

In `EmailVerificationScreen.kt`, handle `NavigateToSignUp` with a new `onNavigateToSignUp` callback, and in `AppNavHost` pass `navController.navigateAsRoot(AppRoute.SignUpScreen)`.

When verification succeeds, navigate to **All set** (§8) instead of Home, but only for a brand-new account (`emailJustSent == true`). A user who signed up long ago and is only verifying now goes straight to Home.

### `EmailVerificationScreenContent.kt` (rewrite)

**File:** `features/auth/presentation/components/emailverification/EmailVerificationScreenContent.kt`

```kotlin
package com.example.dukkanapp.features.auth.presentation.components.emailverification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dukkanapp.R
import com.example.dukkanapp.core.common.text.asString
import com.example.dukkanapp.core.designsystem.components.button.AppButton
import com.example.dukkanapp.core.designsystem.components.button.AppButtonSize
import com.example.dukkanapp.core.designsystem.components.button.AppButtonVariant
import com.example.dukkanapp.core.designsystem.components.display.AppProgressBar
import com.example.dukkanapp.core.designsystem.components.feedback.AppMessageBanner
import com.example.dukkanapp.core.designsystem.components.feedback.AppMessageKind
import com.example.dukkanapp.core.designsystem.components.layout.AppScaffold
import com.example.dukkanapp.core.designsystem.components.layout.AppScreenBackground
import com.example.dukkanapp.core.designsystem.icon.AppIcons
import com.example.dukkanapp.core.designsystem.theme.AppDimens
import com.example.dukkanapp.core.designsystem.theme.AppTheme
import com.example.dukkanapp.core.designsystem.theme.appShadow
import com.example.dukkanapp.features.auth.presentation.components.common.AppAuthHeader
import com.example.dukkanapp.features.auth.presentation.components.common.AuthFormColumn
import com.example.dukkanapp.features.auth.presentation.components.common.AuthHeaderLogo
import com.example.dukkanapp.features.auth.presentation.logic.emailverification.EmailVerificationIntent
import com.example.dukkanapp.features.auth.presentation.logic.emailverification.EmailVerificationUiState

private const val SIGN_UP_STEP = 2
private const val SIGN_UP_TOTAL_STEPS = 2

@Composable
fun EmailVerificationScreenContent(
    state: EmailVerificationUiState,
    onIntent: (EmailVerificationIntent) -> Unit,
) {
    AppScaffold(
        background = AppScreenBackground.AuroraCompact,
        onNavigateBack = { onIntent(EmailVerificationIntent.BackClicked) },
        topBarActions = {
            AppProgressBar(progress = 1f, modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.signup_step, SIGN_UP_STEP, SIGN_UP_TOTAL_STEPS),
                style = MaterialTheme.typography.labelSmall,
                color = AppTheme.colors.textSecondary,
            )
        },
    ) { padding ->
        AuthFormColumn(padding = padding) {
            Spacer(Modifier.height(AppDimens.space16))
            AppAuthHeader(
                title = stringResource(R.string.verify_title),
                subtitle = stringResource(R.string.verify_subtitle),
                logo = AuthHeaderLogo.Tile(AppIcons.Mail),
            )
            Spacer(Modifier.height(AppDimens.space24))

            SentToCard(email = state.email, onChange = { onIntent(EmailVerificationIntent.ChangeEmailClicked) })
            Spacer(Modifier.height(AppDimens.space20))

            if (state.isNotVerified) {
                NotVerifiedCard()
                Spacer(Modifier.height(AppDimens.space20))
                HelpCard()
            } else {
                StepsCard()
            }

            // Success / network errors from the guide's ViewModel ("A new email is on its way").
            state.message?.let { message ->
                Spacer(Modifier.height(AppDimens.space16))
                AppMessageBanner(
                    message = message.text.asString(),
                    kind = if (message.isError) AppMessageKind.Error else AppMessageKind.Success,
                )
            }

            Spacer(Modifier.weight(1f).heightIn(min = AppDimens.space24))

            AppButton(
                text = stringResource(R.string.action_open_email_app),
                onClick = { onIntent(EmailVerificationIntent.OpenEmailAppClicked) },
                leadingIcon = painterResource(AppIcons.Mail),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(AppDimens.space12))
            AppButton(
                text = stringResource(R.string.verify_i_have_verified),
                onClick = { onIntent(EmailVerificationIntent.CheckClicked) },
                variant = AppButtonVariant.Outline,
                isLoading = state.isChecking,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(AppDimens.space16))
            ResendRow(
                secondsLeft = state.resendCooldownSeconds,
                enabled = state.canResend,
                onResend = { onIntent(EmailVerificationIntent.ResendClicked) },
            )
        }
    }
}

/** "Sent to amira@… · Change" on a field-colored card. */
@Composable
private fun SentToCard(email: String, onChange: () -> Unit) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.field, RoundedCornerShape(AppDimens.radiusMd))
            .padding(horizontal = AppDimens.space12, vertical = AppDimens.space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(colors.card, RoundedCornerShape(AppDimens.radiusSm)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(AppIcons.Mail), null, tint = colors.brand, modifier = Modifier.size(AppDimens.iconSm))
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = AppDimens.space12)) {
            Text(stringResource(R.string.verify_sent_to), style = AppTheme.textStyles.caption, color = colors.textSecondary)
            Text(email, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary, maxLines = 1)
        }
        AppButton(
            text = stringResource(R.string.action_change),
            onClick = onChange,
            variant = AppButtonVariant.Ghost,
            size = AppButtonSize.Small,
        )
    }
}

/** A white card with the card shadow (dark mode adds the night-700 outline by itself). */
@Composable
private fun AuthInfoCard(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(AppDimens.radiusCard)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .appShadow(AppTheme.shadows.card, shape)
            .clip(shape)
            .background(AppTheme.colors.card)
            .padding(AppDimens.space16),
        verticalArrangement = Arrangement.spacedBy(AppDimens.space16),
    ) { content() }
}

/** Figma 02.6: the three numbered steps. */
@Composable
private fun StepsCard() {
    AuthInfoCard {
        StepRow(1, stringResource(R.string.verify_step1_title), stringResource(R.string.verify_step1_body))
        StepRow(2, stringResource(R.string.verify_step2_title), stringResource(R.string.verify_step2_body))
        StepRow(3, stringResource(R.string.verify_step3_title), stringResource(R.string.verify_step3_body))
    }
}

@Composable
private fun StepRow(number: Int, title: String, body: String) {
    val colors = AppTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(28.dp).background(colors.brandSoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = colors.brandSoftText,
            )
        }
        Column(modifier = Modifier.padding(start = AppDimens.space12)) {
            Text(title, style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.5.sp), color = colors.textPrimary)
            Text(body, style = AppTheme.textStyles.helper.copy(fontWeight = FontWeight.Medium), color = colors.textSecondary)
        }
    }
}

/** Figma 02.7: "Your email isn't verified yet". */
@Composable
private fun NotVerifiedCard() {
    val colors = AppTheme.colors
    AuthInfoCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(28.dp).background(colors.errorSoft, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(AppIcons.CircleAlert), null, tint = colors.error, modifier = Modifier.size(AppDimens.iconSm))
            }
            Column(modifier = Modifier.padding(start = AppDimens.space12)) {
                Text(
                    stringResource(R.string.verify_not_verified_title),
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                    color = colors.textPrimary,
                )
                Text(
                    stringResource(R.string.verify_not_verified_body),
                    style = AppTheme.textStyles.helper.copy(fontWeight = FontWeight.Medium),
                    color = colors.textSecondary,
                )
            }
        }
    }
}

/** Figma 02.7: "Can't find it?" tips. */
@Composable
private fun HelpCard() {
    val colors = AppTheme.colors
    AuthInfoCard {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.space4)) {
            Text(
                stringResource(R.string.verify_help_title),
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.5.sp),
                color = colors.textPrimary,
            )
            Text(stringResource(R.string.verify_help_spam), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium), color = colors.textSecondary)
            Text(stringResource(R.string.verify_help_expiry), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium), color = colors.textSecondary)
        }
    }
}

/** "Didn't get it? Resend in 0:42" → "Didn't get it? Resend link". */
@Composable
private fun ResendRow(secondsLeft: Int, enabled: Boolean, onResend: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.resend_prompt),
            style = MaterialTheme.typography.bodySmall,
            color = AppTheme.colors.textSecondary,
        )
        // Disabled Ghost = textTertiary label, exactly Figma's grey "Resend in 0:60".
        AppButton(
            text = if (secondsLeft > 0) {
                stringResource(R.string.resend_in, "0:%02d".format(secondsLeft))
            } else {
                stringResource(R.string.verify_resend_link)
            },
            onClick = onResend,
            variant = AppButtonVariant.Ghost,
            size = AppButtonSize.Small,
            enabled = enabled,
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun VerifyEmailPreview() {
    AppTheme {
        EmailVerificationScreenContent(
            state = EmailVerificationUiState(email = "amira.bensalah@gmail.com", resendCooldownSeconds = 60),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun NotVerifiedPreview() {
    AppTheme {
        EmailVerificationScreenContent(
            state = EmailVerificationUiState(email = "amira.bensalah@gmail.com", isNotVerified = true),
            onIntent = {},
        )
    }
}
```

> **Sizes read off Figma** (36 dp icon tile, 28 dp step circles, 13 / 13.5 sp titles) are used only in this file, so they stay local. If another screen needs the numbered-steps card, move `StepsCard` to `core/designsystem/components/display/`.

> **The guide's "Use another account" button** isn't in Figma 02.6/02.7. The back arrow (`BackClicked`) now does the same thing: sign out and return to Welcome.

---

## 8. All set (Figma 02.5), new screen

**Route:** `@Serializable data class AllSetScreen(val firstName: String)`. It has no ViewModel, because it has no logic. Use `navigateAsRoot`, so Back doesn't return to verification.

| y | Element | Component |
|---|---|---|
| aurora tall | | `AppScaffold(background = AuroraTall)`, no back button |
| ~320 | 72 dp circle, `gradients.brand`, `shadows.button`, white `check` 32 dp, with a 120 dp `brandRing` halo behind it | a `Box` in the screen |
| 400 | "You're all set, {firstName}!" | `headlineLarge`, centered |
| 440 | "Your account is ready. Here's a little welcome gift to get you started." | `bodySmall`, `textSecondary`, centered |
| 470 | gift card: `card`, radius `radiusCard` (20), `shadows.card`. Leading 40 dp `gradients.deal` tile with a white `gift` icon. "3 free deliveries" (`titleSmall`) / "Plus 15% off with WELCOME15" (`bodySmall`). Trailing `AppBadge(FreeDelivery, "Unlocked")` | `Row` |
| bottom | `AppButton(Primary, leadingIcon = MapPin)` "Add my delivery address" | goes to the Addresses feature (later). For now it goes to Home |
| bottom | `AppButton(Ghost)` "Start shopping" | goes to Home |

> The welcome gift is **marketing copy until the `coupons` collection exists** (see the Firebase docs). Hide the gift card behind a simple `showWelcomeGift` parameter so you don't promise something the backend doesn't grant yet. The confetti dots in Figma are optional decoration. Skip them, or draw 6 small `Canvas` circles in `brand`, `deal` and `gold`.

---

## 9. Strings

"Update" means the key already exists: change its value in all three files.

| Key | EN | AR | FR |
|---|---|---|---|
| `auth_welcome_title` *(new)* | Welcome to Dukkan | مرحبًا بك في دكّان | Bienvenue sur Dukkan |
| `auth_welcome_subtitle` *(new)* | Fashion, tech, beauty and home — from stores near you, delivered fast. | أزياء، تقنية، تجميل ومنزل — من متاجر قريبة منك، توصيل سريع. | Mode, tech, beauté et maison — des boutiques près de chez vous, livrées vite. |
| `auth_perk_fast` *(new)* | 30 min | 30 دقيقة | 30 min |
| `auth_perk_cash` *(new)* | Cash on delivery | الدفع عند الاستلام | Paiement à la livraison |
| `auth_perk_returns` *(new)* | Easy returns | إرجاع سهل | Retours faciles |
| `auth_continue_google` *(new)* | Continue with Google | المتابعة باستخدام Google | Continuer avec Google |
| `auth_continue_facebook` *(new)* | Continue with Facebook | المتابعة باستخدام Facebook | Continuer avec Facebook |
| `auth_continue_email` *(new)* | Continue with email | المتابعة بالبريد الإلكتروني | Continuer avec l’e-mail |
| `auth_or` *(new)* | or | أو | ou |
| `auth_or_continue_with` *(new)* | or continue with | أو تابع باستخدام | ou continuer avec |
| `auth_google` *(new)* | Google | Google | Google |
| `auth_facebook` *(new)* | Facebook | Facebook | Facebook |
| `auth_terms` *(new)* | Terms | الشروط | Conditions |
| `auth_privacy` *(new)* | Privacy Policy | سياسة الخصوصية | Politique de confidentialité |
| `auth_terms_notice` *(new)* | By continuing you accept the %1$s and %2$s | بالمتابعة، أنت توافق على %1$s و%2$s | En continuant, vous acceptez les %1$s et la %2$s |
| `login_title` *(update)* | Welcome back | مرحبًا بعودتك | Bon retour |
| `login_subtitle` *(update)* | Log in to track orders and see your coupons. | سجّل الدخول لتتبع طلباتك ورؤية قسائمك. | Connectez-vous pour suivre vos commandes et voir vos coupons. |
| `label_email` *(new)* | Email | البريد الإلكتروني | E-mail |
| `label_password` *(new)* | Password | كلمة المرور | Mot de passe |
| `login_remember_me` *(update)* | Keep me logged in | ابقَ متصلًا | Rester connecté |
| `login_forgot_password` *(update)* | Forgot password? | نسيت كلمة المرور؟ | Mot de passe oublié ? |
| `login_no_account` *(update)* | New to Dukkan? | جديد في دكّان؟ | Nouveau sur Dukkan ? |
| `login_sign_up` *(update)* | Create account | إنشاء حساب | Créer un compte |
| `signup_step` *(new)* | %1$d/%2$d | %1$d/%2$d | %1$d/%2$d |
| `signup_title` *(new)* | Create your account | أنشئ حسابك | Créez votre compte |
| `signup_subtitle` *(new)* | It’s free and takes less than a minute. | مجاني ويستغرق أقل من دقيقة. | C’est gratuit et ça prend moins d’une minute. |
| `label_full_name` *(new)* | Full name | الاسم الكامل | Nom complet |
| `placeholder_full_name` *(new)* | Your full name | اسمك الكامل | Votre nom complet |
| `helper_email_ok` *(new)* | Looks good | يبدو جيدًا | Parfait |
| `password_strength_weak` *(new)* | Weak | ضعيفة | Faible |
| `password_strength_fair` *(new)* | Fair | مقبولة | Moyen |
| `password_strength_good` *(new)* | Good | جيدة | Bon |
| `password_strength_strong` *(new)* | Strong | قوية | Fort |
| `password_strength_format` *(new)* | %1$s · 8+ characters, a number and a symbol | %1$s · 8 أحرف على الأقل، رقم ورمز | %1$s · 8 caractères min., un chiffre et un symbole |
| `signup_accept_terms` *(new)* | I agree to the %1$s and %2$s | أوافق على %1$s و%2$s | J’accepte les %1$s et la %2$s |
| `signup_marketing_opt_in` *(new)* | Send me deals by email | أرسل لي العروض عبر البريد الإلكتروني | Recevoir les offres par e-mail |
| `signup_create_account` *(new)* | Create account | إنشاء حساب | Créer un compte |
| `error_full_name_empty` *(new)* | Enter your full name | أدخل اسمك الكامل | Saisissez votre nom complet |
| `error_terms_not_accepted` *(new)* | Please accept the Terms to continue | يرجى قبول الشروط للمتابعة | Veuillez accepter les conditions pour continuer |
| `forgot_title` *(new)* | Reset your password | إعادة تعيين كلمة المرور | Réinitialiser le mot de passe |
| `forgot_subtitle` *(new)* | We’ll email you a secure link to set a new one. | سنرسل لك رابطًا آمنًا لتعيين كلمة مرور جديدة. | Nous vous enverrons un lien sécurisé pour en définir un nouveau. |
| `forgot_send_link` *(new)* | Send reset link | إرسال رابط إعادة التعيين | Envoyer le lien |
| `forgot_sent_title` *(new)* | Check your inbox | تحقق من بريدك الوارد | Consultez votre boîte de réception |
| `forgot_sent_body` *(new)* | Link sent to %1$s | تم إرسال الرابط إلى %1$s | Lien envoyé à %1$s |
| `forgot_sent_expiry` *(new)* | It expires in 1 hour. | تنتهي صلاحيته خلال ساعة واحدة. | Il expire dans 1 heure. |
| `action_open_email_app` *(new)* | Open email app | فتح تطبيق البريد | Ouvrir l’app e-mail |
| `resend_prompt` *(new)* | Didn’t get it? | لم يصلك؟ | Pas reçu ? |
| `resend_in` *(new)* | Resend in %1$s | إعادة الإرسال خلال %1$s | Renvoyer dans %1$s |
| `action_resend` *(new)* | Resend | إعادة الإرسال | Renvoyer |
| `all_set_title` *(new)* | You’re all set, %1$s! | كل شيء جاهز يا %1$s! | Tout est prêt, %1$s ! |
| `all_set_body` *(new)* | Your account is ready. Here’s a little welcome gift to get you started. | حسابك جاهز. إليك هدية ترحيب صغيرة لتبدأ. | Votre compte est prêt. Voici un petit cadeau de bienvenue pour commencer. |
| `all_set_gift_title` *(new)* | 3 free deliveries | 3 توصيلات مجانية | 3 livraisons offertes |
| `all_set_gift_subtitle` *(new)* | Plus 15% off with WELCOME15 | بالإضافة إلى خصم 15% مع WELCOME15 | Plus 15 % de remise avec WELCOME15 |
| `all_set_unlocked` *(new)* | Unlocked | مفعّل | Débloqué |
| `action_add_address` *(new)* | Add my delivery address | أضف عنوان التوصيل | Ajouter mon adresse |
| `action_start_shopping` *(new)* | Start shopping | ابدأ التسوق | Commencer mes achats |
| `cd_clear_text` *(new)* | Clear text | مسح النص | Effacer le texte |
| `verify_title` *(new)* | Verify your email | تحقّق من بريدك الإلكتروني | Vérifiez votre e-mail |
| `verify_subtitle` *(new)* | We sent a verification link to your inbox. | أرسلنا رابط التحقق إلى بريدك الوارد. | Nous avons envoyé un lien de vérification dans votre boîte. |
| `verify_sent_to` *(new)* | Sent to | أُرسل إلى | Envoyé à |
| `action_change` *(new)* | Change | تغيير | Modifier |
| `verify_step1_title` *(new)* | Open the email from Dukkan | افتح الرسالة من دكّان | Ouvrez l’e-mail de Dukkan |
| `verify_step1_body` *(new)* | Subject: “Verify your Dukkan account” | الموضوع: «تحقّق من حسابك في دكّان» | Objet : « Vérifiez votre compte Dukkan » |
| `verify_step2_title` *(new)* | Tap “Verify my email” | اضغط «تحقّق من بريدي» | Appuyez sur « Vérifier mon e-mail » |
| `verify_step2_body` *(new)* | The link works for 24 hours | الرابط صالح لمدة 24 ساعة | Le lien est valable 24 heures |
| `verify_step3_title` *(new)* | Come back and tap I’ve verified | عُد واضغط «تحقّقت» | Revenez et appuyez sur J’ai vérifié |
| `verify_step3_body` *(new)* | We’ll take you to Home | سننقلك إلى الصفحة الرئيسية | Nous vous emmènerons à l’accueil |
| `verify_i_have_verified` *(new)* | I’ve verified | تحقّقت | J’ai vérifié |
| `verify_not_verified_title` *(new)* | Your email isn’t verified yet | لم يتم التحقق من بريدك بعد | Votre e-mail n’est pas encore vérifié |
| `verify_not_verified_body` *(new)* | Open the link we sent, then tap I’ve verified. | افتح الرابط الذي أرسلناه ثم اضغط «تحقّقت». | Ouvrez le lien envoyé, puis appuyez sur J’ai vérifié. |
| `verify_help_title` *(new)* | Can’t find it? | لم تجده؟ | Vous ne le trouvez pas ? |
| `verify_help_spam` *(new)* | Check Spam or Promotions, or search “Dukkan”. | تحقّق من البريد العشوائي أو العروض، أو ابحث عن «Dukkan». | Vérifiez les spams ou promotions, ou cherchez « Dukkan ». |
| `verify_help_expiry` *(new)* | Links expire after 24 hours. | تنتهي صلاحية الروابط بعد 24 ساعة. | Les liens expirent après 24 heures. |
| `verify_resend_link` *(new)* | Resend link | إعادة إرسال الرابط | Renvoyer le lien |

> Arabic: keep `%1$s` and `%2$s` exactly as written. Android substitutes them, and the Arabic sentence order around them is already correct. In `values-fr`, escape apostrophes (`l\’e-mail`, or use the typographic ’ as above, which needs no escape). Add a non-breaking space before `?` and `!` as French typography expects (`&#160;?`).

---

## 10. Delete the old code (last step)

Run this first. Every hit is a screen you still have to migrate (Language and Onboarding use some of these too):

```bash
grep -rnE "AppPrimaryButton|AppTextButton|AppTextFiled|AppPasswordTextField|AuthButton|AppCheckBox|AppDivider|AppAccountRecovery|core\.common\.components\.(header|footer|scaffold|banner|pager)|iconsax" app/src/main/java
```

When it returns nothing, delete:

```
core/common/components/buttons/            (AppPrimaryButton, AppTextButton)
core/common/components/TextField/          (AppTextField with the typo, AppPasswordTextField)
core/common/components/checkbox/           (AppCheckBox)
core/common/components/dividers/           (AppDivider)
core/common/components/header/             (AppAuthHeader, AppAccountRecoveryHeader)
core/common/components/footer/             (AppAuthFooterLink, AppAccountRecoveryFooter)
core/common/components/scaffold/           (old AppScaffold)
core/common/components/banner/             (old AppMessageBanner)
core/common/components/pager/AppPagerIndicator.kt
features/auth/presentation/components/authoptions/AuthButton.kt
features/auth/presentation/components/authoptions/AuthOptionsSection.kt
res/drawable/ic_app_logo.xml, ic_email.xml, ic_apple.xml (if Decision 1 = Facebook)
```

Then remove the Iconsax dependency (`iconsax` in `libs.versions.toml` and `implementation(libs.iconsax.compose)` in `app/build.gradle.kts`). Lucide `AppIcons` replace it, which means one icon family across the app, as Figma requires.

---

## Check

- [ ] Welcome matches Figma 02.1 side by side (same order, the glow, the perks row, the footer at the bottom).
- [ ] Welcome on a small phone (360 × 640) with font scale 130 %: everything scrolls and nothing overlaps.
- [ ] Log in: tapping "Forgot password?" navigates; the touch area is the full 36 dp pill.
- [ ] Log in with a wrong password: the red banner "Incorrect email or password" appears above the fields, and the fields themselves stay neutral.
- [ ] Sign up: submit empty, and full name, email, password and terms all show their own error.
- [ ] Sign up: a valid email turns the field green with "Looks good". Typing a password fills 1–4 segments.
- [ ] Sign up: tapping **Terms** opens the browser and does *not* toggle the checkbox. Tapping the rest of the label toggles it.
- [ ] After sign-up, the console shows `users/{uid}` with `displayName = "Amira Ben Salah"` and `marketingNotificationsEnabled` matching the checkbox.
- [ ] Verify email matches Figma 02.6: sent-to card, three numbered steps, "Open email app" + "I've verified", grey "Resend in 0:60" counting down.
- [ ] Tap "I've verified" before clicking the link: the steps card is replaced by "Your email isn't verified yet" + "Can't find it?" (Figma 02.7), and the footer becomes a blue "Resend link" when the countdown ends.
- [ ] "Change" signs out and opens Sign up.
- [ ] Verification → All set → "Start shopping" lands on Home, and Back exits the app (no return to auth).
- [ ] Arabic: everything mirrors, the progress bar fills from the right, and the "1/2" sits on the left. Compare Log in with Figma `13.4 · Log in · Arabic (RTL)`: Cairo Bold 24 title, fields and buttons mirrored, social buttons keep their order mirrored too.
- [ ] Dark: compare Log in with Figma `13.2 · Log in · dark`.
- [ ] Every field is a pill (fully rounded), like the buttons.
- [ ] The grep in §10 returns nothing, and the app builds without Iconsax.
