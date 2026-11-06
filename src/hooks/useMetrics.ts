import { useEffect, useRef, useState } from 'react';
import { useIsFocused } from '@react-navigation/native';
import { getSnapshot, startSampling, stopSampling, subscribeBatch } from '../services/bridge';
import { useMetricsStore } from '../store/metricsStore';
import { useSettingsStore } from '../store/settingsStore';
import type { MetricsSnapshot, MetricWindow } from '../services/types';

export interface UseMetricsResult {
  snapshot: MetricsSnapshot | null;
  history: MetricsSnapshot[];
  paused: boolean;
  window: MetricWindow;
  error: string | null;
  pause: () => void;
  resume: () => void;
}

export function useMetrics(): UseMetricsResult {
  const isFocused = useIsFocused();
  const snapshot = useMetricsStore((state) => state.snapshot);
  const history = useMetricsStore((state) => state.history);
  const paused = useMetricsStore((state) => state.paused);
  const window = useMetricsStore((state) => state.window);
  const pushBatch = useMetricsStore((state) => state.pushBatch);
  const setSnapshot = useMetricsStore((state) => state.setSnapshot);
  const pause = useMetricsStore((state) => state.pause);
  const resume = useMetricsStore((state) => state.resume);
  const samplingHz = useSettingsStore((state) => state.samplingHz);
  const [error, setError] = useState<string | null>(null);
  const startedRef = useRef(false);

  useEffect(() => {
    if (!isFocused) {
      return undefined;
    }
    let unsubscribe: (() => void) | null = null;
    let cancelled = false;
    const intervalMs = Math.round(1000 / samplingHz);

    const setup = async (): Promise<void> => {
      try {
        unsubscribe = subscribeBatch((snapshots) => {
          if (!cancelled) {
            pushBatch(snapshots);
          }
        });
        await startSampling(intervalMs);
        startedRef.current = true;
        const first = await getSnapshot();
        if (!cancelled) {
          setSnapshot(first);
          setError(null);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : 'Không nhận được dữ liệu native.');
        }
      }
    };

    setup();
    return () => {
      cancelled = true;
      if (unsubscribe) {
        unsubscribe();
      }
      if (startedRef.current) {
        startedRef.current = false;
        stopSampling().catch(() => undefined);
      }
    };
  }, [isFocused, samplingHz, pushBatch, setSnapshot]);

  return { snapshot, history, paused, window, error, pause, resume };
}
