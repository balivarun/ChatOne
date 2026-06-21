'use client';
import { useState, useEffect } from 'react';
import { useChatStore } from '@/store/chatStore';
import { conversationApi, groupApi } from '@/lib/api';
import ConversationItem from './ConversationItem';
import GroupItem from './GroupItem';
import SearchBar from './SearchBar';
import SidebarHeader from './SidebarHeader';
import { Group } from '@/types';
import toast from 'react-hot-toast';

export default function Sidebar() {
  const { conversations, setConversations } = useChatStore();
  const [tab, setTab] = useState<'chats' | 'groups'>('chats');
  const [groups, setGroups] = useState<Group[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      conversationApi
        .getAll()
        .then((r) => {
          const convs = r.data.data || r.data;
          setConversations(Array.isArray(convs) ? convs : []);
        })
        .catch(() => toast.error('Failed to load conversations')),
      groupApi
        .getMine()
        .then((r) => {
          const grps = r.data.data || r.data;
          setGroups(Array.isArray(grps) ? grps : []);
        })
        .catch(() => toast.error('Failed to load groups')),
    ]).finally(() => setLoading(false));
  }, [setConversations]);

  return (
    <div className="flex flex-col h-full bg-sidebar border-r border-black/10 dark:border-white/10 w-full">
      <SidebarHeader />
      <SearchBar />
      <div className="flex border-b border-black/10 dark:border-white/10">
        {(['chats', 'groups'] as const).map((t) => (
          <button
            key={t}
            onClick={() => setTab(t)}
            className={`flex-1 py-3 text-sm font-medium capitalize transition-colors ${
              tab === t
                ? 'text-accent border-b-2 border-accent'
                : 'text-gray-500 hover:text-foreground'
            }`}
          >
            {t}
          </button>
        ))}
      </div>
      <div className="flex-1 overflow-y-auto">
        {loading ? (
          <div className="flex items-center justify-center h-full">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-accent" />
          </div>
        ) : tab === 'chats' ? (
          conversations.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-full text-gray-400 gap-2 p-8 text-center">
              <p className="text-lg font-medium">No conversations yet</p>
              <p className="text-sm">Search for a user to start chatting</p>
            </div>
          ) : (
            conversations.map((c) => (
              <ConversationItem key={c.id} conversation={c} />
            ))
          )
        ) : groups.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full text-gray-400 gap-2 p-8 text-center">
            <p className="text-lg font-medium">No groups yet</p>
            <p className="text-sm">Create a group to get started</p>
          </div>
        ) : (
          groups.map((g) => <GroupItem key={g.id} group={g} />)
        )}
      </div>
    </div>
  );
}
