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
import com.example.dukkanapp.core.theme.AppTheme

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
    val linkStyles = TextLinkStyles(
        style = SpanStyle(
            color = AppTheme.colors.brand,
            fontWeight = FontWeight.Bold
        )
    )

    return buildAnnotatedString {
        append(sentence)
        val termsStart = sentence.indexOf(terms)
        if (termsStart >= 0) {
            addLink(
                LinkAnnotation.Clickable("terms", linkStyles) { onTermsClick() },
                termsStart,
                termsStart + terms.length
            )
        }
        val privacyStart = sentence.indexOf(privacy)
        if (privacyStart >= 0) {
            addLink(
                LinkAnnotation.Clickable("privacy", linkStyles) { onPrivacyClick() },
                privacyStart,
                privacyStart + privacy.length
            )
        }
    }
}