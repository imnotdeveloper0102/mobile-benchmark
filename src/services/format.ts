import type { MetricsSnapshot } from './types';

export function formatBytes(bytes: number): string {
  if (!Number.isFinite(bytes) || bytes < 0) {
    return 'Không khả dụng';
  }
  if (bytes < 1024) {
    return `${bytes} B`;
  }
  const units = ['KB', 'MB', 'GB', 'TB'];
  let value = bytes;
  let unitIndex = -1;
  while (value >= 1024 && unitIndex < units.length - 1) {
    value /= 1024;
    unitIndex += 1;
  }
  return `${value.toFixed(value >= 100 ? 0 : 1)} ${units[unitIndex]}`;
}

export function formatBytesPerSec(bytesPerSec: number): string {
  return `${formatBytes(bytesPerSec)}/s`;
}

export function formatMHz(mhz: number): string {
  if (!Number.isFinite(mhz) || mhz < 0) {
    return 'Không khả dụng';
  }
  if (mhz >= 1000) {
    return `${(mhz / 1000).toFixed(2)} GHz`;
  }
  return `${Math.round(mhz)} MHz`;
}

export function formatTemp(celsius: number | null): string {
  if (celsius === null || celsius === undefined || !Number.isFinite(celsius)) {
    return 'Không khả dụng';
  }
  return `${celsius.toFixed(1)} °C`;
}

export function formatWatts(watts: number | null): string {
  if (watts === null || watts === undefined || !Number.isFinite(watts)) {
    return 'Không khả dụng';
  }
  return `${watts.toFixed(2)} W`;
}

export function formatVoltage(mv: number | null): string {
  if (mv === null || mv === undefined || !Number.isFinite(mv)) {
    return 'Không khả dụng';
  }
  return `${(mv / 1000).toFixed(2)} V`;
}

export function formatCurrent(ua: number | null): string {
  if (ua === null || ua === undefined || !Number.isFinite(ua)) {
    return 'Không khả dụng';
  }
  return `${Math.round(ua / 1000)} mA`;
}

export function formatDuration(ms: number): string {
  if (!Number.isFinite(ms) || ms < 0) {
    return 'Không khả dụng';
  }
  if (ms < 1000) {
    return `${Math.round(ms)} ms`;
  }
  const seconds = ms / 1000;
  if (seconds < 60) {
    return `${seconds.toFixed(1)} s`;
  }
  const minutes = Math.floor(seconds / 60);
  const remainSeconds = Math.round(seconds % 60);
  return `${minutes} phút ${remainSeconds} s`;
}

export function formatDurationMin(minutes: number | null): string {
  if (minutes === null || minutes === undefined || !Number.isFinite(minutes)) {
    return 'Không khả dụng';
  }
  if (minutes < 60) {
    return `${Math.round(minutes)} phút`;
  }
  const hours = Math.floor(minutes / 60);
  const remain = Math.round(minutes % 60);
  return `${hours} giờ ${remain} phút`;
}

export function formatPercent(value: number | null, digits = 1): string {
  if (value === null || value === undefined || !Number.isFinite(value)) {
    return 'Không khả dụng';
  }
  return `${value.toFixed(digits)}%`;
}

export function formatScore(score: number): string {
  return `${Math.round(score)}`;
}

export function formatSpeed(mbps: number): string {
  if (!Number.isFinite(mbps) || mbps < 0) {
    return 'Không khả dụng';
  }
  return `${mbps.toFixed(1)} MB/s`;
}

export function formatFps(fps: number): string {
  if (!Number.isFinite(fps) || fps < 0) {
    return 'Không khả dụng';
  }
  return `${fps.toFixed(1)} FPS`;
}

export function formatDateTime(timestampMs: number): string {
  const date = new Date(timestampMs);
  const day = String(date.getDate()).padStart(2, '0');
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const year = date.getFullYear();
  const hour = String(date.getHours()).padStart(2, '0');
  const minute = String(date.getMinutes()).padStart(2, '0');
  return `${day}/${month}/${year} ${hour}:${minute}`;
}

export function formatTime(timestampMs: number): string {
  const date = new Date(timestampMs);
  const hour = String(date.getHours()).padStart(2, '0');
  const minute = String(date.getMinutes()).padStart(2, '0');
  const second = String(date.getSeconds()).padStart(2, '0');
  return `${hour}:${minute}:${second}`;
}

export function unavailableReason(snapshot: MetricsSnapshot, field: string): string | null {
  const entry = snapshot.unavailable.find((item) => item.field === field);
  return entry ? entry.reason : null;
}

export function isFieldUnavailable(snapshot: MetricsSnapshot, field: string): boolean {
  return snapshot.unavailable.some((item) => item.field === field);
}
