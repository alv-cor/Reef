package dev.pranav.reef.ui.timer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Removed hardcoded color definitions to use M3 color tokens directly.

@Composable
fun TimerControls(
    modifier: Modifier = Modifier,
    onPauseClicked: () -> Unit = {},
    onStopClicked: () -> Unit = {},
    onResetClicked: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Pause Button (Pink Circle) - Using M3 tokens for color approximation (e.g., tertiary)
        Button(
            onClick = onPauseClicked,
            modifier = Modifier
                .size(64.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.tertiaryContainer, // Using M3 token
                contentColor = colorScheme.onTertiaryContainer // Using M3 token
            )
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                // Placeholder for Pause Icon (||)
                Text(
                    text = "||",
                    color = colorScheme.onTertiaryContainer, // Aligned with content color
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 2. Stop Button (Large Light Grey Pill) - Using M3 tokens for color approximation (e.g., surface container)
        Button(
            onClick = onStopClicked,
            modifier = Modifier
                .height(64.dp)
                .weight(1f)
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(32.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.surfaceContainerLow, // Using M3 token
                contentColor = colorScheme.onSurface // Using M3 token
            )
        ) {
            Text(
                text = "Stop",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                style = typography.titleMedium // Using M3 Typography
            )
        }

        // 3. Reset Button (Outline Circle) - Using M3 tokens for outline/icon color
        Button(
            onClick = onResetClicked,
            modifier = Modifier
                .size(64.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent, // Background is transparent
                contentColor = colorScheme.onSurface // Icon color from M3 token
            ),
            border = BorderStroke(2.dp, colorScheme.outline) // Using M3 outline token
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                // Placeholder for Reset Icon (↻)
                Text(
                    text = "↻",
                    color = colorScheme.onSurface, // Aligned with content color
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TimerControlsPreview() {
    // Assuming an overall background color for context
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE0E0F8)) // Overall background color from screenshot
            .padding(top = 200.dp), // Simulate placement in the middle/bottom of the screen
        contentAlignment = Alignment.Center
    ) {
        // MaterialTheme wrapper is essential for M3 components to inherit theme context for rendering.
        MaterialTheme {
            Surface( // Using M3 Surface for context wrapper
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface // Using theme color for card background
            ) {
                TimerControls()
            }
        }
    }
}
