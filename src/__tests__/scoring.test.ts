import {
  clamp,
  computeScores,
  computeTotalScore,
  normalizeHigherBetter,
  normalizeLowerBetter,
  scoreCpu,
  scoreGpu,
  scoreLabel,
  scoreMemory,
  scoreStorage,
} from '../utils/scoring';

describe('clamp', () => {
  it('clamps below min and above max', () => {
    expect(clamp(-5, 0, 1000)).toBe(0);
    expect(clamp(1500, 0, 1000)).toBe(1000);
    expect(clamp(500, 0, 1000)).toBe(500);
  });
});

describe('normalizeHigherBetter', () => {
  it('returns 1000 at reference and 0 at floor', () => {
    expect(normalizeHigherBetter(20000, 20000, 0)).toBe(1000);
    expect(normalizeHigherBetter(0, 20000, 0)).toBe(0);
    expect(normalizeHigherBetter(10000, 20000, 0)).toBe(500);
  });
});

describe('normalizeLowerBetter', () => {
  it('returns 1000 at reference and 0 at worst', () => {
    expect(normalizeLowerBetter(100, 100, 5000)).toBe(1000);
    expect(normalizeLowerBetter(5000, 100, 5000)).toBe(0);
  });
});

describe('component scores', () => {
  it('scores a fast device higher than a slow device', () => {
    const fast = scoreCpu({
      cpuSingleMs: 120,
      cpuMultiMs: 60,
      sha256Ms: 60,
      gzipMs: 60,
      matmulMs: 60,
      sortMs: 60,
    });
    const slow = scoreCpu({
      cpuSingleMs: 4000,
      cpuMultiMs: 2500,
      sha256Ms: 2500,
      gzipMs: 2500,
      matmulMs: 4000,
      sortMs: 2500,
    });
    expect(fast).toBeGreaterThan(slow);
    expect(fast).toBeGreaterThan(800);
    expect(slow).toBeLessThan(300);
  });

  it('scores memory by bandwidth and latency', () => {
    const score = scoreMemory({
      memReadMBps: 20000,
      memWriteMBps: 15000,
      memRandomLatencyNs: 50,
    });
    expect(score).toBe(1000);
    const slow = scoreMemory({
      memReadMBps: 1000,
      memWriteMBps: 1000,
      memRandomLatencyNs: 500,
    });
    expect(slow).toBeLessThan(200);
  });

  it('scores storage by read/write', () => {
    expect(scoreStorage({ storageReadMBps: 1000, storageWriteMBps: 500 })).toBe(1000);
    expect(scoreStorage({ storageReadMBps: 0, storageWriteMBps: 0 })).toBe(0);
  });

  it('scores gpu by fps and jank', () => {
    expect(scoreGpu({ gpuFps: 120, gpuJankPct: 0 })).toBe(1000);
    expect(scoreGpu({ gpuFps: 0, gpuJankPct: 50 })).toBe(0);
  });
});

describe('computeTotalScore', () => {
  it('applies weights 0.4 cpu + 0.2 mem + 0.2 storage + 0.2 gpu', () => {
    expect(computeTotalScore({ cpu: 1000, memory: 1000, storage: 1000, gpu: 1000 })).toBe(1000);
    expect(computeTotalScore({ cpu: 1000, memory: 0, storage: 0, gpu: 0 })).toBe(400);
    expect(computeTotalScore({ cpu: 0, memory: 1000, storage: 0, gpu: 0 })).toBe(200);
    expect(computeTotalScore({ cpu: 0, memory: 0, storage: 0, gpu: 0 })).toBe(0);
  });
});

describe('computeScores', () => {
  it('returns total consistent with weights', () => {
    const result = computeScores({
      cpuSingleMs: 100,
      cpuMultiMs: 50,
      sha256Ms: 50,
      gzipMs: 50,
      matmulMs: 50,
      sortMs: 50,
      memReadMBps: 20000,
      memWriteMBps: 15000,
      memRandomLatencyNs: 50,
      storageReadMBps: 1000,
      storageWriteMBps: 500,
      gpuFps: 120,
      gpuJankPct: 0,
    });
    expect(result.total).toBe(1000);
    expect(result.cpu).toBe(1000);
    expect(result.memory).toBe(1000);
    expect(result.storage).toBe(1000);
    expect(result.gpu).toBe(1000);
  });

  it('clamps total to 0-1000', () => {
    const result = computeScores({
      cpuSingleMs: 99999,
      cpuMultiMs: 99999,
      sha256Ms: 99999,
      gzipMs: 99999,
      matmulMs: 99999,
      sortMs: 99999,
      memReadMBps: 0,
      memWriteMBps: 0,
      memRandomLatencyNs: 9999,
      storageReadMBps: 0,
      storageWriteMBps: 0,
      gpuFps: 0,
      gpuJankPct: 100,
    });
    expect(result.total).toBeGreaterThanOrEqual(0);
    expect(result.total).toBeLessThanOrEqual(1000);
  });
});

describe('scoreLabel', () => {
  it('labels bands', () => {
    expect(scoreLabel(900)).toBe('Xuất sắc');
    expect(scoreLabel(700)).toBe('Tốt');
    expect(scoreLabel(500)).toBe('Trung bình');
    expect(scoreLabel(300)).toBe('Yếu');
    expect(scoreLabel(100)).toBe('Rất yếu');
  });
});
