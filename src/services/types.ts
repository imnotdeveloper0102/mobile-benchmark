export type MetricWindow = '1m' | '5m' | '1h';

export interface UnavailableEntry {
  field: string;
  reason: string;
}

export interface CpuMetrics {
  cores: number;
  freqMHz: number[];
  maxFreqMHz: number[];
  loadEstimatePct: number | null;
  loadMethod: string;
  loadNote: string;
}

export interface MemoryMetrics {
  totalBytes: number;
  availBytes: number;
  usedBytes: number;
  lowMemory: boolean;
  thresholdBytes: number;
  appPssBytes: number;
}

export interface ThermalMetrics {
  batteryTempC: number | null;
  cpuTempC: number | null;
  gpuTempC: number | null;
  thermalStatus: string;
  overheating: boolean;
}

export interface BatteryMetrics {
  levelPct: number;
  charging: boolean;
  currentNowUa: number | null;
  voltageMv: number | null;
  powerWatts: number | null;
  health: string;
  timeRemainingMin: number | null;
}

export interface FpsMetrics {
  fps: number;
  jankPct: number;
  refreshRateHz: number;
  note: string;
}

export interface StorageMetrics {
  internalTotalBytes: number;
  internalAvailBytes: number;
  externalTotalBytes: number | null;
}

export interface NetworkMetrics {
  rxBytesPerSec: number;
  txBytesPerSec: number;
  type: string;
  signalDbm: number | null;
}

export interface MetricsSnapshot {
  timestampMs: number;
  cpu: CpuMetrics;
  memory: MemoryMetrics;
  thermal: ThermalMetrics;
  battery: BatteryMetrics;
  fps: FpsMetrics;
  storage: StorageMetrics;
  network: NetworkMetrics;
  unavailable: UnavailableEntry[];
}

export interface DeviceInfo {
  model: string;
  manufacturer: string;
  brand: string;
  socModel: string | null;
  gpuRenderer: string | null;
  androidVersion: string;
  apiLevel: number;
  securityPatch: string;
  buildId: string;
  board: string;
  hardware: string;
  sensors: string[];
  screenWidthPx: number;
  screenHeightPx: number;
  screenDensityDpi: number;
  screenDensity: number;
  hdrCapable: boolean;
  totalRamBytes: number;
  cpuAbi: string[];
  kernelVersion: string | null;
}

export interface BenchmarkScores {
  cpu: number;
  memory: number;
  storage: number;
  gpu: number;
  total: number;
}

export interface BenchmarkResult {
  id: string;
  startedAtMs: number;
  endedAtMs: number;
  cpuSingleMs: number;
  cpuMultiMs: number;
  sha256Ms: number;
  gzipMs: number;
  matmulMs: number;
  sortMs: number;
  memReadMBps: number;
  memWriteMBps: number;
  memRandomLatencyNs: number;
  storageReadMBps: number;
  storageWriteMBps: number;
  gpuFps: number;
  gpuJankPct: number;
  stressDropPct: number | null;
  scores: BenchmarkScores;
  deviceModel: string;
}

export interface BenchmarkHistoryItem {
  id: string;
  startedAtMs: number;
  endedAtMs: number;
  totalScore: number;
  cpuScore: number;
  memoryScore: number;
  storageScore: number;
  gpuScore: number;
  deviceModel: string;
  stressDropPct: number | null;
}

export interface OverlayConfig {
  enabledMetrics: string[];
  opacity: number;
  size: 'small' | 'medium' | 'large';
}

export interface AlertThresholds {
  tempC: number;
  ramAvailPct: number;
  batteryPct: number;
}

export interface NativeBenchmarkResult {
  id: string;
  startedAtMs: number;
  endedAtMs: number;
  cpuSingleMs: number;
  cpuMultiMs: number;
  sha256Ms: number;
  gzipMs: number;
  matmulMs: number;
  sortMs: number;
  memReadMBps: number;
  memWriteMBps: number;
  memRandomLatencyNs: number;
  storageReadMBps: number;
  storageWriteMBps: number;
  gpuFps: number;
  gpuJankPct: number;
  stressDropPct: number | null;
  scores: BenchmarkScores;
  deviceModel: string;
}
