'use client';
import { useChatStore } from '@/store/chatStore';
import { useAuthStore } from '@/store/authStore';

interface Props {
  conversationId: string;
}

export default function TypingIndicator({ conversationId }: Props) {
  const { typingUsers } = useChatStore();
  const { user } = useAuthStore();
  const typingSet = typingUsers[conversationId] || new Set<string>();
  const typingIds = Array.from(typingSet).filter((id) => id !== user?.id);

  if (typingIds.length === 0) return null;

  return (
    <div className="flex items-center gap-2 px-4 py-1">
      <div className="bg-message-in rounded-xl px-3 py-2 flex items-center gap-1 shadow-sm">
        <span className="text-xs text-gray-500 mr-1">
          {typingIds.length === 1 ? 'Someone is' : 'People are'} typing
        </span>
        <div className="flex gap-0.5">
          <span className="w-1.5 h-1.5 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
          <span className="w-1.5 h-1.5 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
          <span className="w-1.5 h-1.5 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
        </div>
      </div>
    </div>
  );
}
