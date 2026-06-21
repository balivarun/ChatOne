'use client';
import { useState, useEffect, useRef, useCallback } from 'react';
import { Search, X } from 'lucide-react';
import { userApi, conversationApi } from '@/lib/api';
import { useChatStore } from '@/store/chatStore';
import { User, Conversation } from '@/types';
import UserAvatar from '@/components/common/UserAvatar';
import { useRouter } from 'next/navigation';
import toast from 'react-hot-toast';

export default function SearchBar() {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<User[]>([]);
  const [isOpen, setIsOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const { addConversation, setActiveConversation } = useChatStore();
  const router = useRouter();

  const search = useCallback(async (q: string) => {
    if (!q.trim()) {
      setResults([]);
      setIsOpen(false);
      return;
    }
    setLoading(true);
    try {
      const { data } = await userApi.searchUsers(q);
      const users: User[] = data.data?.content || data.content || data.data || [];
      setResults(users);
      setIsOpen(true);
    } catch {
      toast.error('Search failed');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => search(query), 300);
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    };
  }, [query, search]);

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleSelectUser = async (user: User) => {
    try {
      const { data } = await conversationApi.create(user.id);
      const conv: Conversation = data.data || data;
      addConversation(conv);
      setActiveConversation(conv.id);
      router.push(`/chat/${conv.id}`);
    } catch {
      toast.error('Failed to open conversation');
    } finally {
      setQuery('');
      setIsOpen(false);
    }
  };

  const clearSearch = () => {
    setQuery('');
    setResults([]);
    setIsOpen(false);
  };

  return (
    <div ref={containerRef} className="relative px-3 py-2">
      <div className="relative flex items-center">
        <Search className="absolute left-3 h-4 w-4 text-gray-400 pointer-events-none" />
        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onFocus={() => query.trim() && setIsOpen(true)}
          placeholder="Search or start new chat"
          className="w-full pl-9 pr-8 py-2 text-sm bg-black/5 dark:bg-white/5 rounded-lg focus:outline-none focus:bg-black/10 dark:focus:bg-white/10 transition-colors"
        />
        {query && (
          <button
            onClick={clearSearch}
            className="absolute right-3 text-gray-400 hover:text-foreground"
          >
            <X className="h-4 w-4" />
          </button>
        )}
      </div>

      {isOpen && (
        <div className="absolute left-3 right-3 top-full mt-1 bg-white dark:bg-gray-900 rounded-lg shadow-lg border border-gray-200 dark:border-gray-700 z-50 max-h-64 overflow-y-auto">
          {loading ? (
            <div className="flex items-center justify-center py-4">
              <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-accent" />
            </div>
          ) : results.length === 0 ? (
            <div className="py-4 text-center text-sm text-gray-400">
              No users found
            </div>
          ) : (
            results.map((user) => (
              <div
                key={user.id}
                onClick={() => handleSelectUser(user)}
                className="flex items-center gap-3 px-3 py-2 hover:bg-gray-50 dark:hover:bg-gray-800 cursor-pointer transition-colors"
              >
                <UserAvatar user={user} size="sm" showStatus />
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium truncate">{user.displayName}</p>
                  <p className="text-xs text-gray-400 truncate">{user.email}</p>
                </div>
                {user.isOnline && (
                  <span className="text-xs text-green-500">Online</span>
                )}
              </div>
            ))
          )}
        </div>
      )}
    </div>
  );
}
