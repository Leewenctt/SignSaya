package com.mcc.signsaya.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mcc.signsaya.R
import java.util.UUID

enum class BannerType {
    ERROR,
    SUCCESS
}

data class Banner(
    val message: String,
    val type: BannerType,
    val id: String = UUID.randomUUID().toString()
)

@Composable
fun BannerHost(
    banner: Banner?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = banner,
        transitionSpec = {
            (slideInVertically(animationSpec = tween(300)) { it } + fadeIn(animationSpec = tween(300))) togetherWith
                    (slideOutVertically(animationSpec = tween(300)) { it } + fadeOut(animationSpec = tween(300))) using
                    SizeTransform(clip = false)
        },
        contentAlignment = Alignment.BottomCenter,
        label = "banner_animation",
        modifier = modifier.fillMaxWidth()
    ) { currentBanner ->
        if (currentBanner != null) {
            val backgroundColor = if (currentBanner.type == BannerType.ERROR) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
            val contentColor = if (currentBanner.type == BannerType.ERROR) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            }
            val iconRes = if (currentBanner.type == BannerType.ERROR) {
                R.drawable.ic_error
            } else {
                R.drawable.ic_success
            }
            val titleText = if (currentBanner.type == BannerType.ERROR) "ERROR!" else "SUCCESS!"

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundColor)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = contentColor
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                    Text(
                        text = currentBanner.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor.copy(alpha = 0.9f)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_dismiss),
                        contentDescription = "Close",
                        modifier = Modifier.size(24.dp),
                        tint = contentColor
                    )
                }
            }
        }
    }
}
