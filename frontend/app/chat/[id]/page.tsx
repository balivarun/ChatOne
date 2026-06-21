'use client';
import { use } from 'react';
import Sidebar from '@/components/sidebar/Sidebar';
import ChatWindow from '@/components/chat/ChatWindow';
import AuthProvider from '@/components/providers/AuthProvider';
import { useWebSocket } from '@/hooks/useWebSocket';

function ConversationPageInner({ id }: { id: string }) {
  useWebSocket();
  return (
    <div className="flex h-screen overflow-hidden">
      <div className="hidden md:flex w-80 lg:w-96 flex-shrink-0 border-r border-black/10 dark:border-white/10">
        <Sidebar />
      </div>
      <div className="flex-1 min-w-0">
        <ChatWindow conversationId={id} />
      </div>
    </div>
  );
}

export default function ConversationPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  return (
    <AuthProvider>
      <ConversationPageInner id={id} />
    </AuthProvider>
  );
}
