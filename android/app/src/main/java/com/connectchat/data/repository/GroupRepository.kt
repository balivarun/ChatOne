package com.connectchat.data.repository

import com.connectchat.data.api.ApiService
import com.connectchat.data.api.model.AddMembersRequest
import com.connectchat.data.api.model.CreateGroupRequest
import com.connectchat.data.api.model.Group
import com.connectchat.data.api.model.Message
import com.connectchat.data.api.model.SendMessageRequest
import com.connectchat.data.api.model.UpdateGroupRequest
import okhttp3.MultipartBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun createGroup(
        name: String,
        description: String?,
        memberIds: List<String>
    ): Result<Group> = runCatching {
        val response = apiService.createGroup(CreateGroupRequest(name, description, memberIds))
        response.data ?: throw Exception(response.message ?: "Failed to create group")
    }

    suspend fun getMyGroups(): Result<List<Group>> = runCatching {
        val response = apiService.getMyGroups()
        response.data ?: throw Exception(response.message ?: "Failed to get groups")
    }

    suspend fun searchGroups(query: String): Result<List<Group>> = runCatching {
        val response = apiService.searchGroups(query)
        response.data?.content ?: emptyList()
    }

    suspend fun getGroup(id: String): Result<Group> = runCatching {
        val response = apiService.getGroup(id)
        response.data ?: throw Exception(response.message ?: "Group not found")
    }

    suspend fun updateGroup(id: String, name: String?, description: String?): Result<Group> = runCatching {
        val response = apiService.updateGroup(id, UpdateGroupRequest(name, description))
        response.data ?: throw Exception(response.message ?: "Failed to update group")
    }

    suspend fun deleteGroup(id: String): Result<Unit> = runCatching {
        apiService.deleteGroup(id)
    }

    suspend fun updateGroupAvatar(id: String, file: MultipartBody.Part): Result<Group> = runCatching {
        val response = apiService.updateGroupAvatar(id, file)
        response.data ?: throw Exception(response.message ?: "Failed to update avatar")
    }

    suspend fun getGroupMessages(id: String, page: Int = 0): Result<List<Message>> = runCatching {
        val response = apiService.getGroupMessages(id, page)
        response.data?.content ?: emptyList()
    }

    suspend fun sendGroupMessage(id: String, request: SendMessageRequest): Result<Message> = runCatching {
        val response = apiService.sendGroupMessage(id, request)
        response.data ?: throw Exception(response.message ?: "Failed to send message")
    }

    suspend fun addGroupMembers(id: String, userIds: List<String>): Result<Group> = runCatching {
        val response = apiService.addGroupMembers(id, AddMembersRequest(userIds))
        response.data ?: throw Exception(response.message ?: "Failed to add members")
    }

    suspend fun removeGroupMember(groupId: String, userId: String): Result<Unit> = runCatching {
        apiService.removeGroupMember(groupId, userId)
    }

    suspend fun promoteToAdmin(groupId: String, userId: String): Result<Unit> = runCatching {
        apiService.promoteToAdmin(groupId, userId)
    }
}
