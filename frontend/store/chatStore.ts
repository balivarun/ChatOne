import { create } from 'zustand';
import { Conversation, Message } from '@/types';

interface ChatState {
  conversations: Conversation[];
  activeConversationId: string | null;
  messages: Record<string, Message[]>;
  typingUsers: Record<string, Set<string>>;
  hasMoreMessages: Record<string, boolean>;
  setConversations: (convs: Conversation[]) => void;
  addConversation: (conv: Conversation) => void;
  updateConversation: (id: string, partial: Partial<Conversation>) => void;
  setActiveConversation: (id: string | null) => void;
  setMessages: (convId: string, messages: Message[], prepend?: boolean) => void;
  addMessage: (message: Message) => void;
  updateMessage: (message: Message) => void;
  deleteMessage: (messageId: string, convId: string) => void;
  setTyping: (convId: string, userId: string, isTyping: boolean) => void;
  setHasMore: (convId: string, hasMore: boolean) => void;
  incrementUnread: (convId: string) => void;
  clearUnread: (convId: string) => void;
}

export const useChatStore = create<ChatState>((set) => ({
  conversations: [],
  activeConversationId: null,
  messages: {},
  typingUsers: {},
  hasMoreMessages: {},

  setConversations: (conversations) => set({ conversations }),

  addConversation: (conv) =>
    set((s) => ({
      conversations: [conv, ...s.conversations.filter((c) => c.id !== conv.id)],
    })),

  updateConversation: (id, partial) =>
    set((s) => ({
      conversations: s.conversations.map((c) =>
        c.id === id ? { ...c, ...partial } : c
      ),
    })),

  setActiveConversation: (id) => set({ activeConversationId: id }),

  setMessages: (convId, messages, prepend = false) =>
    set((s) => ({
      messages: {
        ...s.messages,
        [convId]: prepend
          ? [...messages, ...(s.messages[convId] || [])]
          : messages,
      },
    })),

  addMessage: (msg) =>
    set((s) => {
      const existing = s.messages[msg.conversationId] || [];
      if (existing.find((m) => m.id === msg.id)) return s;
      return {
        messages: {
          ...s.messages,
          [msg.conversationId]: [...existing, msg],
        },
        conversations: s.conversations
          .map((c) =>
            c.id === msg.conversationId
              ? { ...c, lastMessage: msg, updatedAt: msg.createdAt }
              : c
          )
          .sort(
            (a, b) =>
              new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime()
          ),
      };
    }),

  updateMessage: (msg) =>
    set((s) => ({
      messages: {
        ...s.messages,
        [msg.conversationId]: (s.messages[msg.conversationId] || []).map((m) =>
          m.id === msg.id ? msg : m
        ),
      },
    })),

  deleteMessage: (messageId, convId) =>
    set((s) => ({
      messages: {
        ...s.messages,
        [convId]: (s.messages[convId] || []).map((m) =>
          m.id === messageId
            ? { ...m, isDeleted: true, content: undefined }
            : m
        ),
      },
    })),

  setTyping: (convId, userId, isTyping) =>
    set((s) => {
      const current = new Set(s.typingUsers[convId] || []);
      isTyping ? current.add(userId) : current.delete(userId);
      return { typingUsers: { ...s.typingUsers, [convId]: current } };
    }),

  setHasMore: (convId, hasMore) =>
    set((s) => ({
      hasMoreMessages: { ...s.hasMoreMessages, [convId]: hasMore },
    })),

  incrementUnread: (convId) =>
    set((s) => ({
      conversations: s.conversations.map((c) =>
        c.id === convId ? { ...c, unreadCount: c.unreadCount + 1 } : c
      ),
    })),

  clearUnread: (convId) =>
    set((s) => ({
      conversations: s.conversations.map((c) =>
        c.id === convId ? { ...c, unreadCount: 0 } : c
      ),
    })),
}));
