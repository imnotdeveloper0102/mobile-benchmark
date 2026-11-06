import AsyncStorage from '@react-native-async-storage/async-storage';
import { create } from 'zustand';
import { createJSONStorage, persist } from 'zustand/middleware';
import type { AlertThresholds, OverlayConfig } from '../services/types';
import type { Language } from '../i18n';

export const OVERLAY_METRIC_KEYS = ['cpu', 'ram', 'temp', 'fps', 'battery'] as const;
export type OverlayMetricKey = (typeof OVERLAY_METRIC_KEYS)[number];

interface SettingsState {
  overlay: OverlayConfig;
  thresholds: AlertThresholds;
  samplingHz: number;
  darkMode: boolean;
  language: Language;
  setOverlay: (patch: Partial<OverlayConfig>) => void;
  setThresholds: (patch: Partial<AlertThresholds>) => void;
  setSamplingHz: (hz: number) => void;
  setDarkMode: (value: boolean) => void;
  setLanguage: (language: Language) => void;
}

const STORAGE_KEY = 'benchmark-performance-settings-v1';

export const useSettingsStore = create<SettingsState>()(
  persist(
    (set) => ({
      overlay: {
        enabledMetrics: ['cpu', 'ram', 'temp', 'fps'],
        opacity: 0.85,
        size: 'medium',
      },
      thresholds: {
        tempC: 45,
        ramAvailPct: 15,
        batteryPct: 20,
      },
      samplingHz: 1,
      darkMode: true,
      language: 'vi',
      setOverlay: (patch) =>
        set((state) => ({ overlay: { ...state.overlay, ...patch } })),
      setThresholds: (patch) =>
        set((state) => ({ thresholds: { ...state.thresholds, ...patch } })),
      setSamplingHz: (hz) => set({ samplingHz: Math.min(2, Math.max(0.5, hz)) }),
      setDarkMode: (value) => set({ darkMode: value }),
      setLanguage: (language) => set({ language }),
    }),
    {
      name: STORAGE_KEY,
      storage: createJSONStorage(() => AsyncStorage),
      partialize: (state) => ({
        overlay: state.overlay,
        thresholds: state.thresholds,
        samplingHz: state.samplingHz,
        darkMode: state.darkMode,
        language: state.language,
      }),
    },
  ),
);
