'use client';
import { useState, useEffect, useCallback } from 'react';
import { useChatStore } from '@/store/chatStore';
import { useAuthStore } from '@/store/authStore';
import { groupApi } from '@/lib/api';
import { subscribe, unsubscribe } from '@/lib/websocket';
import MessageList from './MessageList';
import MessageInput from './MessageInput';
import { Group, Message } from '@/types';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import { Users, ArrowLeft } from 'lucide-react';
import { getInitials } from '@/lib/utils';
import { useRouter } from 'next/navigation';
import toast from 'react-hot-toast';
import GroupInfoPanel from '@/components/group/GroupInfoPanel';

interface Props {
  groupId: string;
}

export default function GroupChatWindow({ groupId }: Props) {
  const { messages, setMessages, addMessage, updateMessage, deleteMessage, setHasMore, hasMoreMessages } = useChatStore();
  const { user } = useAuthStore();
  const [group, setGroup] = useState<Group | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [replyTo, setReplyTo] = useState<Message | null>(null);
  const [showInfo, setShowInfo] = useState(false);
  const router = useRouter();

  const loadMessages = useCallback(
    async (p: number) => {
      try {
        const { data } = await groupApi.getMessages(groupId, p);
        const pageData = data.data || data;
        const msgs: Message[] = (pageData.content || pageData).reverse();
        setMessages(groupId, msgs, p > 0);
        setHasMore(groupId, !pageData.last);
      } catch {
        toast.error('Failed to load messages');
      }
    },
    [groupId, setMessages, setHasMore]
  );

  useEffect(() => {
    const loadGroup = async () => {
      try {
        const { data } = await groupApi.getOne(groupId);
        setGroup(data.data || data);
      } catch {
        toast.error('Failed to load group');
      }
    };

    Promise.all([loadGroup(), loadMessages(0)]).finally(() =>
      setLoading(false)
    );

    const groupTopic = `/topic/group/${groupId}`;
    subscribe(groupTopic, (stompMsg) => {
      try {
        const data = JSON.parse(stompMsg.body);
        if (data.type === 'NEW_MESSAGE') addMessage({ ...data.payload, conversationId: groupId });
        else if (data.type === 'EDIT_MESSAGE') updateMessage({ ...data.payload, conversationId: groupId });
        else if (data.type === 'DELETE_MESSAGE') {
          const { messageId } = data.payload as { messageId: string };
          deleteMessage(messageId, groupId);
        }
      } catch {
        // ignore
      }
    });

    return () => {
      unsubscribe(groupTopic);
    };
  }, [groupId, loadMessages, addMessage, updateMessage, deleteMessage]);

  const loadMore = useCallback(() => {
    const next = page + 1;
    setPage(next);
    loadMessages(next);
  }, [page, loadMessages]);

  const groupMessages = messages[groupId] || [];

  if (!group && loading) {
    return (
      <div className="flex-1 flex items-center justify-center bg-chat-bg">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-accent" />
      </div>
    );
  }

  return (
    <div className="flex h-full bg-chat-bg">
      <div className="flex flex-col flex-1 min-w-0">
        {/* Header */}
        <div className="flex items-center justify-between px-3 py-2 bg-sidebar border-b border-black/10 dark:border-white/10">
          <div className="flex items-center gap-3">
            <Button
              variant="ghost"
              size="icon"
              className="md:hidden"
              onClick={() => router.push('/chat')}
            >
              <ArrowLeft className="h-5 w-5" />
            </Button>
            <button
              onClick={() => setShowInfo(!showInfo)}
              className="flex items-center gap-3"
            >
              <Avatar className="w-10 h-10">
                <AvatarImage src={group?.avatarUrl} alt={group?.name} />
                <AvatarFallback className="bg-accent/20 text-accent font-semibold">
                  {getInitials(group?.name || '')}
                </AvatarFallback>
              </Avatar>
              <div>
                <p className="font-semibold text-sm leading-tight">{group?.name}</p>
                <p className="text-xs text-gray-400">
                  {group?.memberCount || 0} members
                </p>
              </div>
            </button>
          </div>
          <Button
            variant="ghost"
            size="icon"
            onClick={() => setShowInfo(!showInfo)}
          >
            <Users className="h-5 w-5" />
          </Button>
        </div>

        <MessageList
          messages={groupMessages}
          currentUserId={user?.id || ''}
          hasMore={hasMoreMessages[groupId] || false}
          onLoadMore={loadMore}
          loading={loading}
          conversationId={groupId}
          onReply={(msg) => setReplyTo(msg)}
          onEdit={() => {}}
          isGroup
        />

        <MessageInput
          conversationId={groupId}
          replyTo={replyTo}
          onClearReply={() => setReplyTo(null)}
          isGroup
        />
      </div>

      {/* Info panel */}
      {showInfo && group && (
        <div className="w-72 border-l border-black/10 dark:border-white/10 flex-shrink-0">
          <GroupInfoPanel
            group={group}
            onGroupUpdated={(updated) => setGroup((g) => (g ? { ...g, ...updated } : g))}
          />
        </div>
      )}
    </div>
  );
}
