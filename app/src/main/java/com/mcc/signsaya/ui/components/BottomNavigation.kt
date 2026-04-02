package com.mcc.signsaya.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.mcc.signsaya.navigation.navItems

private val BottomBarHeight = 64.dp
private val IconBoxSize = 34.dp
private val IconSize = 24.dp
private val IconLabelSpacing = 2.dp
private const val AnimationDuration = 200
private val IconBoxCorner = RoundedCornerShape(8.dp)

private val ColorSpec = tween<Color>(AnimationDuration)
private val TactileSpring = spring<Float>(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)
private val BorderSpring = spring<Dp>(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)

@Composable
fun SignSayaBottomBar(
    currentDestination: NavDestination?,
    onNavigate: (String) -> Unit
) {
    val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(BottomBarHeight + navBarInset)
            .drawBehind {
                drawLine(
                    color = dividerColor,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(bottom = navBarInset),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        navItems.forEach { screen ->
            key(screen.route) {
                val isSelected = currentDestination?.hierarchy
                    ?.any { it.route == screen.route } == true

                BottomBarItem(
                    icon = screen.icon,
                    title = screen.title,
                    route = screen.route,
                    isSelected = isSelected,
                    onNavigate = onNavigate
                )
            }
        }
    }
}

@Composable
private fun RowScope.BottomBarItem(
    icon: Int,
    title: String,
    route: String,
    isSelected: Boolean,
    onNavigate: (String) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) primary else onSurfaceVariant.copy(alpha = 0.3f),
        animationSpec = ColorSpec,
        label = "contentColor"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) primary else Color.Transparent,
        animationSpec = ColorSpec,
        label = "borderColor"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) primaryContainer.copy(alpha = 0.3f) else Color.Transparent,
        animationSpec = ColorSpec,
        label = "bgColor"
    )

    val borderWidth by animateDpAsState(
        targetValue = when {
            isPressed -> 4.dp
            isSelected -> 2.dp
            else -> 0.dp
        },
        animationSpec = BorderSpring,
        label = "borderWidth"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isPressed && !isSelected) 1.2f else 1.0f,
        animationSpec = TactileSpring,
        label = "iconScale"
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { if (!isSelected) onNavigate(route) }
            )
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(IconBoxSize)
                .background(color = bgColor, shape = IconBoxCorner)
                .border(width = borderWidth, color = borderColor, shape = IconBoxCorner),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = title,
                modifier = Modifier
                    .size(IconSize)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    },
                tint = contentColor
            )
        }

        Spacer(modifier = Modifier.height(IconLabelSpacing))

        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor
        )
    }
}
