package com.connectchat.data.api

import com.connectchat.data.api.model.AddMembersRequest
import com.connectchat.data.api.model.ApiResponse
import com.connectchat.data.api.model.AuthResponse
import com.connectchat.data.api.model.Conversation
import com.connectchat.data.api.model.CreateConversationRequest
import com.connectchat.data.api.model.CreateGroupRequest
import com.connectchat.data.api.model.EditMessageRequest
import com.connectchat.data.api.model.ForwardMessageRequest
import com.connectchat.data.api.model.Group
import com.connectchat.data.api.model.MarkReadRequest
import com.connectchat.data.api.model.Message
import com.connectchat.data.api.model.MobileAuthRequest
import com.connectchat.data.api.model.Notification
import com.connectchat.data.api.model.PageResponse
import com.connectchat.data.api.model.RefreshTokenRequest
import com.connectchat.data.api.model.SendMessageRequest
import com.connectchat.data.api.model.UpdateGroupRequest
import com.connectchat.data.api.model.UpdateProfileRequest
import com.connectchat.data.api.model.User
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("api/auth/google/mobile")
    suspend fun mobileGoogleSignIn(@Body request: MobileAuthRequest): ApiResponse<AuthResponse>

    @POST("api/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): ApiResponse<AuthResponse>

    @POST("api/auth/logout")
    suspend fun logout(): ApiResponse<Unit>

    @GET("api/users/me")
    suspend fun getMe(): ApiResponse<User>

    @PUT("api/users/me")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): ApiResponse<User>

    @Multipart
    @POST("api/users/me/avatar")
    suspend fun updateAvatar(@Part file: MultipartBody.Part): ApiResponse<User>

    @GET("api/users/search")
    suspend fun searchUsers(
        @Query("query") query: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): ApiResponse<PageResponse<User>>

    @GET("api/users/{id}")
    suspend fun getUser(@Path("id") id: String): ApiResponse<User>

    @POST("api/users/{id}/block")
    suspend fun blockUser(@Path("id") id: String): ApiResponse<Unit>

    @DELETE("api/users/{id}/block")
    suspend fun unblockUser(@Path("id") id: String): ApiResponse<Unit>

    @GET("api/users/blocked")
    suspend fun getBlockedUsers(): ApiResponse<List<User>>

    @GET("api/conversations")
    suspend fun getConversations(): ApiResponse<List<Conversation>>

    @POST("api/conversations")
    suspend fun createConversation(@Body request: CreateConversationRequest): ApiResponse<Conversation>

    @GET("api/conversations/{id}")
    suspend fun getConversation(@Path("id") id: String): ApiResponse<Conversation>

    @GET("api/conversations/{id}/messages")
    suspend fun getMessages(
        @Path("id") id: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 30
    ): ApiResponse<PageResponse<Message>>

    @PUT("api/conversations/{id}/archive")
    suspend fun archiveConversation(@Path("id") id: String): ApiResponse<Unit>

    @PUT("api/conversations/{id}/pin")
    suspend fun pinConversation(@Path("id") id: String): ApiResponse<Unit>

    @PUT("api/conversations/{id}/mute")
    suspend fun muteConversation(@Path("id") id: String): ApiResponse<Unit>

    @POST("api/messages")
    suspend fun sendMessage(@Body request: SendMessageRequest): ApiResponse<Message>

    @PUT("api/messages/{id}")
    suspend fun editMessage(@Path("id") id: String, @Body request: EditMessageRequest): ApiResponse<Message>

    @DELETE("api/messages/{id}")
    suspend fun deleteMessage(@Path("id") id: String): ApiResponse<Unit>

    @POST("api/messages/{id}/forward")
    suspend fun forwardMessage(@Path("id") id: String, @Body request: ForwardMessageRequest): ApiResponse<Message>

    @PUT("api/messages/read")
    suspend fun markRead(@Body request: MarkReadRequest): ApiResponse<Unit>

    @POST("api/groups")
    suspend fun createGroup(@Body request: CreateGroupRequest): ApiResponse<Group>

    @GET("api/groups/mine")
    suspend fun getMyGroups(): ApiResponse<List<Group>>

    @GET("api/groups/search")
    suspend fun searchGroups(@Query("query") query: String): ApiResponse<PageResponse<Group>>

    @GET("api/groups/{id}")
    suspend fun getGroup(@Path("id") id: String): ApiResponse<Group>

    @PUT("api/groups/{id}")
    suspend fun updateGroup(@Path("id") id: String, @Body request: UpdateGroupRequest): ApiResponse<Group>

    @DELETE("api/groups/{id}")
    suspend fun deleteGroup(@Path("id") id: String): ApiResponse<Unit>

    @Multipart
    @POST("api/groups/{id}/avatar")
    suspend fun updateGroupAvatar(@Path("id") id: String, @Part file: MultipartBody.Part): ApiResponse<Group>

    @GET("api/groups/{id}/messages")
    suspend fun getGroupMessages(
        @Path("id") id: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 30
    ): ApiResponse<PageResponse<Message>>

    @POST("api/groups/{id}/messages")
    suspend fun sendGroupMessage(@Path("id") id: String, @Body request: SendMessageRequest): ApiResponse<Message>

    @POST("api/groups/{id}/members")
    suspend fun addGroupMembers(@Path("id") id: String, @Body request: AddMembersRequest): ApiResponse<Group>

    @DELETE("api/groups/{id}/members/{userId}")
    suspend fun removeGroupMember(@Path("id") id: String, @Path("userId") userId: String): ApiResponse<Unit>

    @PUT("api/groups/{id}/members/{userId}/admin")
    suspend fun promoteToAdmin(@Path("id") id: String, @Path("userId") userId: String): ApiResponse<Unit>

    @Multipart
    @POST("api/files/upload")
    suspend fun uploadFile(@Part file: MultipartBody.Part): ApiResponse<Any>

    @GET("api/notifications")
    suspend fun getNotifications(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): ApiResponse<PageResponse<Notification>>

    @GET("api/notifications/unread-count")
    suspend fun getUnreadCount(): ApiResponse<Map<String, Long>>

    @PUT("api/notifications/read-all")
    suspend fun markAllNotificationsRead(): ApiResponse<Unit>
}
