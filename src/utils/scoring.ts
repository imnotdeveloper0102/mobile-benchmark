export const SCORE_MAX = 1000;

export const SCORE_WEIGHTS = {
  cpu: 0.4,
  memory: 0.2,
  storage: 0.2,
  gpu: 0.2,
} as const;

export function clamp(value: number, min: number, max: number): number {
  if (!Number.isFinite(value)) {
    return min;
  }
  if (value < min) {
    return min;
  }
  if (value > max) {
    return max;
  }
  return value;
}

/**
 * Chuẩn hoá một giá trị đo (càng lớn càng tốt) về thang 0-1000.
 * reference là giá trị tham chiếu cho điểm 1000, floor là giá trị cho điểm 0.
 * Công thức khớp ScoreCalculator Kotlin: score = clamp((value - floor) / (reference - floor)) * 1000.
 */
export function normalizeHigherBetter(
  value: number,
  reference: number,
  floor = 0,
): number {
  if (reference <= floor) {
    return 0;
  }
  return clamp(((value - floor) / (reference - floor)) * SCORE_MAX, 0, SCORE_MAX);
}

/**
 * Chuẩn hoá một giá trị đo (càng nhỏ càng tốt, ví dụ thời gian ms) về thang 0-1000.
 * reference là thời gian tham chiếu cho điểm 1000, worst là thời gian cho điểm 0.
 */
export function normalizeLowerBetter(
  value: number,
  reference: number,
  worst: number,
): number {
  if (worst <= reference) {
    return 0;
  }
  return clamp(((worst - value) / (worst - reference)) * SCORE_MAX, 0, SCORE_MAX);
}

export interface RawBenchmarkInputs {
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
}

export interface ComponentScores {
  cpu: number;
  memory: number;
  storage: number;
  gpu: number;
  total: number;
}

export function scoreCpu(inputs: Pick<RawBenchmarkInputs, 'cpuSingleMs' | 'cpuMultiMs' | 'sha256Ms' | 'gzipMs' | 'matmulMs' | 'sortMs'>): number {
  const single = normalizeLowerBetter(inputs.cpuSingleMs, 100, 5000);
  const multi = normalizeLowerBetter(inputs.cpuMultiMs, 50, 3000);
  const sha = normalizeLowerBetter(inputs.sha256Ms, 50, 3000);
  const gzip = normalizeLowerBetter(inputs.gzipMs, 50, 3000);
  const matmul = normalizeLowerBetter(inputs.matmulMs, 50, 5000);
  const sort = normalizeLowerBetter(inputs.sortMs, 50, 3000);
  const raw = 0.25 * single + 0.25 * multi + 0.15 * sha + 0.1 * gzip + 0.15 * matmul + 0.1 * sort;
  return clamp(raw, 0, SCORE_MAX);
}

export function scoreMemory(
  inputs: Pick<RawBenchmarkInputs, 'memReadMBps' | 'memWriteMBps' | 'memRandomLatencyNs'>,
): number {
  const read = normalizeHigherBetter(inputs.memReadMBps, 20000, 0);
  const write = normalizeHigherBetter(inputs.memWriteMBps, 15000, 0);
  const latency = normalizeLowerBetter(inputs.memRandomLatencyNs, 50, 500);
  const raw = 0.4 * read + 0.4 * write + 0.2 * latency;
  return clamp(raw, 0, SCORE_MAX);
}

export function scoreStorage(
  inputs: Pick<RawBenchmarkInputs, 'storageReadMBps' | 'storageWriteMBps'>,
): number {
  const read = normalizeHigherBetter(inputs.storageReadMBps, 1000, 0);
  const write = normalizeHigherBetter(inputs.storageWriteMBps, 500, 0);
  const raw = 0.5 * read + 0.5 * write;
  return clamp(raw, 0, SCORE_MAX);
}

export function scoreGpu(inputs: Pick<RawBenchmarkInputs, 'gpuFps' | 'gpuJankPct'>): number {
  const fps = normalizeHigherBetter(inputs.gpuFps, 120, 0);
  const jank = normalizeLowerBetter(inputs.gpuJankPct, 0, 50);
  const raw = 0.7 * fps + 0.3 * jank;
  return clamp(raw, 0, SCORE_MAX);
}

export function computeTotalScore(component: Omit<ComponentScores, 'total'>): number {
  const raw =
    SCORE_WEIGHTS.cpu * component.cpu +
    SCORE_WEIGHTS.memory * component.memory +
    SCORE_WEIGHTS.storage * component.storage +
    SCORE_WEIGHTS.gpu * component.gpu;
  return clamp(raw, 0, SCORE_MAX);
}

export function computeScores(inputs: RawBenchmarkInputs): ComponentScores {
  const cpu = scoreCpu(inputs);
  const memory = scoreMemory(inputs);
  const storage = scoreStorage(inputs);
  const gpu = scoreGpu(inputs);
  const total = computeTotalScore({ cpu, memory, storage, gpu });
  return { cpu, memory, storage, gpu, total };
}

export function scoreLabel(total: number): string {
  if (total >= 800) {
    return 'Xuất sắc';
  }
  if (total >= 600) {
    return 'Tốt';
  }
  if (total >= 400) {
    return 'Trung bình';
  }
  if (total >= 200) {
    return 'Yếu';
  }
  return 'Rất yếu';
}
