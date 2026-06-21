'use client';
import Sidebar from '@/components/sidebar/Sidebar';
import AuthProvider from '@/components/providers/AuthProvider';
import { MessageSquare } from 'lucide-react';
import { useWebSocket } from '@/hooks/useWebSocket';

function ChatPageInner() {
  useWebSocket();
  return (
    <div className="flex h-screen overflow-hidden">
      <div className="w-80 lg:w-96 flex-shrink-0 border-r border-black/10 dark:border-white/10">
        <Sidebar />
      </div>
      <div className="flex-1 flex items-center justify-center bg-chat-bg">
        <div className="flex flex-col items-center gap-4 text-gray-400">
          <MessageSquare className="w-16 h-16 opacity-20" />
          <p className="text-xl font-light">
            Select a conversation to start messaging
          </p>
        </div>
      </div>
    </div>
  );
}

export default function ChatPage() {
  return (
    <AuthProvider>
      <ChatPageInner />
    </AuthProvider>
  );
}
