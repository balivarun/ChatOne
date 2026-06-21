package com.connectchat.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :convId ORDER BY createdAt ASC")
    fun getMessagesPaged(convId: String): PagingSource<Int, MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("UPDATE messages SET isDeleted = 1 WHERE id = :id")
    suspend fun markDeleted(id: String)

    @Query("UPDATE messages SET content = :content, isEdited = 1 WHERE id = :id")
    suspend fun updateContent(id: String, content: String)

    @Query("DELETE FROM messages WHERE conversationId = :convId")
    suspend fun clearConversation(convId: String)

    @Query("SELECT * FROM messages WHERE id = :id LIMIT 1")
    suspend fun getMessageById(id: String): MessageEntity?
}
