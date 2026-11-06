import { NativeEventEmitter, NativeModules } from 'react-native';

type EmitterModule = ConstructorParameters<typeof NativeEventEmitter>[0];
import type {
  AlertThresholds,
  BenchmarkHistoryItem,
  BenchmarkResult,
  DeviceInfo,
  MetricsSnapshot,
  NativeBenchmarkResult,
  OverlayConfig,
} from './types';

const MODULE_NAME = 'BenchmarkModule';

// Native tra JSON dang string (promise.resolve(json.toString()) ben
// Kotlin), khong phai object truc tiep, nen kieu o day ghi ro
// `| string` va ham parseNativeJson ben duoi chuyen ve object.
type BenchmarkModuleType = {
  startSampling: (intervalMs: number) => Promise<void>;
  stopSampling: () => Promise<void>;
  getSnapshot: () => Promise<MetricsSnapshot | string>;
  getDeviceInfo: () => Promise<DeviceInfo | string>;
  getBenchmarkHistory: () => Promise<BenchmarkHistoryItem[] | string>;
  runBenchmark: (mode: string) => Promise<NativeBenchmarkResult | string>;
  runStorageSpeedTest: () => Promise<{
    readMBps: number;
    writeMBps: number;
    seqReadMBps?: number;
    seqWriteMBps?: number;
  }>;
  requestOverlayPermission: () => Promise<boolean>;
  startOverlay: (configJson: string) => Promise<void>;
  stopOverlay: () => Promise<void>;
  setAlertThresholds: (json: string) => Promise<void>;
};

function getModule(): BenchmarkModuleType {
  const module = (NativeModules as Record<string, unknown>)[MODULE_NAME] as
    | BenchmarkModuleType
    | undefined;
  if (!module) {
    throw new Error(
      'Native module BenchmarkModule chưa khả dụng. Hãy build bản native (bare React Native) rồi chạy lại app.',
    );
  }
  return module;
}

function isNativeAvailable(): boolean {
  return Boolean((NativeModules as Record<string, unknown>)[MODULE_NAME]);
}

function parseNativeJson<T>(value: unknown): T {
  if (typeof value === 'string') {
    return JSON.parse(value) as T;
  }
  return value as T;
}

let emitter: NativeEventEmitter | null = null;

function getEmitter(): NativeEventEmitter {
  if (!emitter) {
    emitter = new NativeEventEmitter(
      NativeModules[MODULE_NAME] as unknown as EmitterModule,
    );
  }
  return emitter;
}

export async function startSampling(intervalMs: number): Promise<void> {
  const module = getModule();
  await module.startSampling(intervalMs);
}

export async function stopSampling(): Promise<void> {
  if (!isNativeAvailable()) {
    return;
  }
  await getModule().stopSampling();
}

export async function getSnapshot(): Promise<MetricsSnapshot> {
  const module = getModule();
  const raw = await module.getSnapshot();
  return parseNativeJson<MetricsSnapshot>(raw);
}

export async function getDeviceInfo(): Promise<DeviceInfo> {
  const module = getModule();
  const raw = await module.getDeviceInfo();
  const parsed = parseNativeJson<Record<string, unknown>>(raw);
  // Native tra shape phang (xem MetricsCollector.collectDeviceInfo):
  // co ca key cu (sdkInt, screen{...}) lan key moi. Map ve DeviceInfo
  // ma DeviceScreen dang doc, thieu truong nao thi de gia tri mac dinh
  // ro rang thay vi de undefined gay crash khi .join() / .toFixed().
  const screen = (parsed.screen ?? {}) as Record<string, unknown>;
  const abi = parsed.cpuAbi;
  return {
    model: String(parsed.model ?? ''),
    manufacturer: String(parsed.manufacturer ?? ''),
    brand: String(parsed.brand ?? parsed.manufacturer ?? ''),
    socModel: (parsed.socModel as string | null) ?? null,
    gpuRenderer: (parsed.gpuRenderer as string | null) ?? null,
    androidVersion: String(parsed.androidVersion ?? ''),
    apiLevel: Number(parsed.apiLevel ?? parsed.sdkInt ?? 0),
    securityPatch: String(parsed.securityPatch ?? ''),
    buildId: String(parsed.buildId ?? ''),
    board: String(parsed.board ?? ''),
    hardware: String(parsed.hardware ?? ''),
    sensors: Array.isArray(parsed.sensors) ? (parsed.sensors as string[]) : [],
    screenWidthPx: Number(parsed.screenWidthPx ?? screen.widthPx ?? 0),
    screenHeightPx: Number(parsed.screenHeightPx ?? screen.heightPx ?? 0),
    screenDensityDpi: Number(parsed.screenDensityDpi ?? screen.densityDpi ?? 0),
    screenDensity: Number(parsed.screenDensity ?? 0),
    hdrCapable: Boolean(parsed.hdrCapable ?? screen.hdr ?? false),
    totalRamBytes: Number(parsed.totalRamBytes ?? 0),
    cpuAbi: Array.isArray(abi) ? (abi as string[]) : [],
    kernelVersion: (parsed.kernelVersion as string | null) ?? null,
  };
}

