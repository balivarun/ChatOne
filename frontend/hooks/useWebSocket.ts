import { useEffect } from 'react';
import { useAuthStore } from '@/store/authStore';
import { useChatStore } from '@/store/chatStore';
import { useNotificationStore } from '@/store/notificationStore';
import { subscribe } from '@/lib/websocket';
import { Message, Notification as AppNotification } from '@/types';

export function useWebSocket() {
  const { user } = useAuthStore();
  const { addMessage, updateMessage, deleteMessage, setTyping } = useChatStore();
  const { addNotification } = useNotificationStore();

  useEffect(() => {
    if (!user) return;

    subscribe(`/user/queue/messages`, (stompMsg) => {
      try {
        const msg: Message = JSON.parse(stompMsg.body);
        addMessage(msg);
      } catch {
        // ignore
      }
    });

    subscribe(`/user/queue/notifications`, (stompMsg) => {
      try {
        const n: AppNotification = JSON.parse(stompMsg.body);
        addNotification(n);
        if (typeof window !== 'undefined' && Notification.permission === 'granted') {
          new Notification(n.title || 'ConnectChat', {
            body: n.body,
            icon: '/icon.png',
          });
        }
      } catch {
        // ignore
      }
    });

    subscribe(`/user/queue/typing`, (stompMsg) => {
      try {
        const payload = JSON.parse(stompMsg.body) as {
          conversationId: string;
          userId: string;
          isTyping: boolean;
        };
        setTyping(payload.conversationId, payload.userId, payload.isTyping);
      } catch {
        // ignore
      }
    });

    subscribe(`/user/queue/message-updates`, (stompMsg) => {
      try {
        const data = JSON.parse(stompMsg.body) as {
          type: string;
          payload: unknown;
        };
        if (data.type === 'EDIT_MESSAGE') {
          updateMessage(data.payload as Message);
        } else if (data.type === 'DELETE_MESSAGE') {
          const { messageId, conversationId } = data.payload as {
            messageId: string;
            conversationId: string;
          };
          deleteMessage(messageId, conversationId);
        }
      } catch {
        // ignore
      }
    });
  }, [user?.id, addMessage, addNotification, setTyping, updateMessage, deleteMessage]);
}
