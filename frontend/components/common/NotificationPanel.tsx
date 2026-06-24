'use client';
import { useEffect, useRef } from 'react';
import { Bell, Check, CheckCheck, MessageSquare, Users } from 'lucide-react';
import { useNotificationStore } from '@/store/notificationStore';
import { notificationApi } from '@/lib/api';
import { Notification } from '@/types';
import { formatMessageTime } from '@/lib/utils';
import UserAvatar from './UserAvatar';

interface Props {
  onClose: () => void;
}

function notificationIcon(type: Notification['type']) {
  if (type === 'NEW_MESSAGE') return <MessageSquare className="w-4 h-4 text-accent" />;
  return <Users className="w-4 h-4 text-accent" />;
}

export default function NotificationPanel({ onClose }: Props) {
  const { notifications, setNotifications, markRead, markAllRead } = useNotificationStore();
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    notificationApi.getAll().then(({ data }) => {
      const items = data.data?.content || data.data || [];
      setNotifications(items);
    }).catch(() => {});
  }, [setNotifications]);

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) onClose();
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, [onClose]);

  const handleMarkRead = async (id: string) => {
    try {
      await notificationApi.markRead(id);
      markRead(id);
    } catch {}
  };

  const handleMarkAllRead = async () => {
    try {
      await notificationApi.markAllRead();
      markAllRead();
    } catch {}
  };

  return (
    <div
      ref={ref}
      className="absolute right-0 top-full mt-2 w-80 bg-white dark:bg-gray-900 rounded-xl shadow-xl border border-black/10 dark:border-white/10 z-50 overflow-hidden"
    >
      <div className="flex items-center justify-between px-4 py-3 border-b border-black/10 dark:border-white/10">
        <div className="flex items-center gap-2 font-semibold text-sm">
          <Bell className="w-4 h-4" />
          Notifications
        </div>
        <button
          onClick={handleMarkAllRead}
          className="text-xs text-accent hover:underline flex items-center gap-1"
        >
          <CheckCheck className="w-3 h-3" />
          Mark all read
        </button>
      </div>

      <div className="max-h-96 overflow-y-auto">
        {notifications.length === 0 ? (
          <div className="py-10 text-center text-sm text-gray-400">
            No notifications yet
          </div>
        ) : (
          notifications.map((n) => (
            <div
              key={n.id}
              className={`flex items-start gap-3 px-4 py-3 hover:bg-black/5 dark:hover:bg-white/5 transition-colors cursor-pointer ${
                !n.isRead ? 'bg-accent/5' : ''
              }`}
              onClick={() => !n.isRead && handleMarkRead(n.id)}
            >
              <div className="flex-shrink-0 mt-0.5">
                {n.sender ? (
                  <UserAvatar user={n.sender} size="sm" />
                ) : (
                  <div className="w-8 h-8 rounded-full bg-accent/20 flex items-center justify-center">
                    {notificationIcon(n.type)}
                  </div>
                )}
              </div>
              <div className="flex-1 min-w-0">
                <p className={`text-sm ${!n.isRead ? 'font-semibold' : ''}`}>
                  {n.title || 'Notification'}
                </p>
                {n.body && (
                  <p className="text-xs text-gray-500 truncate">{n.body}</p>
                )}
                <p className="text-[10px] text-gray-400 mt-0.5">
                  {formatMessageTime(n.createdAt)}
                </p>
              </div>
              {!n.isRead && (
                <div className="w-2 h-2 rounded-full bg-accent flex-shrink-0 mt-1.5" />
              )}
            </div>
          ))
        )}
      </div>
    </div>
  );
}
