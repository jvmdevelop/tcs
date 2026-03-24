"use client";

import { useEffect, useState } from "react";
import { observer } from "mobx-react-lite";
import { transactionStore } from "@/store/mobx";
import { DashboardLayout } from "@/components/layout/DashboardLayout";
import { EStatus } from "@/lib/types";
import clsx from "clsx";
import Link from "next/link";
import { format } from "date-fns";
import { ChevronLeft, ChevronRight, Filter } from "lucide-react";

const TransactionsPage = observer(() => {
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
    transactionStore.fetchTransactions();
  }, []);

  useEffect(() => {
    if (mounted) {
      transactionStore.fetchTransactions();
    }
  }, [transactionStore.currentPage, transactionStore.statusFilter, mounted]);

  if (!mounted) {
    return null;
  }

  const handleStatusFilter = (status: EStatus | undefined) => {
    transactionStore.setStatusFilter(status);
    transactionStore.setPage(0);
  };

  const handlePageChange = (newPage: number) => {
    transactionStore.setPage(newPage);
  };

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">Transactions</h1>
            <p className="mt-2 text-sm text-gray-600">
              Monitor and review all transactions
            </p>
          </div>
        </div>

        <div className="bg-white rounded-lg shadow p-4">
          <div className="flex items-center space-x-2">
            <Filter className="h-5 w-5 text-gray-400" />
            <span className="text-sm font-medium text-gray-700">Filter by status:</span>
            <button
              onClick={() => handleStatusFilter(undefined)}
              className={clsx(
                "px-3 py-1 rounded-md text-sm font-medium transition-colors",
                !transactionStore.statusFilter
                  ? "bg-primary-600 text-white"
                  : "bg-gray-100 text-gray-700 hover:bg-gray-200"
              )}
            >
              All
            </button>
            {Object.values(EStatus).map((status) => (
              <button
                key={status}
                onClick={() => handleStatusFilter(status)}
                className={clsx(
                  "px-3 py-1 rounded-md text-sm font-medium transition-colors",
                  transactionStore.statusFilter === status
                    ? "bg-primary-600 text-white"
                    : "bg-gray-100 text-gray-700 hover:bg-gray-200"
                )}
              >
                {status}
              </button>
            ))}
          </div>
        </div>

        <div className="bg-white rounded-lg shadow overflow-hidden">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  From → To
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Amount
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Type
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Timestamp
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Status
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {transactionStore.loading ? (
                <tr>
                  <td colSpan={6} className="px-6 py-4 text-center text-gray-500">
                    Loading...
                  </td>
                </tr>
              ) : transactionStore.transactions.length > 0 ? (
                transactionStore.transactions.map((transaction) => (
                  <tr key={transaction.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {transaction.from} → {transaction.to}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-semibold text-gray-900">
                      ${transaction.amount.toFixed(2)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {transaction.type}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {format(new Date(transaction.timestamp), "PPpp")}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span
                        className={clsx(
                          "px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full",
                          {
                            "bg-green-100 text-green-800":
                              transaction.status === EStatus.PROCESSED,
                            "bg-red-100 text-red-800":
                              transaction.status === EStatus.ALERTED,
                            "bg-blue-100 text-blue-800":
                              transaction.status === EStatus.REVIEWED,
                            "bg-yellow-100 text-yellow-800":
                              transaction.status === EStatus.PROCESSING,
                          }
                        )}
                      >
                        {transaction.status}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                      <Link
                        href={`/transactions/${transaction.id}`}
                        className="text-primary-600 hover:text-primary-900"
                      >
                        View Details
                      </Link>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={6} className="px-6 py-4 text-center text-gray-500">
                    No transactions found
                  </td>
                </tr>
              )}
            </tbody>
          </table>

          {transactionStore.totalPages > 1 && (
            <div className="bg-white px-4 py-3 flex items-center justify-between border-t border-gray-200 sm:px-6">
              <div className="flex-1 flex justify-between sm:hidden">
                <button
                  onClick={() => handlePageChange(transactionStore.currentPage - 1)}
                  disabled={transactionStore.currentPage === 0}
                  className="relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50"
                >
                  Previous
                </button>
                <button
                  onClick={() => handlePageChange(transactionStore.currentPage + 1)}
                  disabled={transactionStore.currentPage >= transactionStore.totalPages - 1}
                  className="ml-3 relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50"
                >
                  Next
                </button>
              </div>
              <div className="hidden sm:flex-1 sm:flex sm:items-center sm:justify-between">
                <div>
                  <p className="text-sm text-gray-700">
                    Showing page <span className="font-medium">{transactionStore.currentPage + 1}</span> of{" "}
                    <span className="font-medium">{transactionStore.totalPages}</span> ({transactionStore.totalElements} total)
                  </p>
                </div>
                <div>
                  <nav className="relative z-0 inline-flex rounded-md shadow-sm -space-x-px">
                    <button
                      onClick={() => handlePageChange(transactionStore.currentPage - 1)}
                      disabled={transactionStore.currentPage === 0}
                      className="relative inline-flex items-center px-2 py-2 rounded-l-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50"
                    >
                      <ChevronLeft className="h-5 w-5" />
                    </button>
                    <button
                      onClick={() => handlePageChange(transactionStore.currentPage + 1)}
                      disabled={transactionStore.currentPage >= transactionStore.totalPages - 1}
                      className="relative inline-flex items-center px-2 py-2 rounded-r-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50"
                    >
                      <ChevronRight className="h-5 w-5" />
                    </button>
                  </nav>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </DashboardLayout>
  );
});

export default TransactionsPage;
