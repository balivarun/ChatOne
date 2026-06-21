export interface User {
  id: string;
  email: string;
  displayName: string;
  avatarUrl?: string;
  bio?: string;
  isOnline: boolean;
  lastSeen?: string;
  createdAt: string;
}

export type MessageType = 'TEXT' | 'IMAGE' | 'FILE' | 'VOICE' | 'SYSTEM';
export type ConversationType = 'DIRECT' | 'GROUP';
export type NotificationType =
  | 'NEW_MESSAGE'
  | 'GROUP_INVITE'
  | 'GROUP_MESSAGE'
  | 'MEMBER_ADDED'
  | 'MEMBER_REMOVED';
export type GroupRole = 'ADMIN' | 'MEMBER';

export interface Attachment {
  id: string;
  url: string;
  fileName?: string;
  fileType?: string;
  fileSize?: number;
}

export interface Message {
  id: string;
  conversationId: string;
  sender: User;
  content?: string;
  type: MessageType;
  replyTo?: Message;
  isEdited: boolean;
  isDeleted: boolean;
  attachments: Attachment[];
  readBy: User[];
  createdAt: string;
  updatedAt: string;
}

export interface Group {
  id: string;
  name: string;
  description?: string;
  avatarUrl?: string;
  createdBy: User;
  memberCount: number;
  role: GroupRole;
  createdAt: string;
}

export interface GroupMember {
  user: User;
  role: GroupRole;
  joinedAt: string;
}

export interface Conversation {
  id: string;
  type: ConversationType;
  otherUser?: User;
  group?: Group;
  lastMessage?: Message;
  unreadCount: number;
  isArchived: boolean;
  isPinned: boolean;
  isMuted: boolean;
  updatedAt: string;
}

export interface Notification {
  id: string;
  sender?: User;
  type: NotificationType;
  title?: string;
  body?: string;
  isRead: boolean;
  referenceId?: string;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface TypingPayload {
  conversationId: string;
  userId: string;
  displayName: string;
  isTyping: boolean;
}

export interface WsMessage {
  type:
    | 'NEW_MESSAGE'
    | 'EDIT_MESSAGE'
    | 'DELETE_MESSAGE'
    | 'READ_RECEIPT'
    | 'TYPING'
    | 'USER_STATUS'
    | 'NOTIFICATION';
  payload: unknown;
}
