'use client';
import { useState, useEffect, useCallback } from 'react';
import { useChatStore } from '@/store/chatStore';
import { useAuthStore } from '@/store/authStore';
import { conversationApi } from '@/lib/api';
import { subscribe, unsubscribe } from '@/lib/websocket';
import MessageList from './MessageList';
import MessageInput from './MessageInput';
import ChatHeader from './ChatHeader';
import { Message } from '@/types';
import toast from 'react-hot-toast';

interface Props {
  conversationId: string;
}

export default function ChatWindow({ conversationId }: Props) {
  const {
    messages,
    setMessages,
    addMessage,
    updateMessage,
    deleteMessage,
    setHasMore,
    hasMoreMessages,
    clearUnread,
  } = useChatStore();
  const { user } = useAuthStore();
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [replyTo, setReplyTo] = useState<Message | null>(null);
  const [editMessage, setEditMessage] = useState<Message | null>(null);

  const loadMessages = useCallback(
    async (p: number) => {
      try {
        const { data } = await conversationApi.getMessages(conversationId, p);
        const pageData = data.data || data;
        const msgs: Message[] = (pageData.content || pageData).reverse();
        setMessages(conversationId, msgs, p > 0);
        setHasMore(conversationId, !pageData.last);
        if (p === 0) clearUnread(conversationId);
      } catch {
        toast.error('Failed to load messages');
      }
    },
    [conversationId, setMessages, setHasMore, clearUnread]
  );

  useEffect(() => {
    loadMessages(0).finally(() => setLoading(false));

    const convTopic = `/topic/conversation/${conversationId}`;
    const userQueue = `/user/queue/messages`;

    subscribe(convTopic, (stompMsg) => {
      try {
        const data = JSON.parse(stompMsg.body);
        if (data.type === 'NEW_MESSAGE') addMessage(data.payload as Message);
        else if (data.type === 'EDIT_MESSAGE') updateMessage(data.payload as Message);
        else if (data.type === 'DELETE_MESSAGE') {
          const { messageId } = data.payload as { messageId: string };
          deleteMessage(messageId, conversationId);
        }
      } catch {
        // ignore parse errors
      }
    });

    subscribe(userQueue, (stompMsg) => {
      try {
        const data: Message = JSON.parse(stompMsg.body);
        if (data.conversationId === conversationId) addMessage(data);
      } catch {
        // ignore
      }
    });

    return () => {
      unsubscribe(convTopic);
    };
  }, [conversationId, loadMessages, addMessage, updateMessage, deleteMessage]);

  const loadMore = useCallback(() => {
    const next = page + 1;
    setPage(next);
    loadMessages(next);
  }, [page, loadMessages]);

  const conversationMessages = messages[conversationId] || [];

  return (
    <div className="flex flex-col h-full bg-chat-bg">
      <ChatHeader conversationId={conversationId} />
      <MessageList
        messages={conversationMessages}
        currentUserId={user?.id || ''}
        hasMore={hasMoreMessages[conversationId] || false}
        onLoadMore={loadMore}
        loading={loading}
        conversationId={conversationId}
        onReply={(msg) => setReplyTo(msg)}
        onEdit={(msg) => setEditMessage(msg)}
      />
      <MessageInput
        conversationId={conversationId}
        replyTo={replyTo}
        onClearReply={() => setReplyTo(null)}
      />
    </div>
  );
}
