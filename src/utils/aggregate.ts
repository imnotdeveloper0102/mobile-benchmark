import type { MetricsSnapshot } from '../services/types';

export interface AggregatedPoint {
  timestampMs: number;
  value: number;
}

export interface Anomaly {
  kind: 'thermal_rise' | 'battery_drop';
  startedAtMs: number;
  endedAtMs: number;
  detail: string;
}

/**
 * Gộp mẫu theo cửa sổ: lấy trung bình mỗi bucket.
 * bucketMs ví dụ 1000 cho 1m, 5000 cho 5m, 60000 cho 1h.
 * Trả về tối đa 120 điểm để chart vẽ nhẹ.
 */
export function downsample(
  snapshots: MetricsSnapshot[],
  valueOf: (snapshot: MetricsSnapshot) => number | null,
  bucketMs: number,
): AggregatedPoint[] {
  if (snapshots.length === 0 || bucketMs <= 0) {
    return [];
  }
  const buckets = new Map<number, { sum: number; count: number }>();
  for (const snapshot of snapshots) {
    const value = valueOf(snapshot);
    if (value === null || value === undefined || !Number.isFinite(value)) {
      continue;
    }
    const bucketStart = Math.floor(snapshot.timestampMs / bucketMs) * bucketMs;
    const existing = buckets.get(bucketStart);
    if (existing) {
      existing.sum += value;
      existing.count += 1;
    } else {
      buckets.set(bucketStart, { sum: value, count: 1 });
    }
  }
  const sorted = Array.from(buckets.entries()).sort((a, b) => a[0] - b[0]);
  const averaged: AggregatedPoint[] = sorted.map(([timestampMs, bucket]) => ({
    timestampMs,
    value: bucket.sum / bucket.count,
  }));
  if (averaged.length <= 120) {
    return averaged;
  }
  const step = Math.ceil(averaged.length / 120);
  const reduced: AggregatedPoint[] = [];
  for (let i = 0; i < averaged.length; i += step) {
    const chunk = averaged.slice(i, i + step);
    const sum = chunk.reduce((acc, point) => acc + point.value, 0);
    reduced.push({ timestampMs: chunk[0].timestampMs, value: sum / chunk.length });
  }
  return reduced;
}

export function bucketMsForWindow(window: '1m' | '5m' | '1h'): number {
  if (window === '1m') {
    return 1000;
  }
  if (window === '5m') {
    return 5000;
  }
  return 60000;
}

export function filterByWindow(
  snapshots: MetricsSnapshot[],
  window: '1m' | '5m' | '1h',
  nowMs: number,
): MetricsSnapshot[] {
  const spanMs = window === '1m' ? 60_000 : window === '5m' ? 300_000 : 3_600_000;
  const cutoff = nowMs - spanMs;
  return snapshots.filter((snapshot) => snapshot.timestampMs >= cutoff);
}

export function detectThermalRise(
  snapshots: MetricsSnapshot[],
  windowMs = 300_000,
  riseThresholdC = 10,
): Anomaly[] {
  const anomalies: Anomaly[] = [];
  const sorted = [...snapshots].sort((a, b) => a.timestampMs - b.timestampMs);
  for (let i = 0; i < sorted.length; i += 1) {
    const start = sorted[i];
    if (start.thermal.batteryTempC === null) {
      continue;
    }
    for (let j = i + 1; j < sorted.length; j += 1) {
      const end = sorted[j];
      if (end.timestampMs - start.timestampMs > windowMs) {
        break;
      }
      if (end.thermal.batteryTempC === null) {
        continue;
      }
      const rise = end.thermal.batteryTempC - start.thermal.batteryTempC;
      if (rise > riseThresholdC) {
        anomalies.push({
          kind: 'thermal_rise',
          startedAtMs: start.timestampMs,
          endedAtMs: end.timestampMs,
          detail: `Nhiệt độ pin tăng ${rise.toFixed(1)} °C trong ${Math.round((end.timestampMs - start.timestampMs) / 60000)} phút`,
        });
        i = j;
        break;
      }
    }
  }
  return anomalies;
}

export function detectBatteryDrop(
  snapshots: MetricsSnapshot[],
  windowMs = 600_000,
  dropThresholdPct = 5,
): Anomaly[] {
  const anomalies: Anomaly[] = [];
  const sorted = [...snapshots].sort((a, b) => a.timestampMs - b.timestampMs);
  for (let i = 0; i < sorted.length; i += 1) {
    const start = sorted[i];
    if (start.battery.charging) {
      continue;
    }
    for (let j = i + 1; j < sorted.length; j += 1) {
      const end = sorted[j];
      if (end.timestampMs - start.timestampMs > windowMs) {
        break;
      }
      if (end.battery.charging) {
        break;
      }
      const drop = start.battery.levelPct - end.battery.levelPct;
      if (drop > dropThresholdPct) {
        anomalies.push({
          kind: 'battery_drop',
          startedAtMs: start.timestampMs,
          endedAtMs: end.timestampMs,
          detail: `Pin tụt ${drop.toFixed(0)}% trong ${Math.round((end.timestampMs - start.timestampMs) / 60000)} phút`,
        });
        i = j;
        break;
      }
    }
  }
  return anomalies;
}

export function detectAnomalies(snapshots: MetricsSnapshot[]): Anomaly[] {
  return [...detectThermalRise(snapshots), ...detectBatteryDrop(snapshots)].sort(
    (a, b) => a.startedAtMs - b.startedAtMs,
  );
}

export function average(values: number[]): number | null {
  const finite = values.filter((value) => Number.isFinite(value));
  if (finite.length === 0) {
    return null;
  }
  return finite.reduce((acc, value) => acc + value, 0) / finite.length;
}

export function maxValue(values: number[]): number | null {
  const finite = values.filter((value) => Number.isFinite(value));
  if (finite.length === 0) {
    return null;
  }
  return Math.max(...finite);
}
