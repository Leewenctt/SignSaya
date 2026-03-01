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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primary,
        shadowColor = MaterialTheme.colorScheme.primary.darker(),
        contentColor = MaterialTheme.colorScheme.onPrimary,
        borderColor = MaterialTheme.colorScheme.primary.darker(),
        modifier = modifier
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.background,
        shadowColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        modifier = modifier
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(100),
        label = "ghostScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isPressed) 0.6f else 1f,
        animationSpec = tween(100),
        label = "ghostAlpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .graphicsLayer(scaleX = scale, scaleY = scale, alpha = alpha)
            .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

// Internal base button used by PrimaryButton and SecondaryButton
@Composable
private fun AppButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    containerColor: Color,
    shadowColor: Color,
    contentColor: Color,
    borderColor: Color = Color.Transparent,
    shadowHeight: Dp = 3.dp,
    cornerRadius: Dp = 16.dp,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp + shadowHeight)
            .drawBehind {
                drawRoundRect(
                    color = shadowColor,
                    topLeft = Offset(0f, shadowHeight.toPx()),
                    size = Size(size.width, size.height - shadowHeight.toPx()),
                    cornerRadius = CornerRadius(cornerRadius.toPx())
                )
            }
    ) {
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(cornerRadius),
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor,
                disabledContainerColor = containerColor
            ),
            border = BorderStroke(1.dp, borderColor),
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
                .offset(y = if (isPressed) shadowHeight else 0.dp)
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
