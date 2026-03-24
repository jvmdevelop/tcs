export enum EStatus {
  PROCESSED = "PROCESSED",
  PROCESSING = "PROCESSING",
  ALERTED = "ALERTED",
  REVIEWED = "REVIEWED",
}

export enum RuleType {
  THRESHOLD = "THRESHOLD",
  VELOCITY = "VELOCITY",
  PATTERN = "PATTERN",
  ML_BASED = "ML_BASED",
  GEO_ANOMALY = "GEO_ANOMALY",
}

export interface Transaction {
  id: string;
  correlationId: string;
  amount: number;
  from: string;
  to: string;
  type: string;
  timestamp: string;
  status: EStatus;
}

export interface TransactionDetails extends Transaction {
  mlScore?: number;
  alertReasons?: string[];
  processingHistory?: Record<string, any>[];
  createdAt: string;
  updatedAt: string;
  ipAddress?: string;
  deviceId?: string;
  location?: string;
  merchantCategory?: string;
  deviceUsed?: string;
  fraudType?: string;
  timeSinceLastTransaction?: number;
  spendingDeviationScore?: number;
  velocityScore?: number;
  geoAnomalyScore?: number;
  paymentChannel?: string;
  deviceHash?: string;
  aiAnalysis?: string;
}

export interface Rule {
  id: number;
  name: string;
  description?: string;
  type: RuleType;
  configuration: string;
  enabled: boolean;
  priority: number;
  severity: number;
  createdBy?: string;
  modifiedBy?: string;
  createdAt: string;
  updatedAt: string;
  executionCount: number;
  alertCount: number;
}

export interface RuleRequest {
  name: string;
  description?: string;
  type: RuleType;
  configuration: string;
  enabled?: boolean;
  priority?: number;
  severity?: number;
}

export interface DashboardStats {
  totalProcessed: number;
  totalAlerted: number;
  totalReviewed: number;
  totalProcessing: number;
  activeRulesCount: number;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface RuleChangeHistory {
  id: number;
  ruleId: number;
  action: string;
  changedBy: string;
  changedAt: string;
  oldValue?: string;
  newValue?: string;
}
