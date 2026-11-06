import { create } from 'zustand';
import type { BenchmarkHistoryItem, BenchmarkResult } from '../services/types';

export type BenchmarkStatus = 'idle' | 'running' | 'done' | 'error';

interface BenchmarkState {
  status: BenchmarkStatus;
  progress: number;
  currentPhase: string;
  lastResult: BenchmarkResult | null;
  history: BenchmarkHistoryItem[];
  errorMessage: string | null;
  setRunning: (phase: string, progress: number) => void;
  setPhase: (phase: string, progress: number) => void;
  setResult: (result: BenchmarkResult) => void;
  setError: (message: string) => void;
  addHistory: (item: BenchmarkHistoryItem) => void;
  setHistory: (items: BenchmarkHistoryItem[]) => void;
  reset: () => void;
}

export const useBenchmarkStore = create<BenchmarkState>()((set) => ({
  status: 'idle',
  progress: 0,
  currentPhase: '',
  lastResult: null,
  history: [],
  errorMessage: null,
  setRunning: (phase, progress) =>
    set({ status: 'running', currentPhase: phase, progress, errorMessage: null }),
  setPhase: (phase, progress) => set({ currentPhase: phase, progress }),
  setResult: (result) =>
    set((state) => ({
      status: 'done',
      progress: 100,
      lastResult: result,
      history: [
        {
          id: result.id,
          startedAtMs: result.startedAtMs,
          endedAtMs: result.endedAtMs,
          totalScore: result.scores.total,
          cpuScore: result.scores.cpu,
          memoryScore: result.scores.memory,
          storageScore: result.scores.storage,
          gpuScore: result.scores.gpu,
          deviceModel: result.deviceModel,
          stressDropPct: result.stressDropPct,
        },
        ...state.history,
      ].slice(0, 100),
    })),
  setError: (message) => set({ status: 'error', errorMessage: message }),
  addHistory: (item) =>
    set((state) => {
      if (state.history.some((existing) => existing.id === item.id)) {
        return state;
      }
      return { history: [item, ...state.history].slice(0, 100) };
    }),
  setHistory: (items) => set({ history: items.slice(0, 100) }),
  reset: () => set({ status: 'idle', progress: 0, currentPhase: '', errorMessage: null }),
}));
