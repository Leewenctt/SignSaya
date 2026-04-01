package com.mcc.signsaya.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mcc.signsaya.R

@Composable
fun PrimaryButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    AppButton(
        onClick = onClick,
        enabled = enabled && !loading,
        containerColor = MaterialTheme.colorScheme.primary,
        shadowColor = MaterialTheme.colorScheme.primary.darker(),
        contentColor = MaterialTheme.colorScheme.onPrimary,
        borderColor = MaterialTheme.colorScheme.primary.darker(),
        modifier = modifier
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
                strokeCap = StrokeCap.Round
            )
        } else {
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun GoogleButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    AppButton(
        onClick = onClick,
        enabled = enabled && !loading,
        containerColor = MaterialTheme.colorScheme.surface,
        shadowColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        borderColor = MaterialTheme.colorScheme.outlineVariant,
        modifier = modifier
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp,
                strokeCap = StrokeCap.Round
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_google),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.Unspecified
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "CONTINUE WITH GOOGLE",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    AppButton(
        onClick = onClick,
        enabled = enabled,
        containerColor = MaterialTheme.colorScheme.background,
        shadowColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
        contentColor = MaterialTheme.colorScheme.primary,
        borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
        modifier = modifier
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.98f else 1f,
        animationSpec = tween(150),
        label = "ghostScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.6f else 1f,
        animationSpec = tween(150),
        label = "ghostAlpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .graphicsLayer(scaleX = scale, scaleY = scale, alpha = alpha)
            .padding(vertical = 6.dp, horizontal = 6.dp)
    ) {
        Text(
            text = text,
            color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun BackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        // Negative offset compensates for IconButton's internal padding to align with edge content
        modifier = modifier.offset(x = (-12).dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_back),
            contentDescription = "Back",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun AppButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
    containerColor: Color,
    shadowColor: Color,
    contentColor: Color,
    borderColor: Color = Color.Transparent,
    shadowHeight: Dp = 3.dp,
    cornerRadius: Dp = 14.dp,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val finalContentColor = if (enabled) contentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    val finalContainerColor = if (enabled) containerColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val finalBorderColor = if (enabled) borderColor else Color.Transparent

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp + shadowHeight)
            .then(
                if (enabled) {
                    Modifier.drawBehind {
                        drawRoundRect(
                            color = shadowColor,
                            topLeft = Offset(0f, shadowHeight.toPx()),
                            size = Size(size.width, size.height - shadowHeight.toPx()),
                            cornerRadius = CornerRadius(cornerRadius.toPx())
                        )
                    }
                } else {
                    Modifier
                }
            )
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = RoundedCornerShape(cornerRadius),
            colors = ButtonDefaults.buttonColors(
                containerColor = finalContainerColor,
                contentColor = finalContentColor,
                disabledContainerColor = finalContainerColor,
                disabledContentColor = finalContentColor
            ),
            border = BorderStroke(1.dp, finalBorderColor),
            interactionSource = interactionSource,
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                hoveredElevation = 0.dp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .align(Alignment.TopStart)
                .offset(y = if (isPressed || !enabled) shadowHeight else 0.dp)
        ) {
            content()
        }
    }
}

internal fun Color.darker(factor: Float = 0.75f) = Color(
    red = red * factor,
    green = green * factor,
    blue = blue * factor,
    alpha = alpha
)
