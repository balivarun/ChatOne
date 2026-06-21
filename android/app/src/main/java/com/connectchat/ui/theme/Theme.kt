package com.connectchat.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun ConnectChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = Accent,
            background = BackgroundDark,
            surface = SurfaceDark,
            onPrimary = Color.White,
            secondary = AccentDark,
            error = ErrorRed
        )
    } else {
        lightColorScheme(
            primary = Accent,
            background = Color.White,
            surface = SidebarLight,
            onPrimary = Color.White,
            secondary = AccentDark,
            error = ErrorRed
        )
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
