package com.connectchat.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.connectchat.ui.screens.chat.ChatScreen
import com.connectchat.ui.screens.chatlist.ChatListScreen
import com.connectchat.ui.screens.group.CreateGroupScreen
import com.connectchat.ui.screens.group.GroupInfoScreen
import com.connectchat.ui.screens.login.LoginScreen
import com.connectchat.ui.screens.profile.ProfileScreen
import com.connectchat.ui.screens.settings.SettingsScreen
import com.connectchat.ui.screens.call.CallScreen
import com.connectchat.ui.screens.usersearch.UserSearchScreen

@Composable
fun NavGraph(navController: NavHostController, startDestination: String) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.ChatList.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.ChatList.route) {
            ChatListScreen(
                onConversationClick = { id -> navController.navigate(Screen.Chat.createRoute(id)) },
                onCreateGroup = { navController.navigate(Screen.CreateGroup.route) },
                onProfileClick = { navController.navigate(Screen.Profile.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                onNewChatClick = { navController.navigate(Screen.UserSearch.route) }
            )
        }
        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val convId = backStackEntry.arguments?.getString("conversationId") ?: return@composable
            ChatScreen(
                conversationId = convId,
                onBack = { navController.popBackStack() },
                onStartCall = { navController.navigate(Screen.Call.route) { launchSingleTop = true } },
                onGroupInfoClick = { groupId -> navController.navigate(Screen.GroupInfo.createRoute(groupId)) }
            )
        }
        composable(
            route = Screen.GroupInfo.route,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            GroupInfoScreen(
                groupId = groupId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.CreateGroup.route) {
            CreateGroupScreen(
                onBack = { navController.popBackStack() },
                onGroupCreated = { id ->
                    navController.navigate(Screen.Chat.createRoute(id)) {
                        popUpTo(Screen.ChatList.route)
                    }
                }
            )
        }
        composable(Screen.Profile.route) {
            ProfileScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.UserSearch.route) {
            UserSearchScreen(
                onUserSelected = { convId ->
                    navController.navigate(Screen.Chat.createRoute(convId)) {
                        popUpTo(Screen.ChatList.route)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Call.route) {
            CallScreen(
                onCallEnded = {
                    navController.navigate(Screen.ChatList.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
