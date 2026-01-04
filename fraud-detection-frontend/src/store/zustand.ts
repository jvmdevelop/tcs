import { create } from "zustand";
import { devtools } from "zustand/middleware";
import type { DashboardStats, Transaction } from "@/lib/types";

interface DashboardStore {
  stats: DashboardStats | null;
  recentTransactions: Transaction[];
  loading: boolean;
  error: string | null;
  setStats: (stats: DashboardStats) => void;
  setRecentTransactions: (transactions: Transaction[]) => void;
  setLoading: (loading: boolean) => void;
  setError: (error: string | null) => void;
}

export const useDashboardStore = create<DashboardStore>()(
  devtools(
    (set) => ({
      stats: null,
      recentTransactions: [],
      loading: false,
      error: null,
      setStats: (stats) => set({ stats }),
      setRecentTransactions: (recentTransactions) => set({ recentTransactions }),
      setLoading: (loading) => set({ loading }),
      setError: (error) => set({ error }),
    }),
    { name: "DashboardStore" }
  )
);

interface UIStore {
  sidebarOpen: boolean;
  theme: "light" | "dark";
  toggleSidebar: () => void;
  setTheme: (theme: "light" | "dark") => void;
}

export const useUIStore = create<UIStore>()(
  devtools(
    (set) => ({
      sidebarOpen: true,
      theme: "light",
      toggleSidebar: () => set((state) => ({ sidebarOpen: !state.sidebarOpen })),
      setTheme: (theme) => set({ theme }),
    }),
    { name: "UIStore" }
  )
);
