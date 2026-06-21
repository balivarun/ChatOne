'use client';
import { useEffect } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { useAuthStore } from '@/store/authStore';

export default function AuthCallbackPage() {
  const router = useRouter();
  const params = useSearchParams();
  const { setTokens, fetchMe } = useAuthStore();

  useEffect(() => {
    const token = params.get('token');
    const refresh = params.get('refresh');
    if (token && refresh) {
      setTokens(token, refresh);
      fetchMe().then(() => router.replace('/chat'));
    } else {
      router.replace('/login');
    }
  }, [params, setTokens, fetchMe, router]);

  return (
    <div className="flex items-center justify-center h-screen">
      <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-accent" />
    </div>
  );
}
