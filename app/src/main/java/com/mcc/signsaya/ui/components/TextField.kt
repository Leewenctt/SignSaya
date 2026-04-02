package com.mcc.signsaya.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.mcc.signsaya.R

@Composable
fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    iconRes: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    error: String? = null,
    hideError: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.38f)
    val disabledAlpha = 0.38f

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface.copy(alpha = disabledAlpha),
            modifier = Modifier.padding(bottom = 4.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            textStyle = MaterialTheme.typography.bodyMedium,
            placeholder = {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = outlineColor
                )
            },
            isError = error != null && !hideError,
            leadingIcon = {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = when {
                        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = disabledAlpha)
                        value.isNotEmpty() -> MaterialTheme.colorScheme.primary
                        else -> outlineColor
                    },
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = trailingIcon,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = outlineColor,
                errorBorderColor = MaterialTheme.colorScheme.error,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                errorLabelColor = MaterialTheme.colorScheme.error,
                disabledBorderColor = outlineColor,
                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = disabledAlpha),
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = disabledAlpha),
                disabledPlaceholderColor = outlineColor,
                disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = disabledAlpha)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        AnimatedVisibility(
            visible = !error.isNullOrEmpty() && !hideError,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Text(
                text = error ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    error: String? = null,
    hideError: Boolean = false,
    isPasswordVisible: Boolean? = null,
    onPasswordVisibleChange: ((Boolean) -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    var internalVisible by remember { mutableStateOf(false) }
    val visible = isPasswordVisible ?: internalVisible

    AuthField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        iconRes = R.drawable.ic_lock,
        modifier = modifier,
        enabled = enabled,
        error = error,
        hideError = hideError,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            if (value.isNotEmpty()) {
                val interactionSource = remember { MutableInteractionSource() }
                Icon(
                    painter = painterResource(
                        if (visible) R.drawable.ic_visible else R.drawable.ic_notvisible
                    ),
                    contentDescription = if (visible) "Hide password" else "Show password",
                    tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f),
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            enabled = enabled,
                            onClick = {
                                if (onPasswordVisibleChange != null) {
                                    onPasswordVisibleChange(!visible)
                                } else {
                                    internalVisible = !internalVisible
                                }
                            }
                        )
                )
            }
        },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions
    )
}
