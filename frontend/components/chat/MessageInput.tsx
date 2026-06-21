'use client';
import { useState, useRef, useCallback, useEffect } from 'react';
import { useDropzone } from 'react-dropzone';
import EmojiPicker, { EmojiClickData } from 'emoji-picker-react';
import { Send, Paperclip, Smile, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Message } from '@/types';
import { messageApi, fileApi } from '@/lib/api';
import { useChatStore } from '@/store/chatStore';
import { sendMessage as wsSend } from '@/lib/websocket';
import { useAuthStore } from '@/store/authStore';
import toast from 'react-hot-toast';

interface Props {
  conversationId: string;
  replyTo?: Message | null;
  onClearReply?: () => void;
  isGroup?: boolean;
}

export default function MessageInput({
  conversationId,
  replyTo,
  onClearReply,
  isGroup = false,
}: Props) {
  const [text, setText] = useState('');
  const [showEmoji, setShowEmoji] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [sending, setSending] = useState(false);
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const typingTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const { addMessage } = useChatStore();
  const { user } = useAuthStore();

  const adjustHeight = () => {
    const ta = textareaRef.current;
    if (!ta) return;
    ta.style.height = 'auto';
    ta.style.height = `${Math.min(ta.scrollHeight, 120)}px`;
  };

  useEffect(() => {
    adjustHeight();
  }, [text]);

  const sendTyping = useCallback(
    (isTyping: boolean) => {
      wsSend('/app/chat.typing', {
        conversationId,
        userId: user?.id,
        displayName: user?.displayName,
        isTyping,
      });
    },
    [conversationId, user]
  );

  const handleTextChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setText(e.target.value);
    sendTyping(true);
    if (typingTimerRef.current) clearTimeout(typingTimerRef.current);
    typingTimerRef.current = setTimeout(() => sendTyping(false), 2000);
  };

  const handleSend = async () => {
    const content = text.trim();
    if (!content && !uploading) return;
    if (sending) return;
    setSending(true);
    try {
      const { data } = await messageApi.send({
        conversationId,
        content,
        type: 'TEXT',
        replyToId: replyTo?.id,
      });
      const msg = data.data || data;
      addMessage(msg);
      setText('');
      onClearReply?.();
      sendTyping(false);
    } catch {
      toast.error('Failed to send message');
    } finally {
      setSending(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleEmojiClick = (emojiData: EmojiClickData) => {
    setText((prev) => prev + emojiData.emoji);
    setShowEmoji(false);
    textareaRef.current?.focus();
  };

  const onDrop = useCallback(
    async (acceptedFiles: File[]) => {
      if (acceptedFiles.length === 0) return;
      setUploading(true);
      try {
        for (const file of acceptedFiles) {
          await fileApi.upload(file);
          const { data } = await messageApi.send({
            conversationId,
            type: file.type.startsWith('image/') ? 'IMAGE' : 'FILE',
            replyToId: replyTo?.id,
          });
          const msg = data.data || data;
          addMessage(msg);
        }
        onClearReply?.();
      } catch {
        toast.error('Failed to upload file');
      } finally {
        setUploading(false);
      }
    },
    [conversationId, replyTo, addMessage, onClearReply]
  );

  const { getInputProps, open: openDropzone } = useDropzone({
    onDrop,
    noClick: true,
    noKeyboard: true,
  });

  return (
    <div className="relative">
      <input {...getInputProps()} />

      {/* Reply preview */}
      {replyTo && (
        <div className="flex items-center gap-2 px-4 py-2 bg-sidebar border-t border-black/10 dark:border-white/10">
          <div className="flex-1 pl-2 border-l-2 border-accent">
            <p className="text-xs font-medium text-accent">
              {replyTo.sender.displayName}
            </p>
            <p className="text-xs text-gray-500 truncate">
              {replyTo.isDeleted ? 'Deleted message' : replyTo.content || 'Attachment'}
            </p>
          </div>
          <button onClick={onClearReply} className="text-gray-400 hover:text-foreground">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      <div className="flex items-end gap-2 px-3 py-2 bg-sidebar border-t border-black/10 dark:border-white/10">
        {/* Emoji picker */}
        <div className="relative">
          <Button
            variant="ghost"
            size="icon"
            className="flex-shrink-0"
            onClick={() => setShowEmoji((v) => !v)}
          >
            <Smile className="h-5 w-5" />
          </Button>
          {showEmoji && (
            <div className="absolute bottom-full left-0 mb-2 z-50">
              <EmojiPicker onEmojiClick={handleEmojiClick} />
            </div>
          )}
        </div>

        {/* File attachment */}
        <Button
          variant="ghost"
          size="icon"
          className="flex-shrink-0"
          onClick={openDropzone}
          disabled={uploading}
        >
          {uploading ? (
            <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-accent" />
          ) : (
            <Paperclip className="h-5 w-5" />
          )}
        </Button>

        {/* Text area */}
        <div className="flex-1 relative">
          <textarea
            ref={textareaRef}
            value={text}
            onChange={handleTextChange}
            onKeyDown={handleKeyDown}
            placeholder="Type a message"
            rows={1}
            className="w-full resize-none rounded-lg px-3 py-2 text-sm bg-black/5 dark:bg-white/5 focus:outline-none focus:bg-black/10 dark:focus:bg-white/10 transition-colors max-h-[120px] overflow-y-auto"
            style={{ lineHeight: '1.5' }}
          />
        </div>

        {/* Send button */}
        <Button
          variant="default"
          size="icon"
          className="flex-shrink-0 rounded-full"
          onClick={handleSend}
          disabled={(!text.trim() && !uploading) || sending}
        >
          {sending ? (
            <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white" />
          ) : (
            <Send className="h-4 w-4" />
          )}
        </Button>
      </div>
    </div>
  );
}
