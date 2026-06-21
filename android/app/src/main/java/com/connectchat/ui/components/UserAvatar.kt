package com.connectchat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.connectchat.ui.theme.Accent
import com.connectchat.ui.theme.OnlineGreen

@Composable
fun UserAvatar(
    avatarUrl: String?,
    displayName: String,
    size: Dp = 48.dp,
    showOnlineDot: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "Avatar of $displayName",
                modifier = Modifier.size(size).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            val initial = displayName.firstOrNull()?.uppercaseChar() ?: '?'
            val bgColor = generateAvatarColor(displayName)
            Box(
                modifier = Modifier.size(size).background(bgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.4f).sp
                )
            }
        }

        if (showOnlineDot) {
            Box(
                modifier = Modifier
                    .size(size * 0.28f)
                    .background(OnlineGreen, CircleShape)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(size * 0.22f)
                        .background(OnlineGreen, CircleShape)
                        .align(Alignment.Center)
                )
            }
        }
    }
}

private val avatarColors = listOf(
    Color(0xFF5C6BC0),
    Color(0xFF42A5F5),
    Color(0xFF26A69A),
    Color(0xFF66BB6A),
    Color(0xFFAB47BC),
    Color(0xFFEF5350),
    Color(0xFFFF7043),
    Color(0xFF8D6E63)
)

private fun generateAvatarColor(name: String): Color {
    val index = name.hashCode().and(0x7FFFFFFF) % avatarColors.size
    return avatarColors[index]
}
