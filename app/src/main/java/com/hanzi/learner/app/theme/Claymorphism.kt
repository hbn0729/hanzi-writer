package com.hanzi.learner.app.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Claymorphism (Soft 3D, playful, toy-like)
 * - Soft shadows
 * - Thick borders (3-4px)
 * - Rounded corners (16-24px)
 * - Press interaction animation
 */
@Composable
fun Modifier.claymorphism(
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
    shadowColor: Color = Color.Black.copy(alpha = 0.1f),
    cornerRadius: Dp = 16.dp,
    borderWidth: Dp = 3.dp,
    elevation: Dp = 6.dp,
    shape: Shape = RoundedCornerShape(cornerRadius)
): Modifier = this
    .shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = shadowColor,
        spotColor = shadowColor
    )
    .background(color = backgroundColor, shape = shape)
    .border(width = borderWidth, color = borderColor, shape = shape)

fun Modifier.clayClickable(
    onClick: () -> Unit,
    interactionSource: MutableInteractionSource? = null,
): Modifier = composed {
    val actualInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    this.clickable(
        interactionSource = actualInteractionSource,

        indication = androidx.compose.material.ripple.rememberRipple(color = MaterialTheme.colorScheme.primary),
        onClick = onClick
    )
}
