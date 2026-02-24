package com.hanzi.learner.app.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Claymorphism (Soft 3D, playful, toy-like)
 * - Shadow via Modifier.shadow (graphicsLayer, single node)
 * - Background + border merged into single drawBehind pass (reduced overdraw)
 * - Rounded corners (16-24px)
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
    .drawBehind {
        val cr = CornerRadius(cornerRadius.toPx())
        // Background fill
        drawRoundRect(color = backgroundColor, cornerRadius = cr)
        // Border stroke inset by half width to stay within bounds
        val bw = borderWidth.toPx()
        val halfBw = bw / 2f
        drawRoundRect(
            color = borderColor,
            topLeft = Offset(halfBw, halfBw),
            size = Size(size.width - bw, size.height - bw),
            cornerRadius = cr,
            style = Stroke(width = bw)
        )
    }

@Composable
fun Modifier.clayClickable(
    onClick: () -> Unit,
    interactionSource: MutableInteractionSource? = null,
): Modifier {
    val actualInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    return this.clickable(
        interactionSource = actualInteractionSource,
        indication = androidx.compose.material.ripple.rememberRipple(color = MaterialTheme.colorScheme.primary),
        onClick = onClick
    )
}
