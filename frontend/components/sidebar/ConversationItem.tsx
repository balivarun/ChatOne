'use client';
import { useRouter, usePathname } from 'next/navigation';
import { Pin, BellOff, Archive, CheckCheck, Check } from 'lucide-react';
import { Conversation } from '@/types';
import { useChatStore } from '@/store/chatStore';
import { useAuthStore } from '@/store/authStore';
import UserAvatar from '@/components/common/UserAvatar';
import { formatConversationTime, cn } from '@/lib/utils';

interface Props {
  conversation: Conversation;
}

export default function ConversationItem({ conversation }: Props) {
  const router = useRouter();
  const pathname = usePathname();
  const { setActiveConversation } = useChatStore();
  const { user } = useAuthStore();

  const isActive = pathname === `/chat/${conversation.id}`;

  const displayName =
    conversation.type === 'DIRECT'
      ? conversation.otherUser?.displayName || 'Unknown'
      : conversation.group?.name || 'Unknown Group';

  const avatarUser =
    conversation.type === 'DIRECT' && conversation.otherUser
      ? conversation.otherUser
      : {
          displayName: displayName,
          avatarUrl: conversation.group?.avatarUrl,
          isOnline: false,
        };

  const lastMessageText = conversation.lastMessage
    ? conversation.lastMessage.isDeleted
      ? 'This message was deleted'
      : conversation.lastMessage.type === 'IMAGE'
      ? 'Photo'
      : conversation.lastMessage.type === 'FILE'
      ? 'File'
      : conversation.lastMessage.type === 'VOICE'
      ? 'Voice message'
      : conversation.lastMessage.content || ''
    : 'No messages yet';

  const isOwnMessage =
    conversation.lastMessage?.sender.id === user?.id;

  const handleClick = () => {
    setActiveConversation(conversation.id);
    router.push(`/chat/${conversation.id}`);
  };

  return (
    <div
      onClick={handleClick}
      className={cn(
        'flex items-center gap-3 px-3 py-3 cursor-pointer hover:bg-black/5 dark:hover:bg-white/5 transition-colors',
        isActive && 'bg-black/8 dark:bg-white/8'
      )}
    >
      <div className="relative flex-shrink-0">
        <UserAvatar
          user={avatarUser}
          size="md"
          showStatus={conversation.type === 'DIRECT'}
        />
        {conversation.isPinned && (
          <span className="absolute -top-1 -right-1 text-accent">
            <Pin className="w-3 h-3" />
          </span>
        )}
      </div>

      <div className="flex-1 min-w-0">
        <div className="flex items-center justify-between gap-1">
          <span
            className={cn(
              'text-sm font-medium truncate',
              conversation.isArchived && 'text-gray-400'
            )}
          >
            {displayName}
          </span>
          <div className="flex items-center gap-1 flex-shrink-0">
            {conversation.isMuted && (
              <BellOff className="w-3 h-3 text-gray-400" />
            )}
            {conversation.isArchived && (
              <Archive className="w-3 h-3 text-gray-400" />
            )}
            {conversation.lastMessage && (
              <span className="text-xs text-gray-400">
                {formatConversationTime(conversation.lastMessage.createdAt)}
              </span>
            )}
          </div>
        </div>
        <div className="flex items-center justify-between gap-1 mt-0.5">
          <div className="flex items-center gap-1 min-w-0">
            {isOwnMessage && (
              <span className="flex-shrink-0">
                {conversation.lastMessage?.readBy &&
                conversation.lastMessage.readBy.length > 0 ? (
                  <CheckCheck className="w-3.5 h-3.5 text-accent" />
                ) : (
                  <Check className="w-3.5 h-3.5 text-gray-400" />
                )}
              </span>
            )}
            <p className="text-xs text-gray-500 truncate">{lastMessageText}</p>
          </div>
          {conversation.unreadCount > 0 && !conversation.isMuted && (
            <span className="flex-shrink-0 bg-accent text-white text-xs rounded-full min-w-[18px] h-[18px] flex items-center justify-center px-1">
              {conversation.unreadCount > 99 ? '99+' : conversation.unreadCount}
            </span>
          )}
          {conversation.unreadCount > 0 && conversation.isMuted && (
            <span className="flex-shrink-0 bg-gray-400 text-white text-xs rounded-full min-w-[18px] h-[18px] flex items-center justify-center px-1">
              {conversation.unreadCount > 99 ? '99+' : conversation.unreadCount}
            </span>
          )}
        </div>
      </div>
    </div>
  );
}
