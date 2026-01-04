import axios from "axios";
import type {
  DashboardStats,
  Transaction,
  TransactionDetails,
  Rule,
  RuleRequest,
  PageResponse,
  RuleChangeHistory,
  EStatus,
} from "./types";

const API_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api";

const api = axios.create({
  baseURL: API_URL,
  headers: {
    "Content-Type": "application/json",
  },
  withCredentials: true,
});

export const dashboardApi = {
  getStats: () => api.get<DashboardStats>("/admin/dashboard/stats"),
  getRecentTransactions: (limit: number = 10) =>
    api.get<Transaction[]>("/admin/dashboard/recent-transactions", {
      params: { limit },
    }),
};

export const transactionsApi = {
  getAll: (params: {
    status?: EStatus;
    page?: number;
    size?: number;
    sortBy?: string;
    direction?: "ASC" | "DESC";
  }) =>
    api.get<PageResponse<Transaction>>("/admin/transactions", { params }),
  
  getById: (id: string) =>
    api.get<TransactionDetails>(`/admin/transactions/${id}`),
  
  review: (id: string) =>
    api.post<Transaction>(`/admin/transactions/${id}/review`),
  
  search: (params: {
    correlationId?: string;
    from?: string;
    to?: string;
    startDate?: string;
    endDate?: string;
    page?: number;
    size?: number;
  }) =>
    api.get<PageResponse<Transaction>>("/admin/transactions/search", { params }),
};

export const rulesApi = {
  getAll: (enabled?: boolean) =>
    api.get<Rule[]>("/admin/rules", { params: { enabled } }),
  
  getById: (id: number) =>
    api.get<Rule>(`/admin/rules/${id}`),
  
  create: (data: RuleRequest) =>
    api.post<Rule>("/admin/rules", data),
  
  update: (id: number, data: RuleRequest) =>
    api.put<Rule>(`/admin/rules/${id}`, data),
  
  toggle: (id: number) =>
    api.patch<Rule>(`/admin/rules/${id}/toggle`),
  
  delete: (id: number) =>
    api.delete(`/admin/rules/${id}`),
  
  getHistory: (id: number) =>
    api.get<RuleChangeHistory[]>(`/admin/rules/${id}/history`),
  
  getTypes: () =>
    api.get<string[]>("/admin/rules/types"),
};

export const statusesApi = {
  getAll: () => api.get<string[]>("/admin/statuses"),
};

export default api;
