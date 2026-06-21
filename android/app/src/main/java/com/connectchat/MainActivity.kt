package com.connectchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.map
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.connectchat.data.preferences.UserPreferences
import com.connectchat.ui.navigation.NavGraph
import com.connectchat.ui.navigation.Screen
import com.connectchat.ui.theme.Accent
import com.connectchat.ui.theme.ConnectChatTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // null = DataStore not yet read (show loading)
            // ""  = DataStore read, no token stored (go to Login)
            // non-empty = has token (go to ChatList)
            val token by userPreferences.accessToken
                .map { it ?: "" }
                .collectAsState(initial = null)

            ConnectChatTheme {
                when {
                    token == null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Accent)
                        }
                    }
                    else -> {
                        val navController = rememberNavController()
                        val startDest = if (token!!.isNotBlank()) {
                            Screen.ChatList.route
                        } else {
                            Screen.Login.route
                        }
                        NavGraph(
                            navController = navController,
                            startDestination = startDest
                        )
                    }
                }
            }
        }
    }
}
