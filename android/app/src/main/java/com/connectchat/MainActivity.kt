package com.connectchat

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.flow.map
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.connectchat.data.call.CallManager
import com.connectchat.data.call.CallState
import com.connectchat.data.preferences.UserPreferences
import com.connectchat.data.service.ConnectChatFcmService
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

    @Inject
    lateinit var callManager: CallManager

    private val pendingConversationId = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingConversationId.value = intent.getStringExtra("conversationId")
        // Dismiss incoming call notification when user taps Answer
        if (intent?.action == ConnectChatFcmService.ACTION_INCOMING_CALL) {
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .cancel(ConnectChatFcmService.CALL_NOTIFICATION_ID)
        }
        enableEdgeToEdge()
        setContent {
            val token by userPreferences.accessToken
                .map { it ?: "" }
                .collectAsState(initial = null)

            val callState by callManager.callState.collectAsState()

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

                        LaunchedEffect(pendingConversationId.value) {
                            val convId = pendingConversationId.value ?: return@LaunchedEffect
                            if (token!!.isNotBlank()) {
                                pendingConversationId.value = null
                                navController.navigate(Screen.Chat.createRoute(convId)) {
                                    launchSingleTop = true
                                }
                            }
                        }

                        // Navigate to call screen when a call starts (incoming or outgoing)
                        LaunchedEffect(callState) {
                            if (callState is CallState.Incoming || callState is CallState.Outgoing) {
                                navController.navigate(Screen.Call.route) {
                                    launchSingleTop = true
                                }
                            }
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingConversationId.value = intent.getStringExtra("conversationId")
        if (intent.action == ConnectChatFcmService.ACTION_INCOMING_CALL) {
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .cancel(ConnectChatFcmService.CALL_NOTIFICATION_ID)
        }
    }
}
