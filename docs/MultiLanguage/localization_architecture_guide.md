# Android Localization & Locale Switching in Jetpack Compose

This document explains the architecture and recent changes made to support dynamic, in-app language switching without UI flickering or restarting the app.

---

## 1. The Big Picture: How the Flow Works
When a user selects a language in your app, here is the exact step-by-step flow of how the system processes it:

1. **User Action:** The user taps a language in `LanguageSelectionScreenContent`.
2. **ViewModel to Repository:** The click event is sent to the `LanguageSelectionViewModel`, which calls `SelectLanguageUseCase`, which in turn calls `LanguageRepositoryImpl`.
3. **Data Persistence:** The Repository saves the selected language code ("ar", "en", "fr") into Android DataStore so it remembers the choice on the next app launch.
4. **Triggering the OS:** The Repository calls `PlatformLocaleDataSource.setAppLocal(code)`. This executes `AppCompatDelegate.setApplicationLocales(...)`.
5. **System Interception:** The Android System intercepts this command, updates the app's internal `Configuration`, and notifies the Activity that the locale has changed.
6. **Compose Recomposition:** Because we told the OS *not* to kill the Activity (using `configChanges`), Jetpack Compose detects the updated `Configuration` and instantly re-runs any `stringResource(R.string...)` functions to fetch the new translations.

---

## 2. ComponentActivity vs. AppCompatActivity

You asked why we had to change `MainActivity` from `ComponentActivity` to `AppCompatActivity`.

### `ComponentActivity`
* **What it is:** The modern, lightweight base class for Android Activities.
* **When to use it:** When building a 100% Jetpack Compose app from scratch. It has everything Compose needs (lifecycles, view models) without any legacy UI baggage.

### `AppCompatActivity`
* **What it is:** An older, heavier class that extends `ComponentActivity`. It includes the `AppCompatDelegate`, which backports modern Android features to older Android versions (API 21 to 32).
* **Why we need it for Localization:** `AppCompatDelegate.setApplicationLocales()` is the official Google API for changing languages dynamically.
  * On Android 13+ (API 33), the OS natively supports per-app languages.
  * On Android 12 and below, the OS *does not*. To make it work on older devices, `AppCompatDelegate` intercepts the creation of the Activity's `Context` and forces it to use the new language. **This legacy interception only works if your Activity inherits from `AppCompatActivity`.**

> [!IMPORTANT]
> If you used `ComponentActivity`, language switching would work on an Android 14 device, but would completely fail to update the UI on an Android 11 device.

---

## 3. Why did we create `locales_config.xml`?

Starting in Android 13 (API 33), Google introduced a system-level "App Languages" settings page where users can change an app's language outside of the app.

For security and efficiency, Android 13+ requires you to explicitly declare **which languages your app actually supports**.

* **The Rule:** If you call `AppCompatDelegate.setApplicationLocales("ar")` on Android 13+, the OS will look at your `locales_config.xml`. If "ar" is not in that file (or the file doesn't exist), **the OS silently ignores your request** and the language doesn't change.
* **The Implementation:** We created the file, listed `en`, `ar`, and `fr`, and linked it in the `AndroidManifest.xml` using `android:localeConfig="@xml/locales_config"`.

---

## 4. The "Black Flash" and `android:configChanges`

By default, when a "configuration change" happens in Android (like rotating the screen, plugging in a keyboard, or **changing the language**), Android takes a destructive approach: **It completely destroys your Activity and creates a brand new one.**

### The Problem
When `MainActivity` was destroyed and recreated, there was a split-second gap where Jetpack Compose wasn't drawn yet. During this gap, Android showed the default window background (which was a dark color). This resulted in an ugly "black flash."

### The Solution
We added this line to the `<activity>` in `AndroidManifest.xml`:
```xml
android:configChanges="locale|layoutDirection|uiMode|screenSize|screenLayout|keyboardHidden"
```

### What this does:
This tells the Android OS: *"Hey, if the `locale` (language) or `layoutDirection` (Left-to-Right vs Right-to-Left) changes, **DO NOT** kill my Activity. Just update the configuration internally, and I will handle the UI updates myself."*

Because Jetpack Compose is reactive, it listens to the `Configuration` object. The moment the OS updates the configuration under the hood:
1. The Activity stays alive (no black flash).
2. Compose detects the change.
3. Compose triggers a "recomposition", instantly swapping out the English strings for Arabic strings and flipping the layout to RTL.

> [!TIP]
> This is the true power of Jetpack Compose. In the old XML view system, updating the language without restarting the Activity required manually finding every single `TextView` and calling `.setText()` on it. In Compose, it happens automatically!