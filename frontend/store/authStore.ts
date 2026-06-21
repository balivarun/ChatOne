import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { User } from '@/types';
import { authApi } from '@/lib/api';
import { connectWebSocket, disconnectWebSocket } from '@/lib/websocket';

interface AuthState {
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  setTokens: (access: string, refresh: string) => void;
  fetchMe: () => Promise<void>;
  updateUser: (user: Partial<User>) => void;
  logout: () => Promise<void>;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      accessToken: null,
      refreshToken: null,
      isAuthenticated: false,
      isLoading: false,
      setTokens: (access, refresh) => {
        localStorage.setItem('accessToken', access);
        localStorage.setItem('refreshToken', refresh);
        set({ accessToken: access, refreshToken: refresh, isAuthenticated: true });
        connectWebSocket(access);
      },
      fetchMe: async () => {
        set({ isLoading: true });
        try {
          const { data } = await authApi.getMe();
          set({ user: data.data, isAuthenticated: true });
        } catch {
          set({ isAuthenticated: false });
        } finally {
          set({ isLoading: false });
        }
      },
      updateUser: (partial) =>
        set((s) => ({ user: s.user ? { ...s.user, ...partial } : null })),
      logout: async () => {
        await authApi.logout().catch(() => {});
        disconnectWebSocket();
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        set({
          user: null,
          accessToken: null,
          refreshToken: null,
          isAuthenticated: false,
        });
      },
    }),
    {
      name: 'auth-store',
      partialize: (s) => ({
        accessToken: s.accessToken,
        refreshToken: s.refreshToken,
      }),
    }
  )
);
