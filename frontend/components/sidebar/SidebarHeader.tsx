'use client';
import { useState } from 'react';
import { useRouter } from 'next/navigation';
import {
  MessageSquarePlus,
  Users,
  Bell,
  Settings,
  LogOut,
  MoreVertical,
} from 'lucide-react';
import { useAuthStore } from '@/store/authStore';
import { useNotificationStore } from '@/store/notificationStore';
import UserAvatar from '@/components/common/UserAvatar';
import ThemeToggle from '@/components/common/ThemeToggle';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { Button } from '@/components/ui/button';
import CreateGroupModal from '@/components/group/CreateGroupModal';
import toast from 'react-hot-toast';

export default function SidebarHeader() {
  const { user, logout } = useAuthStore();
  const { unreadCount } = useNotificationStore();
  const router = useRouter();
  const [groupModalOpen, setGroupModalOpen] = useState(false);

  const handleLogout = async () => {
    try {
      await logout();
      router.replace('/login');
    } catch {
      toast.error('Failed to logout');
    }
  };

  if (!user) return null;

  return (
    <div className="flex items-center justify-between px-3 py-3 bg-sidebar border-b border-black/10 dark:border-white/10">
      <div className="flex items-center gap-2">
        <UserAvatar user={user} size="md" showStatus />
        <span className="font-semibold text-sm hidden lg:block truncate max-w-[100px]">
          {user.displayName}
        </span>
      </div>

      <div className="flex items-center gap-1">
        <ThemeToggle />

        <Button
          variant="ghost"
          size="icon"
          onClick={() => setGroupModalOpen(true)}
          title="New Group"
        >
          <Users className="h-5 w-5" />
        </Button>

        <Button
          variant="ghost"
          size="icon"
          onClick={() => router.push('/chat')}
          title="New Chat"
        >
          <MessageSquarePlus className="h-5 w-5" />
        </Button>

        <Button
          variant="ghost"
          size="icon"
          className="relative"
          onClick={() => router.push('/settings')}
          title="Notifications"
        >
          <Bell className="h-5 w-5" />
          {unreadCount > 0 && (
            <span className="absolute -top-1 -right-1 bg-accent text-white text-xs rounded-full w-4 h-4 flex items-center justify-center">
              {unreadCount > 9 ? '9+' : unreadCount}
            </span>
          )}
        </Button>

        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="icon">
              <MoreVertical className="h-5 w-5" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-48">
            <DropdownMenuItem onClick={() => router.push('/profile')}>
              <Settings className="mr-2 h-4 w-4" />
              Profile
            </DropdownMenuItem>
            <DropdownMenuItem onClick={() => router.push('/settings')}>
              <Settings className="mr-2 h-4 w-4" />
              Settings
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem
              onClick={handleLogout}
              className="text-red-500 focus:text-red-500"
            >
              <LogOut className="mr-2 h-4 w-4" />
              Logout
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>

      <CreateGroupModal
        open={groupModalOpen}
        onClose={() => setGroupModalOpen(false)}
      />
    </div>
  );
}
