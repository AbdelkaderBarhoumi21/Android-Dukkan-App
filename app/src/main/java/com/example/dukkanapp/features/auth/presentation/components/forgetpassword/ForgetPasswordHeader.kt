package com.example.dukkanapp.features.auth.presentation.components.forgetpassword

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.example.dukkanapp.R
import com.example.dukkanapp.core.utils.constants.AppDimens

@Composable
fun ForgetPasswordHeader(
    modifier: Modifier = Modifier,
    logo: Painter = painterResource(R.drawable.ic_app_logo),
    title: String,
    subTitle: String,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
    ) {
        Image(
            painter = logo,
            contentDescription = null,
            modifier = Modifier.size(AppDimens.iconXxl)
        )
        Spacer(Modifier.height(AppDimens.spaceMd))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(AppDimens.spaceSm))
        Text(
            text = subTitle,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }


}