import {
  formatBytes,
  formatBytesPerSec,
  formatCurrent,
  formatDateTime,
  formatDuration,
  formatDurationMin,
  formatFps,
  formatMHz,
  formatPercent,
  formatScore,
  formatSpeed,
  formatTemp,
  formatVoltage,
  formatWatts,
  isFieldUnavailable,
  unavailableReason,
} from '../services/format';
import type { MetricsSnapshot } from '../services/types';

function makeSnapshotWithUnavailable(): MetricsSnapshot {
  return {
    timestampMs: 1_700_000_000_000,
    cpu: {
      cores: 8,
      freqMHz: [],
      maxFreqMHz: [],
      loadEstimatePct: null,
      loadMethod: 'freq',
      loadNote: '',
    },
    memory: {
      totalBytes: 0,
      availBytes: 0,
      usedBytes: 0,
      lowMemory: false,
      thresholdBytes: 0,
      appPssBytes: 0,
    },
    thermal: {
      batteryTempC: null,
      cpuTempC: null,
      gpuTempC: null,
      thermalStatus: 'none',
      overheating: false,
    },
    battery: {
      levelPct: 0,
      charging: false,
      currentNowUa: null,
      voltageMv: null,
      powerWatts: null,
      health: 'unknown',
      timeRemainingMin: null,
    },
    fps: { fps: 0, jankPct: 0, refreshRateHz: 60, note: '' },
    storage: { internalTotalBytes: 0, internalAvailBytes: 0, externalTotalBytes: null },
    network: { rxBytesPerSec: 0, txBytesPerSec: 0, type: 'unknown', signalDbm: null },
    unavailable: [
      { field: 'thermal.cpuTemp', reason: 'Không đọc được /sys/class/thermal trên máy này' },
    ],
  };
}

describe('formatBytes', () => {
  it('formats B, KB, MB, GB', () => {
    expect(formatBytes(512)).toBe('512 B');
    expect(formatBytes(2048)).toBe('2.0 KB');
    expect(formatBytes(5 * 1024 * 1024)).toBe('5.0 MB');
    expect(formatBytes(2 * 1024 * 1024 * 1024)).toBe('2.0 GB');
  });
  it('returns unavailable for negative', () => {
    expect(formatBytes(-1)).toBe('Không khả dụng');
  });
});

describe('formatBytesPerSec', () => {
  it('appends /s', () => {
    expect(formatBytesPerSec(1024)).toBe('1.0 KB/s');
  });
});

describe('formatMHz', () => {
  it('formats MHz and GHz', () => {
    expect(formatMHz(800)).toBe('800 MHz');
    expect(formatMHz(2800)).toBe('2.80 GHz');
  });
});

describe('formatTemp', () => {
  it('formats celsius and null', () => {
    expect(formatTemp(36.5)).toBe('36.5 °C');
    expect(formatTemp(null)).toBe('Không khả dụng');
  });
});

describe('formatWatts / voltage / current', () => {
  it('formats power values and null', () => {
    expect(formatWatts(5.25)).toBe('5.25 W');
    expect(formatWatts(null)).toBe('Không khả dụng');
    expect(formatVoltage(4200)).toBe('4.20 V');
    expect(formatVoltage(null)).toBe('Không khả dụng');
    expect(formatCurrent(1500000)).toBe('1500 mA');
    expect(formatCurrent(null)).toBe('Không khả dụng');
  });
});

describe('formatDuration', () => {
  it('formats ms, seconds, minutes', () => {
    expect(formatDuration(500)).toBe('500 ms');
    expect(formatDuration(1500)).toBe('1.5 s');
    expect(formatDuration(90000)).toBe('1 phút 30 s');
  });
});

describe('formatDurationMin', () => {
  it('formats minutes and hours', () => {
    expect(formatDurationMin(30)).toBe('30 phút');
    expect(formatDurationMin(90)).toBe('1 giờ 30 phút');
    expect(formatDurationMin(null)).toBe('Không khả dụng');
  });
});

describe('formatPercent', () => {
  it('formats percent and null', () => {
    expect(formatPercent(45.678)).toBe('45.7%');
    expect(formatPercent(80, 0)).toBe('80%');
    expect(formatPercent(null)).toBe('Không khả dụng');
  });
});

describe('formatScore / speed / fps', () => {
  it('formats score, speed, fps', () => {
    expect(formatScore(750.4)).toBe('750');
    expect(formatSpeed(123.45)).toBe('123.5 MB/s');
    expect(formatFps(59.97)).toBe('60.0 FPS');
  });
});

describe('formatDateTime', () => {
  it('returns a dd/mm/yyyy hh:mm string', () => {
    const text = formatDateTime(1_700_000_000_000);
    expect(text).toMatch(/^\d{2}\/\d{2}\/\d{4} \d{2}:\d{2}$/);
  });
});

describe('unavailable helpers', () => {
  it('finds reason by field', () => {
    const snapshot = makeSnapshotWithUnavailable();
    expect(unavailableReason(snapshot, 'thermal.cpuTemp')).toBe(
      'Không đọc được /sys/class/thermal trên máy này',
    );
    expect(unavailableReason(snapshot, 'other')).toBeNull();
    expect(isFieldUnavailable(snapshot, 'thermal.cpuTemp')).toBe(true);
    expect(isFieldUnavailable(snapshot, 'other')).toBe(false);
  });
});