export async function getBenchmarkHistory(): Promise<BenchmarkHistoryItem[]> {
  const module = getModule();
  const raw = await module.getBenchmarkHistory();
  const history = parseNativeJson<unknown>(raw);
  if (!Array.isArray(history)) {
    return [];
  }
  // Map phong truong hop native cu tra key khac (timestampMs / memScore).
  return (history as Record<string, unknown>[]).map((item) => ({
    id: String(item.id ?? ''),
    startedAtMs: Number(item.startedAtMs ?? item.timestampMs ?? 0),
    endedAtMs: Number(item.endedAtMs ?? item.timestampMs ?? 0),
    totalScore: Number(item.totalScore ?? 0),
    cpuScore: Number(item.cpuScore ?? 0),
    memoryScore: Number(item.memoryScore ?? item.memScore ?? 0),
    storageScore: Number(item.storageScore ?? 0),
    gpuScore: Number(item.gpuScore ?? 0),
    deviceModel: String(item.deviceModel ?? ''),
    stressDropPct:
      item.stressDropPct === null || item.stressDropPct === undefined
        ? null
        : Number(item.stressDropPct),
  }));
}

export async function runBenchmark(
  mode: 'single' | 'multi' | 'full' | 'stress',
): Promise<BenchmarkResult> {
  const module = getModule();
  const raw = await module.runBenchmark(mode);
  const result = parseNativeJson<NativeBenchmarkResult>(raw);
  const mapped: BenchmarkResult = {
    id: result.id,
    startedAtMs: result.startedAtMs,
    endedAtMs: result.endedAtMs,
    cpuSingleMs: result.cpuSingleMs,
    cpuMultiMs: result.cpuMultiMs,
    sha256Ms: result.sha256Ms,
    gzipMs: result.gzipMs,
    matmulMs: result.matmulMs,
    sortMs: result.sortMs,
    memReadMBps: result.memReadMBps,
    memWriteMBps: result.memWriteMBps,
    memRandomLatencyNs: result.memRandomLatencyNs,
    storageReadMBps: result.storageReadMBps,
    storageWriteMBps: result.storageWriteMBps,
    gpuFps: result.gpuFps,
    gpuJankPct: result.gpuJankPct,
    stressDropPct: result.stressDropPct,
    scores: result.scores,
    deviceModel: result.deviceModel,
  };
  return mapped;
}

export async function runStorageSpeedTest(): Promise<{
  readMBps: number;
  writeMBps: number;
}> {
  const module = getModule();
  const raw = await module.runStorageSpeedTest();
  const parsed = parseNativeJson<Record<string, unknown>>(raw);
  // Native tra seqReadMBps / seqWriteMBps (xem BenchmarkModule).
  return {
    readMBps: Number(parsed.readMBps ?? parsed.seqReadMBps ?? 0),
    writeMBps: Number(parsed.writeMBps ?? parsed.seqWriteMBps ?? 0),
  };
}

export async function requestOverlayPermission(): Promise<boolean> {
  const module = getModule();
  return module.requestOverlayPermission();
}

export async function startOverlay(config: OverlayConfig): Promise<void> {
  const module = getModule();
  await module.startOverlay(JSON.stringify(config));
}

export async function stopOverlay(): Promise<void> {
  if (!isNativeAvailable()) {
    return;
  }
  await getModule().stopOverlay();
}

export async function setAlertThresholds(thresholds: AlertThresholds): Promise<void> {
  const module = getModule();
  await module.setAlertThresholds(JSON.stringify(thresholds));
}

export type MetricsBatchCallback = (snapshots: MetricsSnapshot[]) => void;

export function subscribeBatch(callback: MetricsBatchCallback): () => void {
  if (!isNativeAvailable()) {
    throw new Error(
      'Native module BenchmarkModule chưa khả dụng nên không thể nhận batch realtime.',
    );
  }
  const subscription = getEmitter().addListener('onMetricsBatch', (payload: unknown) => {
    if (Array.isArray(payload)) {
      callback(payload as MetricsSnapshot[]);
      return;
    }
    if (payload && typeof payload === 'object') {
      const obj = payload as Record<string, unknown>;
      // Native gui {batch: "[{snapshot}]"} (xem BenchmarkModule).
      // Truong hop nay la dang that chay, phai parse chuoi JSON.
      if ('batch' in obj) {
        const batch = parseNativeJson<unknown>(obj.batch);
        if (Array.isArray(batch)) {
          callback(batch as MetricsSnapshot[]);
        }
        return;
      }
      if ('snapshots' in obj) {
        const batch = obj.snapshots;
        if (Array.isArray(batch)) {
          callback(batch as MetricsSnapshot[]);
        }
      }
    }
  });
  return () => {
    subscription.remove();
  };
}

export function nativeAvailable(): boolean {
  return isNativeAvailable();
}
