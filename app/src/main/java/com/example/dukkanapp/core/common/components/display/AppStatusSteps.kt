package com.example.dukkanapp.core.common.components.display

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppPillShape
import com.example.dukkanapp.core.theme.AppTheme
import com.example.dukkanapp.core.utils.constants.AppIcons

data class AppStep(val label: String, val icon: Int)

@Composable
fun AppStatusSteps(
    steps: List<AppStep>,
    currentIndex: Int,
    modifier: Modifier = Modifier
) {
    val c = AppTheme.colors
    Row(
        modifier = modifier, verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, step ->
            when {
                index < currentIndex -> Box(
                    modifier = Modifier
                        .size(
                            AppDimens.statusStepDone
                        )
                        .background(
                            c.brandRing,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(AppIcons.Check),
                        contentDescription = null,
                        tint = c.brand,
                        modifier = Modifier.size(
                            AppDimens.iconXxs
                        )
                    )
                }

                index == currentIndex -> Row(
                    modifier = Modifier
                        .height(AppDimens.statusStepCurrent)
                        .background(
                            c.brand, AppPillShape
                        )
                        .padding(
                            start = AppDimens.space8,
                            end = AppDimens.space12
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.space8)
                ) {
                    Icon(
                        painter = painterResource(
                            step.icon
                        ),
                        contentDescription = null,
                        tint = c.onBrand,
                        modifier = Modifier.size(AppDimens.iconXs)
                    )
                    Text(
                        text = step.label,
                        style = AppTheme.textStyles.labelStrong, color = c.onBrand
                    )
                }

                else -> Box(
                    Modifier
                        .size(AppDimens.statusStep)
                        .background(c.field, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(step.icon),
                        null,
                        tint = c.textTertiary,
                        modifier = Modifier.size(AppDimens.iconXxs)
                    )
                }

            }
            if (index < steps.lastIndex) {
                Box(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = AppDimens.space2)
                        .height(AppDimens.stepConnector)
                        .background(
                            if (index < currentIndex) c.brand else c.borderStrong, AppPillShape
                        )
                )

            }
        }
    }
}