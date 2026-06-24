'use client';
import { useState } from 'react';
import Image from 'next/image';
import {
  Reply,
  Forward,
  Pencil,
  Trash2,
  Download,
  CheckCheck,
  Check,
} from 'lucide-react';
import { Message } from '@/types';
import { formatMessageTime, formatFileSize, isImageType, cn } from '@/lib/utils';
import UserAvatar from '@/components/common/UserAvatar';
import { messageApi } from '@/lib/api';
import toast from 'react-hot-toast';
import { useChatStore } from '@/store/chatStore';

interface Props {
  message: Message;
  isOwn: boolean;
  showAvatar?: boolean;
  showSenderName?: boolean;
  onReply: (msg: Message) => void;
  onEdit?: (msg: Message) => void;
}

export default function MessageBubble({
  message,
  isOwn,
  showAvatar = false,
  showSenderName = false,
  onReply,
  onEdit,
}: Props) {
  const [hovered, setHovered] = useState(false);
  const { deleteMessage, updateMessage } = useChatStore();

  const handleDelete = async () => {
    if (!confirm('Delete this message?')) return;
    try {
      await messageApi.delete(message.id);
      deleteMessage(message.id, message.conversationId);
    } catch {
      toast.error('Failed to delete message');
    }
  };

  if (message.isDeleted) {
    return (
      <div className={`flex ${isOwn ? 'justify-end' : 'justify-start'} px-4 py-0.5`}>
        <div
          className={`px-4 py-2 rounded-xl max-w-xs italic text-gray-400 text-sm ${
            isOwn ? 'bg-message-out' : 'bg-message-in'
          }`}
        >
          This message was deleted
        </div>
      </div>
    );
  }

  return (
    <div
      className={`flex ${isOwn ? 'justify-end' : 'justify-start'} px-4 py-0.5 group`}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
    >
      {!isOwn && showAvatar && (
        <div className="mr-2 self-end flex-shrink-0">
          <UserAvatar user={message.sender} size="sm" />
        </div>
      )}
      {!isOwn && !showAvatar && <div className="w-8 mr-2 flex-shrink-0" />}

      <div className={`relative max-w-[70%] ${isOwn ? 'items-end' : 'items-start'} flex flex-col`}>
        {/* Hover action bar */}
        {hovered && (
          <div
            className={`absolute ${
              isOwn ? 'right-full mr-2' : 'left-full ml-2'
            } top-0 flex items-center gap-1 bg-white dark:bg-gray-800 rounded-lg shadow-md p-1 z-10`}
          >
            <button
              onClick={() => onReply(message)}
              className="p-1 hover:bg-gray-100 dark:hover:bg-gray-700 rounded"
              title="Reply"
            >
              <Reply className="w-4 h-4 text-gray-500" />
            </button>
            <button
              className="p-1 hover:bg-gray-100 dark:hover:bg-gray-700 rounded"
              title="Forward"
            >
              <Forward className="w-4 h-4 text-gray-500" />
            </button>
            {isOwn && message.type === 'TEXT' && (
              <button
                onClick={() => onEdit?.(message)}
                className="p-1 hover:bg-gray-100 dark:hover:bg-gray-700 rounded"
                title="Edit"
              >
                <Pencil className="w-4 h-4 text-gray-500" />
              </button>
            )}
            {isOwn && (
              <button
                onClick={handleDelete}
                className="p-1 hover:bg-red-50 dark:hover:bg-red-900/20 rounded"
                title="Delete"
              >
                <Trash2 className="w-4 h-4 text-red-500" />
              </button>
            )}
          </div>
        )}

        <div
          className={cn(
            'rounded-xl shadow-sm',
            isOwn ? 'bg-message-out rounded-br-sm' : 'bg-message-in rounded-bl-sm',
            message.replyTo ? 'rounded-tl-xl rounded-tr-xl' : ''
          )}
        >
          {showSenderName && !isOwn && (
            <p className="text-xs font-semibold text-accent px-3 pt-2">
              {message.sender.displayName}
            </p>
          )}

          {/* Reply preview */}
          {message.replyTo && (
            <div className="mx-2 mt-2 mb-1 pl-2 border-l-2 border-accent bg-black/5 dark:bg-white/5 rounded-r-md">
              <p className="text-xs font-medium text-accent truncate">
                {message.replyTo.sender.displayName}
              </p>
              <p className="text-xs text-gray-500 truncate">
                {message.replyTo.isDeleted
                  ? 'Deleted message'
                  : message.replyTo.content || 'Attachment'}
              </p>
            </div>
          )}

          {/* Attachments */}
          {message.attachments && message.attachments.length > 0 && (
            <div className="p-2 space-y-1">
              {message.attachments.map((att) =>
                isImageType(att.fileType) ? (
                  <div key={att.id} className="relative w-48 h-48 rounded-lg overflow-hidden">
                    <Image
                      src={att.url}
                      alt={att.fileName || 'Image'}
                      fill
                      className="object-cover"
                      sizes="192px"
                    />
                  </div>
                ) : (
                  <a
                    key={att.id}
                    href={att.url}
                    download={att.fileName}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="flex items-center gap-2 bg-black/5 dark:bg-white/5 rounded-lg px-3 py-2 hover:bg-black/10 dark:hover:bg-white/10 transition-colors"
                  >
                    <Download className="w-4 h-4 text-accent" />
                    <div className="flex-1 min-w-0">
                      <p className="text-xs font-medium truncate">
                        {att.fileName || 'File'}
                      </p>
                      {att.fileSize && (
                        <p className="text-xs text-gray-400">
                          {formatFileSize(att.fileSize)}
                        </p>
                      )}
                    </div>
                  </a>
                )
              )}
            </div>
          )}

          {/* Sticker */}
          {message.type === 'STICKER' && message.content && (
            <div className="px-3 py-2">
              <span style={{ fontSize: '64px', lineHeight: 1 }}>{message.content}</span>
            </div>
          )}

          {/* Text content */}
          {message.type !== 'STICKER' && message.content && (
            <div className="px-3 py-2">
              <p className="text-sm whitespace-pre-wrap break-words">
                {message.content}
              </p>
            </div>
          )}

          {/* Timestamp and status */}
          <div
            className={`flex items-center gap-1 px-2 pb-1 ${
              isOwn ? 'justify-end' : 'justify-end'
            }`}
          >
            {message.isEdited && (
              <span className="text-[10px] text-gray-400">edited</span>
            )}
            <span className="text-[10px] text-gray-400">
              {formatMessageTime(message.createdAt)}
            </span>
            {isOwn && (
              <span className="ml-0.5">
                {message.readBy && message.readBy.length > 0 ? (
                  <CheckCheck className="w-3.5 h-3.5 text-accent" />
                ) : (
                  <Check className="w-3.5 h-3.5 text-gray-400" />
                )}
              </span>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
