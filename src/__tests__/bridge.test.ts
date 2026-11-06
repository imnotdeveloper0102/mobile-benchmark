/**
 * Mock NativeModules ở đây CHỈ dùng cho test bridge, không phải mock dữ liệu app.
 * App thật luôn gọi native module thật; khi native chưa có, bridge ném lỗi rõ ràng.
 */
import { NativeModules } from 'react-native';

// Khong dung requireActual('react-native'): ban that keo theo DevMenu
// TurboModule va crash trong Jest. Mock toi thieu du cho bridge.
jest.mock('react-native', () => {
  return {
    NativeModules: {
      BenchmarkModule: {
        startSampling: jest.fn().mockResolvedValue(undefined),
        stopSampling: jest.fn().mockResolvedValue(undefined),
        getSnapshot: jest.fn().mockResolvedValue({
          timestampMs: 1000,
          cpu: {
            cores: 8,
            freqMHz: [1800],
            maxFreqMHz: [2800],
            loadEstimatePct: 50,
            loadMethod: 'freq',
            loadNote: '',
          },
          memory: {
            totalBytes: 1000,
            availBytes: 500,
            usedBytes: 500,
            lowMemory: false,
            thresholdBytes: 100,
            appPssBytes: 50,
          },
          thermal: {
            batteryTempC: 35,
            cpuTempC: null,
            gpuTempC: null,
            thermalStatus: 'none',
            overheating: false,
          },
          battery: {
            levelPct: 80,
            charging: false,
            currentNowUa: null,
            voltageMv: 4000,
            powerWatts: null,
            health: 'good',
            timeRemainingMin: null,
          },
          fps: { fps: 60, jankPct: 0, refreshRateHz: 60, note: '' },
          storage: {
            internalTotalBytes: 1000,
            internalAvailBytes: 500,
            externalTotalBytes: null,
          },
          network: { rxBytesPerSec: 0, txBytesPerSec: 0, type: 'wifi', signalDbm: null },
          unavailable: [],
        }),
        getDeviceInfo: jest.fn().mockResolvedValue({
          model: 'Test Model',
          manufacturer: 'Test',
          brand: 'Test',
          socModel: null,
          gpuRenderer: null,
          androidVersion: '14',
          apiLevel: 34,
          securityPatch: '2024-01-01',
          buildId: 'TEST',
          board: 'test',
          hardware: 'test',
          sensors: [],
          screenWidthPx: 1080,
          screenHeightPx: 2400,
          screenDensityDpi: 420,
          screenDensity: 2.625,
          hdrCapable: false,
          totalRamBytes: 8000000000,
          cpuAbi: ['arm64-v8a'],
          kernelVersion: null,
        }),
        getBenchmarkHistory: jest.fn().mockResolvedValue([]),
        runBenchmark: jest.fn().mockResolvedValue({
          id: 'run-1',
          startedAtMs: 1000,
          endedAtMs: 2000,
          cpuSingleMs: 100,
          cpuMultiMs: 50,
          sha256Ms: 50,
          gzipMs: 50,
          matmulMs: 50,
          sortMs: 50,
          memReadMBps: 10000,
          memWriteMBps: 8000,
          memRandomLatencyNs: 100,
          storageReadMBps: 500,
          storageWriteMBps: 250,
          gpuFps: 60,
          gpuJankPct: 5,
          stressDropPct: null,
          scores: { cpu: 900, memory: 800, storage: 700, gpu: 800, total: 820 },
          deviceModel: 'Test Model',
        }),
        runStorageSpeedTest: jest.fn().mockResolvedValue({ readMBps: 500, writeMBps: 250 }),
        requestOverlayPermission: jest.fn().mockResolvedValue(true),
        startOverlay: jest.fn().mockResolvedValue(undefined),
        stopOverlay: jest.fn().mockResolvedValue(undefined),
        setAlertThresholds: jest.fn().mockResolvedValue(undefined),
      },
    },
    NativeEventEmitter: jest.fn().mockImplementation(() => ({
      addListener: jest.fn().mockReturnValue({ remove: jest.fn() }),
    })),
  };
});

import {
  getBenchmarkHistory,
  getDeviceInfo,
  getSnapshot,
  nativeAvailable,
  requestOverlayPermission,
  runBenchmark,
  runStorageSpeedTest,
  startOverlay,
  startSampling,
  stopSampling,
} from '../services/bridge';

describe('bridge', () => {
  it('reports native as available when mocked module exists', () => {
    expect(nativeAvailable()).toBe(true);
  });

  it('startSampling forwards intervalMs', async () => {
    await startSampling(1000);
    expect(
      (NativeModules.BenchmarkModule as unknown as { startSampling: jest.Mock }).startSampling,
    ).toHaveBeenCalledWith(1000);
  });

  it('stopSampling resolves', async () => {
    await expect(stopSampling()).resolves.toBeUndefined();
  });

  it('getSnapshot returns the native snapshot', async () => {
    const snapshot = await getSnapshot();
    expect(snapshot.timestampMs).toBe(1000);
    expect(snapshot.cpu.cores).toBe(8);
  });

  it('getDeviceInfo returns device model', async () => {
    const info = await getDeviceInfo();
    expect(info.model).toBe('Test Model');
    expect(info.socModel).toBeNull();
  });

  it('getBenchmarkHistory returns an array', async () => {
    await expect(getBenchmarkHistory()).resolves.toEqual([]);
  });

  it('runBenchmark maps native result to BenchmarkResult', async () => {
    const result = await runBenchmark('full');
    expect(result.id).toBe('run-1');
    expect(result.scores.total).toBe(820);
    expect(result.stressDropPct).toBeNull();
  });

  it('runStorageSpeedTest returns read/write', async () => {
    await expect(runStorageSpeedTest()).resolves.toEqual({ readMBps: 500, writeMBps: 250 });
  });

  it('overlay permission and start/stop resolve', async () => {
    await expect(requestOverlayPermission()).resolves.toBe(true);
    await expect(
      startOverlay({ enabledMetrics: ['cpu'], opacity: 0.85, size: 'medium' }),
    ).resolves.toBeUndefined();
    await expect(stopSampling()).resolves.toBeUndefined();
  });
});
