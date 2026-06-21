import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface UIState {
  theme: 'light' | 'dark' | 'system';
  sidebarOpen: boolean;
  searchOpen: boolean;
  setTheme: (t: 'light' | 'dark' | 'system') => void;
  toggleSidebar: () => void;
  setSearchOpen: (open: boolean) => void;
}

export const useUIStore = create<UIState>()(
  persist(
    (set) => ({
      theme: 'system',
      sidebarOpen: true,
      searchOpen: false,
      setTheme: (theme) => set({ theme }),
      toggleSidebar: () => set((s) => ({ sidebarOpen: !s.sidebarOpen })),
      setSearchOpen: (searchOpen) => set({ searchOpen }),
    }),
    { name: 'ui-store' }
  )
);
