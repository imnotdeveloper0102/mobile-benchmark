import { create } from 'zustand';
import type { MetricsSnapshot, MetricWindow } from '../services/types';

export const MAX_HISTORY_POINTS = 3600;

interface MetricsState {
  snapshot: MetricsSnapshot | null;
  history: MetricsSnapshot[];
  window: MetricWindow;
  paused: boolean;
  pushBatch: (snapshots: MetricsSnapshot[]) => void;
  setSnapshot: (snapshot: MetricsSnapshot) => void;
  setWindow: (window: MetricWindow) => void;
  pause: () => void;
  resume: () => void;
  clear: () => void;
}

export const useMetricsStore = create<MetricsState>()((set) => ({
  snapshot: null,
  history: [],
  window: '5m',
  paused: false,
  pushBatch: (snapshots) =>
    set((state) => {
      if (state.paused || snapshots.length === 0) {
        return state;
      }
      const merged = [...state.history, ...snapshots];
      const trimmed =
        merged.length > MAX_HISTORY_POINTS
          ? merged.slice(merged.length - MAX_HISTORY_POINTS)
          : merged;
      return {
        snapshot: snapshots[snapshots.length - 1],
        history: trimmed,
      };
    }),
  setSnapshot: (snapshot) =>
    set((state) => {
      if (state.paused) {
        return state;
      }
      const merged = [...state.history, snapshot];
      const trimmed =
        merged.length > MAX_HISTORY_POINTS
          ? merged.slice(merged.length - MAX_HISTORY_POINTS)
          : merged;
      return { snapshot, history: trimmed };
    }),
  setWindow: (window) => set({ window }),
  pause: () => set({ paused: true }),
  resume: () => set({ paused: false }),
  clear: () => set({ snapshot: null, history: [] }),
}));
