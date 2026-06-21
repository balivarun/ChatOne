'use client';
import { useChatStore } from '@/store/chatStore';
import { useRouter } from 'next/navigation';
import { ArrowLeft, Search, MoreVertical, Phone, Video } from 'lucide-react';
import { Button } from '@/components/ui/button';
import UserAvatar from '@/components/common/UserAvatar';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { getInitials } from '@/lib/utils';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { conversationApi } from '@/lib/api';
import toast from 'react-hot-toast';

interface Props {
  conversationId: string;
}

export default function ChatHeader({ conversationId }: Props) {
  const { conversations, updateConversation } = useChatStore();
  const router = useRouter();
  const conversation = conversations.find((c) => c.id === conversationId);

  if (!conversation) return null;

  const isGroup = conversation.type === 'GROUP';
  const displayName = isGroup
    ? conversation.group?.name || 'Group'
    : conversation.otherUser?.displayName || 'Unknown';
  const subtitle = isGroup
    ? `${conversation.group?.memberCount || 0} members`
    : conversation.otherUser?.isOnline
    ? 'Online'
    : 'Offline';

  const handleArchive = async () => {
    try {
      await conversationApi.archive(conversationId);
      updateConversation(conversationId, { isArchived: !conversation.isArchived });
      toast.success(conversation.isArchived ? 'Unarchived' : 'Archived');
    } catch {
      toast.error('Failed to archive');
    }
  };

  const handlePin = async () => {
    try {
      await conversationApi.pin(conversationId);
      updateConversation(conversationId, { isPinned: !conversation.isPinned });
      toast.success(conversation.isPinned ? 'Unpinned' : 'Pinned');
    } catch {
      toast.error('Failed to pin');
    }
  };

  const handleMute = async () => {
    try {
      await conversationApi.mute(conversationId);
      updateConversation(conversationId, { isMuted: !conversation.isMuted });
      toast.success(conversation.isMuted ? 'Unmuted' : 'Muted');
    } catch {
      toast.error('Failed to mute');
    }
  };

  return (
    <div className="flex items-center justify-between px-3 py-2 bg-sidebar border-b border-black/10 dark:border-white/10 z-10">
      <div className="flex items-center gap-3">
        <Button
          variant="ghost"
          size="icon"
          className="md:hidden"
          onClick={() => router.push('/chat')}
        >
          <ArrowLeft className="h-5 w-5" />
        </Button>

        {isGroup ? (
          <Avatar className="w-10 h-10 cursor-pointer" onClick={() => router.push(`/group/${conversation.group?.id}`)}>
            <AvatarImage src={conversation.group?.avatarUrl} alt={displayName} />
            <AvatarFallback className="bg-accent/20 text-accent font-semibold">
              {getInitials(displayName)}
            </AvatarFallback>
          </Avatar>
        ) : conversation.otherUser ? (
          <UserAvatar user={conversation.otherUser} size="md" showStatus />
        ) : null}

        <div>
          <p className="font-semibold text-sm leading-tight">{displayName}</p>
          <p
            className={`text-xs ${
              !isGroup && conversation.otherUser?.isOnline
                ? 'text-accent'
                : 'text-gray-400'
            }`}
          >
            {subtitle}
          </p>
        </div>
      </div>

      <div className="flex items-center gap-1">
        <Button variant="ghost" size="icon" title="Search in conversation">
          <Search className="h-5 w-5" />
        </Button>
        <Button variant="ghost" size="icon" title="Voice call" disabled>
          <Phone className="h-5 w-5" />
        </Button>
        <Button variant="ghost" size="icon" title="Video call" disabled>
          <Video className="h-5 w-5" />
        </Button>

        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="icon">
              <MoreVertical className="h-5 w-5" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-48">
            <DropdownMenuItem onClick={handlePin}>
              {conversation.isPinned ? 'Unpin' : 'Pin'} conversation
            </DropdownMenuItem>
            <DropdownMenuItem onClick={handleMute}>
              {conversation.isMuted ? 'Unmute' : 'Mute'} notifications
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem onClick={handleArchive}>
              {conversation.isArchived ? 'Unarchive' : 'Archive'} conversation
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </div>
  );
}
