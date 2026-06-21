'use client';
import { useEffect } from 'react';
import { useAuthStore } from '@/store/authStore';
import { useRouter, usePathname } from 'next/navigation';

const PUBLIC_PATHS = ['/login', '/auth/callback'];

export default function AuthProvider({
  children,
}: {
  children: React.ReactNode;
}) {
  const { fetchMe, isAuthenticated, isLoading } = useAuthStore();
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

  return <>{children}</>;
}
