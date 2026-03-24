"use client";

import { useEffect } from "react";
import { useSelector, useDispatch } from "react-redux";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { rulesApi } from "@/lib/api";
import { DashboardLayout } from "@/components/layout/DashboardLayout";
import { RootState } from "@/store/redux";
import { setRules, setLoading } from "@/store/redux";
import { Plus, Edit, Trash2, Power } from "lucide-react";
import clsx from "clsx";
import toast from "react-hot-toast";
import { useState } from "react";
import { RuleModal } from "@/components/rules/RuleModal";
import type { Rule } from "@/lib/types";

export default function RulesPage() {
  const dispatch = useDispatch();
  const queryClient = useQueryClient();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedRule, setSelectedRule] = useState<Rule | null>(null);

  const { data: rulesData, isLoading } = useQuery({
    queryKey: ["rules"],
    queryFn: async () => {
      const response = await rulesApi.getAll();
      return response.data;
    },
  });

  useEffect(() => {
    if (rulesData) {
      dispatch(setRules(rulesData));
    }
    dispatch(setLoading(isLoading));
  }, [rulesData, isLoading, dispatch]);

  const toggleMutation = useMutation({
    mutationFn: (id: number) => rulesApi.toggle(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["rules"] });
      toast.success("Rule toggled successfully");
    },
    onError: () => {
      toast.error("Failed to toggle rule");
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => rulesApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["rules"] });
      toast.success("Rule deleted successfully");
    },
    onError: () => {
      toast.error("Failed to delete rule");
    },
  });

  const handleEdit = (rule: Rule) => {
    setSelectedRule(rule);
    setIsModalOpen(true);
  };

  const handleCreate = () => {
    setSelectedRule(null);
    setIsModalOpen(true);
  };

  const handleDelete = (id: number) => {
    if (confirm("Are you sure you want to delete this rule?")) {
      deleteMutation.mutate(id);
    }
  };

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">Rules</h1>
            <p className="mt-2 text-sm text-gray-600">
              Manage fraud detection rules
            </p>
          </div>
          <button
            onClick={handleCreate}
            className="flex items-center px-4 py-2 bg-primary-600 text-white rounded-md hover:bg-primary-700"
          >
            <Plus className="h-5 w-5 mr-2" />
            Create Rule
          </button>
        </div>

        <div className="bg-white rounded-lg shadow overflow-hidden">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Name
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Type
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Priority
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Severity
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Status
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Stats
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {isLoading ? (
                <tr>
                  <td colSpan={7} className="px-6 py-4 text-center text-gray-500">
                    Loading...
                  </td>
                </tr>
              ) : rulesData && rulesData.length > 0 ? (
                rulesData.map((rule) => (
                  <tr key={rule.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4">
                      <div>
                        <div className="text-sm font-medium text-gray-900">
                          {rule.name}
                        </div>
                        {rule.description && (
                          <div className="text-sm text-gray-500">
                            {rule.description}
                          </div>
                        )}
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {rule.type}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {rule.priority}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span
                        className={clsx(
                          "px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full",
                          {
                            "bg-red-100 text-red-800": rule.severity >= 3,
                            "bg-yellow-100 text-yellow-800": rule.severity === 2,
                            "bg-green-100 text-green-800": rule.severity === 1,
                          }
                        )}
                      >
                        {rule.severity}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span
                        className={clsx(
                          "px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full",
                          rule.enabled
                            ? "bg-green-100 text-green-800"
                            : "bg-gray-100 text-gray-800"
                        )}
                      >
                        {rule.enabled ? "Enabled" : "Disabled"}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      <div className="text-xs">
                        <div>Executed: {rule.executionCount}</div>
                        <div>Alerts: {rule.alertCount}</div>
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                      <div className="flex items-center space-x-2">
                        <button
                          onClick={() => toggleMutation.mutate(rule.id)}
                          className={clsx(
                            "p-1 rounded",
                            rule.enabled
                              ? "text-green-600 hover:text-green-900"
                              : "text-gray-400 hover:text-gray-600"
                          )}
                          title={rule.enabled ? "Disable" : "Enable"}
                        >
                          <Power className="h-5 w-5" />
                        </button>
                        <button
                          onClick={() => handleEdit(rule)}
                          className="text-primary-600 hover:text-primary-900"
                          title="Edit"
                        >
                          <Edit className="h-5 w-5" />
                        </button>
                        <button
                          onClick={() => handleDelete(rule.id)}
                          className="text-red-600 hover:text-red-900"
                          title="Delete"
                        >
                          <Trash2 className="h-5 w-5" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={7} className="px-6 py-4 text-center text-gray-500">
                    No rules found
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      <RuleModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        rule={selectedRule}
      />
    </DashboardLayout>
  );
}
