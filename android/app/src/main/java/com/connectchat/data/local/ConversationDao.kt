package com.connectchat.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversations(conversations: List<ConversationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)

    @Query("UPDATE conversations SET unreadCount = 0 WHERE id = :id")
    suspend fun clearUnread(id: String)

    @Query("UPDATE conversations SET unreadCount = unreadCount + 1, lastMessageContent = :lastMsg, updatedAt = :time WHERE id = :id")
    suspend fun incrementUnread(id: String, lastMsg: String, time: String)

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getConversationById(id: String): ConversationEntity?

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversation(id: String)
}
