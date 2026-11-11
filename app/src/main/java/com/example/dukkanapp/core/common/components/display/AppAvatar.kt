package com.example.dukkanapp.core.common.components.display

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppTheme

enum class AppAvatarSize(
    val size: Dp
) {
    Small(AppDimens.avatarSm),
    Medium(AppDimens.avatarMd),
    Header(AppDimens.avatarHeader),
    Large(AppDimens.avatarLg),
}

@Composable
fun AppAvatar(
    initial: String,
    modifier: Modifier = Modifier,
    photo: Painter? = null,
    size: AppAvatarSize = AppAvatarSize.Medium
) {
    Box(
        modifier = modifier
            .size(size.size)
            .clip(CircleShape)
            .background(AppTheme.colors.brandRing),
        contentAlignment = Alignment.Center
    ) {
        if (photo != null) {
            Image(
                painter = photo,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size.size)
            )
        } else {
            Text(
                text = initial.take(2).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = AppTheme.colors.brandSoftText
            )
        }
    }
}