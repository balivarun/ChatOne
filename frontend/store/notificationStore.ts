import { create } from 'zustand';
import { Notification } from '@/types';

interface NotificationState {
  notifications: Notification[];
  unreadCount: number;
  setNotifications: (n: Notification[]) => void;
  addNotification: (n: Notification) => void;
  setUnreadCount: (c: number) => void;
  markRead: (id: string) => void;
  markAllRead: () => void;
}

export const useNotificationStore = create<NotificationState>((set) => ({
  notifications: [],
  unreadCount: 0,

  setNotifications: (notifications) => set({ notifications }),

  addNotification: (n) =>
    set((s) => ({
      notifications: [n, ...s.notifications],
      unreadCount: s.unreadCount + 1,
    })),

  setUnreadCount: (unreadCount) => set({ unreadCount }),

  markRead: (id) =>
    set((s) => ({
      notifications: s.notifications.map((n) =>
        n.id === id ? { ...n, isRead: true } : n
      ),
      unreadCount: Math.max(0, s.unreadCount - 1),
    })),

  markAllRead: () =>
    set((s) => ({
      notifications: s.notifications.map((n) => ({ ...n, isRead: true })),
      unreadCount: 0,
    })),
}));
