"use client";

import { Sidebar } from "./Sidebar";
import { useUIStore } from "@/store/zustand";
import clsx from "clsx";

export function DashboardLayout({ children }: { children: React.ReactNode }) {
  const { sidebarOpen } = useUIStore();

  return (
    <div className="flex h-screen bg-gray-100">
      <Sidebar />
      <main
        className={clsx(
          "flex-1 overflow-auto transition-all duration-200",
          {
            "lg:ml-64": sidebarOpen,
            "lg:ml-0": !sidebarOpen,
          }
        )}
      >
        <div className="container mx-auto px-4 py-8 lg:px-8">
          {children}
        </div>
      </main>
    </div>
  );
}
