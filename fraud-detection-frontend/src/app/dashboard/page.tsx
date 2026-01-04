"use client";

import { useQuery } from "@tanstack/react-query";
import { dashboardApi } from "@/lib/api";
import { DashboardLayout } from "@/components/layout/DashboardLayout";
import { useDashboardStore } from "@/store/zustand";
import { useEffect } from "react";
import { AlertTriangle, CheckCircle, Clock, FileText } from "lucide-react";
import clsx from "clsx";
import Link from "next/link";
import { format } from "date-fns";

export default function DashboardPage() {
  const { setStats, setRecentTransactions, setLoading } = useDashboardStore();

  const { data: statsData, isLoading: statsLoading } = useQuery({
    queryKey: ["dashboard-stats"],
    queryFn: async () => {
      const response = await dashboardApi.getStats();
      return response.data;
    },
  });

  const { data: recentData, isLoading: recentLoading } = useQuery({
    queryKey: ["recent-transactions"],
    queryFn: async () => {
      const response = await dashboardApi.getRecentTransactions(10);
      return response.data;
    },
  });

  useEffect(() => {
    if (statsData) {
      setStats(statsData);
    }
    if (recentData) {
      setRecentTransactions(recentData);
    }
    setLoading(statsLoading || recentLoading);
  }, [statsData, recentData, statsLoading, recentLoading, setStats, setRecentTransactions, setLoading]);

  const stats = [
    {
      name: "Processed",
      value: statsData?.totalProcessed || 0,
      icon: CheckCircle,
      color: "bg-green-500",
    },
    {
      name: "Alerted",
      value: statsData?.totalAlerted || 0,
      icon: AlertTriangle,
      color: "bg-red-500",
    },
    {
      name: "Reviewed",
      value: statsData?.totalReviewed || 0,
      icon: FileText,
      color: "bg-blue-500",
    },
    {
      name: "Processing",
      value: statsData?.totalProcessing || 0,
      icon: Clock,
      color: "bg-yellow-500",
    },
  ];

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Dashboard</h1>
          <p className="mt-2 text-sm text-gray-600">
            Overview of fraud detection system
          </p>
        </div>

        <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
          {stats.map((stat) => {
            const Icon = stat.icon;
            return (
              <div
                key={stat.name}
                className="bg-white rounded-lg shadow p-6 hover:shadow-lg transition-shadow"
              >
                <div className="flex items-center">
                  <div className={clsx("p-3 rounded-lg", stat.color)}>
                    <Icon className="h-6 w-6 text-white" />
                  </div>
                  <div className="ml-4">
                    <p className="text-sm font-medium text-gray-600">{stat.name}</p>
                    <p className="text-2xl font-bold text-gray-900">{stat.value}</p>
                  </div>
                </div>
              </div>
            );
          })}
        </div>

        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold text-gray-900">Active Rules</h2>
            <span className="px-3 py-1 bg-primary-100 text-primary-800 rounded-full text-sm font-medium">
              {statsData?.activeRulesCount || 0} rules
            </span>
          </div>
        </div>

        <div className="bg-white rounded-lg shadow">
          <div className="px-6 py-4 border-b border-gray-200">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold text-gray-900">
                Recent Transactions
              </h2>
              <Link
                href="/transactions"
                className="text-sm text-primary-600 hover:text-primary-700 font-medium"
              >
                View all
              </Link>
            </div>
          </div>
          <div className="divide-y divide-gray-200">
            {recentLoading ? (
              <div className="p-6 text-center text-gray-500">Loading...</div>
            ) : recentData && recentData.length > 0 ? (
              recentData.map((transaction) => (
                <Link
                  key={transaction.id}
                  href={`/transactions/${transaction.id}`}
                  className="block px-6 py-4 hover:bg-gray-50 transition-colors"
                >
                  <div className="flex items-center justify-between">
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-gray-900 truncate">
                        {transaction.from} → {transaction.to}
                      </p>
                      <p className="text-sm text-gray-500">
                        {format(new Date(transaction.timestamp), "PPpp")}
                      </p>
                    </div>
                    <div className="ml-4 flex items-center space-x-4">
                      <span className="text-lg font-semibold text-gray-900">
                        ${transaction.amount.toFixed(2)}
                      </span>
                      <span
                        className={clsx(
                          "px-3 py-1 rounded-full text-xs font-medium",
                          {
                            "bg-green-100 text-green-800":
                              transaction.status === "PROCESSED",
                            "bg-red-100 text-red-800":
                              transaction.status === "ALERTED",
                            "bg-blue-100 text-blue-800":
                              transaction.status === "REVIEWED",
                            "bg-yellow-100 text-yellow-800":
                              transaction.status === "PROCESSING",
                          }
                        )}
                      >
                        {transaction.status}
                      </span>
                    </div>
                  </div>
                </Link>
              ))
            ) : (
              <div className="p-6 text-center text-gray-500">
                No transactions found
              </div>
            )}
          </div>
        </div>
      </div>
    </DashboardLayout>
  );
}
