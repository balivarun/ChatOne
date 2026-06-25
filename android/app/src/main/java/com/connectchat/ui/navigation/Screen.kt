package com.connectchat.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object ChatList : Screen("chat_list")
    object Chat : Screen("chat/{conversationId}") {
        fun createRoute(id: String) = "chat/$id"
    }
    object GroupInfo : Screen("group_info/{groupId}") {
        fun createRoute(id: String) = "group_info/$id"
    }
    object CreateGroup : Screen("create_group")
    object Profile : Screen("profile")
    object Settings : Screen("settings")
    object UserSearch : Screen("user_search")
    object Call : Screen("call_screen")
}
