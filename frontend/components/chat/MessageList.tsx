'use client';
import { useEffect, useRef, useCallback, useState } from 'react';
import { useInView } from 'react-intersection-observer';
import { format } from 'date-fns';
import { Message } from '@/types';
import MessageBubble from './MessageBubble';
import TypingIndicator from './TypingIndicator';
import { messageApi } from '@/lib/api';
import toast from 'react-hot-toast';
import { useChatStore } from '@/store/chatStore';

interface Props {
  messages: Message[];
  currentUserId: string;
  hasMore: boolean;
  onLoadMore: () => void;
  loading: boolean;
  conversationId: string;
  onReply: (msg: Message) => void;
  onEdit: (msg: Message) => void;
  isGroup?: boolean;
}

function groupMessagesByDate(messages: Message[]): Array<{ date: string; messages: Message[] }> {
  const groups: Record<string, Message[]> = {};
  messages.forEach((msg) => {
    const date = format(new Date(msg.createdAt), 'MMMM d, yyyy');
    if (!groups[date]) groups[date] = [];
    groups[date].push(msg);
  });
  return Object.entries(groups).map(([date, msgs]) => ({ date, messages: msgs }));
}

export default function MessageList({
  messages,
  currentUserId,
  hasMore,
  onLoadMore,
  loading,
  conversationId,
  onReply,
  onEdit,
  isGroup = false,
}: Props) {
  const bottomRef = useRef<HTMLDivElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const [autoScroll, setAutoScroll] = useState(true);
  const { ref: topRef, inView: topInView } = useInView({ threshold: 0 });

  const scrollToBottom = useCallback(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, []);

  useEffect(() => {
    if (autoScroll) scrollToBottom();
  }, [messages, autoScroll, scrollToBottom]);

  useEffect(() => {
    if (topInView && hasMore && !loading) {
      onLoadMore();
    }
  }, [topInView, hasMore, loading, onLoadMore]);

  const handleScroll = () => {
    const container = containerRef.current;
    if (!container) return;
    const isNearBottom =
      container.scrollHeight - container.scrollTop - container.clientHeight < 100;
    setAutoScroll(isNearBottom);
  };

  // Mark messages as read
  useEffect(() => {
    const unreadIds = messages
      .filter((m) => m.sender.id !== currentUserId && !m.readBy?.find((u) => u.id === currentUserId))
      .map((m) => m.id);
    if (unreadIds.length > 0) {
      messageApi.markRead(unreadIds).catch(() => {});
    }
  }, [messages, currentUserId]);

  const grouped = groupMessagesByDate(messages);

  if (loading && messages.length === 0) {
    return (
      <div className="flex-1 flex items-center justify-center">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-accent" />
      </div>
    );
  }

  return (
    <div
      ref={containerRef}
      onScroll={handleScroll}
      className="flex-1 overflow-y-auto py-4"
    >
      {/* Load more sentinel */}
      <div ref={topRef} className="h-1" />
      {loading && messages.length > 0 && (
        <div className="flex justify-center py-2">
          <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-accent" />
        </div>
      )}

      {grouped.map(({ date, messages: dayMsgs }) => (
        <div key={date}>
          <div className="flex justify-center my-3">
            <span className="bg-black/10 dark:bg-white/10 text-xs px-3 py-1 rounded-full">
              {date}
            </span>
          </div>
          {dayMsgs.map((msg, idx) => {
            const prevMsg = idx > 0 ? dayMsgs[idx - 1] : null;
            const showAvatar =
              !isGroup ||
              !prevMsg ||
              prevMsg.sender.id !== msg.sender.id;
            const showSenderName = isGroup && showAvatar;
            return (
              <MessageBubble
                key={msg.id}
                message={msg}
                isOwn={msg.sender.id === currentUserId}
                showAvatar={showAvatar}
                showSenderName={showSenderName}
                onReply={onReply}
                onEdit={onEdit}
              />
            );
          })}
        </div>
      ))}

      <TypingIndicator conversationId={conversationId} />
      <div ref={bottomRef} />
    </div>
  );
}
