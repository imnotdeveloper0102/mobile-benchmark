import {
  bucketMsForWindow,
  detectAnomalies,
  detectBatteryDrop,
  detectThermalRise,
  downsample,
  filterByWindow,
} from '../utils/aggregate';
import type { MetricsSnapshot } from '../services/types';

function makeSnapshot(timestampMs: number, batteryTempC: number | null, levelPct: number, charging = false): MetricsSnapshot {
  return {
    timestampMs,
    cpu: {
      cores: 8,
      freqMHz: [1800],
      maxFreqMHz: [2800],
      loadEstimatePct: 50,
      loadMethod: 'freq',
      loadNote: '',
    },
    memory: {
      totalBytes: 8 * 1024 * 1024 * 1024,
      availBytes: 4 * 1024 * 1024 * 1024,
      usedBytes: 4 * 1024 * 1024 * 1024,
      lowMemory: false,
      thresholdBytes: 100 * 1024 * 1024,
      appPssBytes: 100 * 1024 * 1024,
    },
    thermal: {
      batteryTempC,
      cpuTempC: null,
      gpuTempC: null,
      thermalStatus: 'none',
      overheating: false,
    },
    battery: {
      levelPct,
      charging,
      currentNowUa: null,
      voltageMv: 4000,
      powerWatts: null,
      health: 'good',
      timeRemainingMin: null,
    },
    fps: { fps: 60, jankPct: 0, refreshRateHz: 60, note: '' },
    storage: {
      internalTotalBytes: 128 * 1024 * 1024 * 1024,
      internalAvailBytes: 64 * 1024 * 1024 * 1024,
      externalTotalBytes: null,
    },
    network: { rxBytesPerSec: 0, txBytesPerSec: 0, type: 'wifi', signalDbm: -60 },
    unavailable: [],
  };
}

describe('bucketMsForWindow', () => {
  it('returns 1000 / 5000 / 60000', () => {
    expect(bucketMsForWindow('1m')).toBe(1000);
    expect(bucketMsForWindow('5m')).toBe(5000);
    expect(bucketMsForWindow('1h')).toBe(60000);
  });
});

describe('downsample', () => {
  it('averages values per bucket', () => {
    const base = 1_700_000_000_000;
    const snapshots = [
      makeSnapshot(base, 30, 80),
      makeSnapshot(base + 500, 32, 80),
      makeSnapshot(base + 1500, 34, 80),
    ];
    const points = downsample(snapshots, (item) => item.thermal.batteryTempC, 1000);
    expect(points).toHaveLength(2);
    expect(points[0].value).toBeCloseTo(31);
    expect(points[1].value).toBeCloseTo(34);
  });

  it('skips null values', () => {
    const base = 1_700_000_000_000;
    const snapshots = [
      makeSnapshot(base, null, 80),
      makeSnapshot(base + 1000, 40, 80),
    ];
    const points = downsample(snapshots, (item) => item.thermal.batteryTempC, 1000);
    expect(points).toHaveLength(1);
    expect(points[0].value).toBe(40);
  });

  it('returns empty for empty input', () => {
    expect(downsample([], (item) => item.thermal.batteryTempC, 1000)).toEqual([]);
  });
});

describe('filterByWindow', () => {
  it('keeps only snapshots inside the window', () => {
    const now = 1_700_000_000_000;
    const snapshots = [
      makeSnapshot(now - 120_000, 30, 80),
      makeSnapshot(now - 30_000, 30, 80),
      makeSnapshot(now, 30, 80),
    ];
    expect(filterByWindow(snapshots, '1m', now)).toHaveLength(2);
    expect(filterByWindow(snapshots, '5m', now)).toHaveLength(3);
  });
});

describe('detectThermalRise', () => {
  it('flags a rise over 10C in 5 minutes', () => {
    const base = 1_700_000_000_000;
    const snapshots = [
      makeSnapshot(base, 30, 80),
      makeSnapshot(base + 120_000, 42, 80),
    ];
    expect(detectThermalRise(snapshots)).toHaveLength(1);
  });

  it('does not flag a small rise', () => {
    const base = 1_700_000_000_000;
    const snapshots = [
      makeSnapshot(base, 30, 80),
      makeSnapshot(base + 120_000, 35, 80),
    ];
    expect(detectThermalRise(snapshots)).toHaveLength(0);
  });
});

describe('detectBatteryDrop', () => {
  it('flags a drop over 5% in 10 minutes', () => {
    const base = 1_700_000_000_000;
    const snapshots = [
      makeSnapshot(base, 30, 80),
      makeSnapshot(base + 300_000, 30, 73),
    ];
    expect(detectBatteryDrop(snapshots)).toHaveLength(1);
  });

  it('does not flag a drop while charging', () => {
    const base = 1_700_000_000_000;
    const snapshots = [
      makeSnapshot(base, 30, 80, true),
      makeSnapshot(base + 300_000, 30, 70, true),
    ];
    expect(detectBatteryDrop(snapshots)).toHaveLength(0);
  });
});

describe('detectAnomalies', () => {
  it('combines thermal and battery anomalies sorted by time', () => {
    const base = 1_700_000_000_000;
    const snapshots = [
      makeSnapshot(base, 30, 80),
      makeSnapshot(base + 120_000, 42, 73),
    ];
    const anomalies = detectAnomalies(snapshots);
    expect(anomalies.length).toBeGreaterThanOrEqual(1);
    expect(anomalies[0].kind).toBe('thermal_rise');
  });
});
