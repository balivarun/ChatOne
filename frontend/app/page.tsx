'use client';
import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/store/authStore';

export default function HomePage() {
  const { isAuthenticated, isLoading, fetchMe } = useAuthStore();
  const router = useRouter();

  useEffect(() => {
    fetchMe().then(() => {
      const { isAuthenticated: authed } = useAuthStore.getState();
      router.replace(authed ? '/chat' : '/login');
    });
  }, [fetchMe, router]);

  return (
    <div className="flex items-center justify-center h-screen">
      <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-accent" />
    </div>
  );
}
