import { configureStore, createSlice, PayloadAction } from "@reduxjs/toolkit";
import type { Rule } from "@/lib/types";

interface RulesState {
  rules: Rule[];
  selectedRule: Rule | null;
  loading: boolean;
  error: string | null;
}

const initialState: RulesState = {
  rules: [],
  selectedRule: null,
  loading: false,
  error: null,
};

const rulesSlice = createSlice({
  name: "rules",
  initialState,
  reducers: {
    setRules: (state, action: PayloadAction<Rule[]>) => {
      state.rules = action.payload;
    },
    setSelectedRule: (state, action: PayloadAction<Rule | null>) => {
      state.selectedRule = action.payload;
    },
    setLoading: (state, action: PayloadAction<boolean>) => {
      state.loading = action.payload;
    },
    setError: (state, action: PayloadAction<string | null>) => {
      state.error = action.payload;
    },
    addRule: (state, action: PayloadAction<Rule>) => {
      state.rules.push(action.payload);
    },
    updateRule: (state, action: PayloadAction<Rule>) => {
      const index = state.rules.findIndex((r) => r.id === action.payload.id);
      if (index !== -1) {
        state.rules[index] = action.payload;
      }
    },
    deleteRule: (state, action: PayloadAction<number>) => {
      state.rules = state.rules.filter((r) => r.id !== action.payload);
    },
  },
});

export const {
  setRules,
  setSelectedRule,
  setLoading,
  setError,
  addRule,
  updateRule,
  deleteRule,
} = rulesSlice.actions;

export const store = configureStore({
  reducer: {
    rules: rulesSlice.reducer,
  },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
