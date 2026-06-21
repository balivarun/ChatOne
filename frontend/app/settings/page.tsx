'use client';
import { useState, useEffect } from 'react';
import { ArrowLeft, LogOut, BellOff, Shield } from 'lucide-react';
import { useAuthStore } from '@/store/authStore';
import { userApi, notificationApi } from '@/lib/api';
import { useNotificationStore } from '@/store/notificationStore';
import AuthProvider from '@/components/providers/AuthProvider';
import { Button } from '@/components/ui/button';
import ThemeToggle from '@/components/common/ThemeToggle';
import UserAvatar from '@/components/common/UserAvatar';
import { User } from '@/types';
import { useRouter } from 'next/navigation';
import toast from 'react-hot-toast';

function SettingsPageInner() {
  const { user, logout } = useAuthStore();
  const { unreadCount, markAllRead, setUnreadCount } = useNotificationStore();
  const [blockedUsers, setBlockedUsers] = useState<User[]>([]);
  const [loadingBlocked, setLoadingBlocked] = useState(false);
  const router = useRouter();

  useEffect(() => {
    notificationApi
      .getUnreadCount()
      .then(({ data }) => setUnreadCount(data.data ?? data.count ?? 0))
      .catch(() => {});

    setLoadingBlocked(true);
    userApi
      .getBlockedUsers()
      .then(({ data }) => {
        const users: User[] = data.data || data;
        setBlockedUsers(Array.isArray(users) ? users : []);
      })
      .catch(() => {})
      .finally(() => setLoadingBlocked(false));
  }, [setUnreadCount]);

  const handleUnblock = async (userId: string) => {
    try {
      await userApi.unblockUser(userId);
      setBlockedUsers((prev) => prev.filter((u) => u.id !== userId));
      toast.success('User unblocked');
    } catch {
      toast.error('Failed to unblock user');
    }
  };

  const handleMarkAllRead = async () => {
    try {
      await notificationApi.markAllRead();
      markAllRead();
      toast.success('All notifications marked as read');
    } catch {
      toast.error('Failed to mark notifications as read');
    }
  };

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
    <div className="min-h-screen bg-background">
      <div className="max-w-lg mx-auto py-8 px-4">
        <div className="flex items-center gap-3 mb-8">
          <Button variant="ghost" size="icon" onClick={() => router.back()}>
            <ArrowLeft className="h-5 w-5" />
          </Button>
          <h1 className="text-2xl font-bold">Settings</h1>
        </div>

        {/* Profile card */}
        <div
          className="bg-white dark:bg-sidebar rounded-2xl shadow-sm p-4 flex items-center gap-4 mb-4 cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors"
          onClick={() => router.push('/profile')}
        >
          <UserAvatar user={user} size="lg" />
          <div>
            <p className="font-semibold text-lg">{user.displayName}</p>
            <p className="text-sm text-gray-400">{user.bio || 'No bio set'}</p>
          </div>
        </div>

        {/* Appearance */}
        <div className="bg-white dark:bg-sidebar rounded-2xl shadow-sm p-4 mb-4">
          <h2 className="font-semibold mb-3">Appearance</h2>
          <div className="flex items-center justify-between">
            <span className="text-sm">Theme</span>
            <ThemeToggle />
          </div>
        </div>

        {/* Notifications */}
        <div className="bg-white dark:bg-sidebar rounded-2xl shadow-sm p-4 mb-4">
          <div className="flex items-center justify-between mb-3">
            <h2 className="font-semibold">Notifications</h2>
            {unreadCount > 0 && (
              <Button variant="ghost" size="sm" onClick={handleMarkAllRead}>
                Mark all read
              </Button>
            )}
          </div>
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm">Unread notifications</p>
              <p className="text-xs text-gray-400">{unreadCount} unread</p>
            </div>
            <BellOff className="w-5 h-5 text-gray-400" />
          </div>
        </div>

        {/* Blocked Users */}
        <div className="bg-white dark:bg-sidebar rounded-2xl shadow-sm p-4 mb-4">
          <div className="flex items-center gap-2 mb-3">
            <Shield className="w-4 h-4" />
            <h2 className="font-semibold">Blocked Users</h2>
          </div>
          {loadingBlocked ? (
            <div className="flex justify-center py-4">
              <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-accent" />
            </div>
          ) : blockedUsers.length === 0 ? (
            <p className="text-sm text-gray-400 text-center py-4">
              No blocked users
            </p>
          ) : (
            <div className="space-y-2">
              {blockedUsers.map((u) => (
                <div
                  key={u.id}
                  className="flex items-center justify-between"
                >
                  <div className="flex items-center gap-3">
                    <UserAvatar user={u} size="sm" />
                    <div>
                      <p className="text-sm font-medium">{u.displayName}</p>
                      <p className="text-xs text-gray-400">{u.email}</p>
                    </div>
                  </div>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handleUnblock(u.id)}
                  >
                    Unblock
                  </Button>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Account */}
        <div className="bg-white dark:bg-sidebar rounded-2xl shadow-sm p-4">
          <h2 className="font-semibold mb-3">Account</h2>
          <Button
            variant="destructive"
            className="w-full"
            onClick={handleLogout}
          >
            <LogOut className="mr-2 h-4 w-4" />
            Logout
          </Button>
        </div>
      </div>
    </div>
  );
}

export default function SettingsPage() {
  return (
    <AuthProvider>
      <SettingsPageInner />
    </AuthProvider>
  );
}
