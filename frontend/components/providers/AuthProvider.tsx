'use client';
import { useEffect } from 'react';
import { useAuthStore } from '@/store/authStore';
import { useNotificationStore } from '@/store/notificationStore';
import { notificationApi } from '@/lib/api';
import { useRouter, usePathname } from 'next/navigation';

const PUBLIC_PATHS = ['/login', '/auth/callback'];

export default function AuthProvider({
  children,
}: {
  children: React.ReactNode;
}) {
  const { fetchMe, isAuthenticated, isLoading } = useAuthStore();
  const { setUnreadCount } = useNotificationStore();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    const token =
      typeof window !== 'undefined'
        ? localStorage.getItem('accessToken')
        : null;
    if (token && !isAuthenticated) {
      fetchMe();
    }
    if (!isLoading && !isAuthenticated && !PUBLIC_PATHS.includes(pathname)) {
      router.replace('/login');
    }
  }, [isAuthenticated, isLoading, pathname, fetchMe, router]);

  useEffect(() => {
    if (!isAuthenticated) return;
    // Request browser notification permission
    if (typeof window !== 'undefined' && 'Notification' in window && Notification.permission === 'default') {
      Notification.requestPermission();
    }
    // Fetch unread notification count
    notificationApi.getUnreadCount().then(({ data }) => {
      setUnreadCount(data.data ?? 0);
    }).catch(() => {});
  }, [isAuthenticated, setUnreadCount]);

  return <>{children}</>;
}
