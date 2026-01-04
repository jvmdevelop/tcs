"use client";

import { useQuery } from "@tanstack/react-query";
import { transactionsApi } from "@/lib/api";
import { DashboardLayout } from "@/components/layout/DashboardLayout";
import { useParams, useRouter } from "next/navigation";
import { format } from "date-fns";
import { ArrowLeft, CheckCircle } from "lucide-react";
import clsx from "clsx";
import toast from "react-hot-toast";

export default function TransactionDetailPage() {
  const params = useParams();
  const router = useRouter();
  const id = params.id as string;

  const { data: transaction, isLoading, refetch } = useQuery({
    queryKey: ["transaction", id],
    queryFn: async () => {
      const response = await transactionsApi.getById(id);
      return response.data;
    },
  });

  const handleReview = async () => {
    try {
      await transactionsApi.review(id);
      toast.success("Transaction marked as reviewed");
      refetch();
    } catch (error) {
      toast.error("Failed to review transaction");
    }
  };

  if (isLoading) {
    return (
      <DashboardLayout>
        <div className="flex items-center justify-center h-64">
          <div className="text-gray-500">Loading...</div>
        </div>
      </DashboardLayout>
    );
  }

  if (!transaction) {
    return (
      <DashboardLayout>
        <div className="text-center py-12">
          <p className="text-gray-500">Transaction not found</p>
        </div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <button
            onClick={() => router.back()}
            className="flex items-center text-gray-600 hover:text-gray-900"
          >
            <ArrowLeft className="h-5 w-5 mr-2" />
            Back
          </button>
          {transaction.status === "ALERTED" && (
            <button
              onClick={handleReview}
              className="flex items-center px-4 py-2 bg-primary-600 text-white rounded-md hover:bg-primary-700"
            >
              <CheckCircle className="h-5 w-5 mr-2" />
              Mark as Reviewed
            </button>
          )}
        </div>

        <div className="bg-white rounded-lg shadow p-6">
          <h1 className="text-2xl font-bold text-gray-900 mb-4">
            Transaction Details
          </h1>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-sm font-medium text-gray-500">ID</label>
              <p className="text-sm text-gray-900">{transaction.id}</p>
            </div>
            <div>
              <label className="text-sm font-medium text-gray-500">Amount</label>
              <p className="text-lg font-bold text-gray-900">${transaction.amount.toFixed(2)}</p>
            </div>
            <div>
              <label className="text-sm font-medium text-gray-500">From</label>
              <p className="text-sm text-gray-900">{transaction.from}</p>
            </div>
            <div>
              <label className="text-sm font-medium text-gray-500">To</label>
              <p className="text-sm text-gray-900">{transaction.to}</p>
            </div>
            <div>
              <label className="text-sm font-medium text-gray-500">Status</label>
              <span
                className={clsx(
                  "inline-block px-3 py-1 rounded-full text-xs font-medium mt-1",
                  {
                    "bg-green-100 text-green-800": transaction.status === "PROCESSED",
                    "bg-red-100 text-red-800": transaction.status === "ALERTED",
                    "bg-blue-100 text-blue-800": transaction.status === "REVIEWED",
                    "bg-yellow-100 text-yellow-800": transaction.status === "PROCESSING",
                  }
                )}
              >
                {transaction.status}
              </span>
            </div>
            <div>
              <label className="text-sm font-medium text-gray-500">ML Score</label>
              <p className="text-sm text-gray-900">{transaction.mlScore?.toFixed(4) || "N/A"}</p>
            </div>
            <div>
              <label className="text-sm font-medium text-gray-500">Timestamp</label>
              <p className="text-sm text-gray-900">{format(new Date(transaction.timestamp), "PPpp")}</p>
            </div>
          </div>

          {transaction.aiAnalysis && (
            <div className="mt-6">
              <label className="text-sm font-medium text-gray-500">AI Analysis</label>
              <p className="mt-2 text-sm text-gray-700 bg-gray-50 p-4 rounded">{transaction.aiAnalysis}</p>
            </div>
          )}

          {transaction.alertReasons && transaction.alertReasons.length > 0 && (
            <div className="mt-6">
              <label className="text-sm font-medium text-gray-500">Alert Reasons</label>
              <ul className="mt-2 list-disc list-inside space-y-1">
                {transaction.alertReasons.map((reason, i) => (
                  <li key={i} className="text-sm text-gray-700">{reason}</li>
                ))}
              </ul>
            </div>
          )}
        </div>
      </div>
    </DashboardLayout>
  );
}
