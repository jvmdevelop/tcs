import { makeAutoObservable, runInAction } from "mobx";
import type { Transaction, PageResponse, EStatus } from "@/lib/types";
import { transactionsApi } from "@/lib/api";

class TransactionStore {
  transactions: Transaction[] = [];
  selectedTransaction: Transaction | null = null;
  currentPage = 0;
  pageSize = 20;
  totalPages = 0;
  totalElements = 0;
  loading = false;
  error: string | null = null;
  statusFilter: EStatus | undefined = undefined;

  constructor() {
    makeAutoObservable(this);
  }

  setStatusFilter(status: EStatus | undefined) {
    this.statusFilter = status;
  }

  setPage(page: number) {
    this.currentPage = page;
  }

  setPageSize(size: number) {
    this.pageSize = size;
  }

  async fetchTransactions() {
    this.loading = true;
    this.error = null;
    try {
      const response = await transactionsApi.getAll({
        status: this.statusFilter,
        page: this.currentPage,
        size: this.pageSize,
      });
      runInAction(() => {
        this.transactions = response.data.content;
        this.totalPages = response.data.totalPages;
        this.totalElements = response.data.totalElements;
        this.loading = false;
      });
    } catch (error: any) {
      runInAction(() => {
        this.error = error.message || "Failed to fetch transactions";
        this.loading = false;
      });
    }
  }

  async fetchTransactionById(id: string) {
    this.loading = true;
    this.error = null;
    try {
      const response = await transactionsApi.getById(id);
      runInAction(() => {
        this.selectedTransaction = response.data as any;
        this.loading = false;
      });
    } catch (error: any) {
      runInAction(() => {
        this.error = error.message || "Failed to fetch transaction";
        this.loading = false;
      });
    }
  }

  async reviewTransaction(id: string) {
    try {
      await transactionsApi.review(id);
      await this.fetchTransactions();
    } catch (error: any) {
      runInAction(() => {
        this.error = error.message || "Failed to review transaction";
      });
    }
  }
}

export const transactionStore = new TransactionStore();
