import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppTheme
import com.example.dukkanapp.core.utils.constants.AppIcons

enum class AppMessageKind {
    Error, Success, Info
}

@Composable
fun AppMessageBanner(
    message: String,
    modifier: Modifier = Modifier,
    kind: AppMessageKind = AppMessageKind.Error
) {
    val c = AppTheme.colors
    val (fill, content, icon) = when (kind) {
        AppMessageKind.Error -> Triple(c.errorSoft, c.error, AppIcons.CircleAlert)
        AppMessageKind.Success -> Triple(c.successSoft, c.successText, AppIcons.CircleCheck)
        AppMessageKind.Info -> Triple(c.brandSoft, c.brandSoftText, AppIcons.Info)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                liveRegion = LiveRegionMode.Polite
            },
        shape = RoundedCornerShape(AppDimens.radiusMd),
        color = fill,
        contentColor = content,
        border = if (kind == AppMessageKind.Error) BorderStroke(
            AppDimens.strokeThin,
            c.errorBorder
        ) else null
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = AppDimens.space16,
                vertical = AppDimens.space12
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.space8)
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(AppDimens.icon)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}