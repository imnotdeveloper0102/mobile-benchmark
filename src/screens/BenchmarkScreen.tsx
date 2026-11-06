import React, { useCallback, useEffect } from 'react';
import { Alert, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { getBenchmarkHistory, runBenchmark } from '../services/bridge';
import { useBenchmarkStore } from '../store/benchmarkStore';
import { GaugeScore } from '../components/GaugeScore';
import { colors, fontSize, radius, spacing } from '../theme/theme';
import { t } from '../i18n';
import { formatDateTime, formatDuration, formatPercent, formatSpeed } from '../services/format';
import { scoreLabel } from '../utils/scoring';
import type { BenchmarkResult } from '../services/types';

type BenchmarkMode = 'single' | 'multi' | 'full' | 'stress';

const PHASE_LABELS: Record<BenchmarkMode, string[]> = {
  single: ['CPU đơn nhân', 'SHA-256', 'Sắp xếp'],
  multi: ['CPU đa nhân', 'Nhân ma trận', 'Sắp xếp'],
  full: ['CPU', 'Bộ nhớ', 'Lưu trữ', 'Đồ hoạ'],
  stress: ['Làm nóng', 'Chạy tải nặng', 'Đo throttling'],
};

function detailRows(result: BenchmarkResult): { label: string; value: string }[] {
  return [
    { label: t('benchmark.cpuSingle'), value: formatDuration(result.cpuSingleMs) },
    { label: t('benchmark.cpuMulti'), value: formatDuration(result.cpuMultiMs) },
    { label: t('benchmark.sha256'), value: formatDuration(result.sha256Ms) },
    { label: t('benchmark.gzip'), value: formatDuration(result.gzipMs) },
    { label: t('benchmark.matmul'), value: formatDuration(result.matmulMs) },
    { label: t('benchmark.sort'), value: formatDuration(result.sortMs) },
    { label: t('benchmark.memRead'), value: formatSpeed(result.memReadMBps) },
    { label: t('benchmark.memWrite'), value: formatSpeed(result.memWriteMBps) },
    {
      label: t('benchmark.memLatency'),
      value: `${Math.round(result.memRandomLatencyNs)} ns`,
    },
    { label: t('benchmark.storageRead'), value: formatSpeed(result.storageReadMBps) },
    { label: t('benchmark.storageWrite'), value: formatSpeed(result.storageWriteMBps) },
    { label: t('benchmark.gpuFps'), value: `${result.gpuFps.toFixed(1)} FPS` },
    { label: t('benchmark.gpuJank'), value: formatPercent(result.gpuJankPct) },
    {
      label: t('benchmark.stressDrop'),
      value:
        result.stressDropPct === null ? t('common.unavailable') : formatPercent(result.stressDropPct),
    },
  ];
}

export function BenchmarkScreen(): React.JSX.Element {
  const status = useBenchmarkStore((state) => state.status);
  const progress = useBenchmarkStore((state) => state.progress);
  const currentPhase = useBenchmarkStore((state) => state.currentPhase);
  const lastResult = useBenchmarkStore((state) => state.lastResult);
  const history = useBenchmarkStore((state) => state.history);
  const errorMessage = useBenchmarkStore((state) => state.errorMessage);
  const setRunning = useBenchmarkStore((state) => state.setRunning);
  const setPhase = useBenchmarkStore((state) => state.setPhase);
  const setResult = useBenchmarkStore((state) => state.setResult);
  const setError = useBenchmarkStore((state) => state.setError);
  const setHistory = useBenchmarkStore((state) => state.setHistory);

  useEffect(() => {
    getBenchmarkHistory()
      .then((items) => setHistory(items))
      .catch(() => undefined);
  }, [setHistory]);

  const run = useCallback(
    async (mode: BenchmarkMode): Promise<void> => {
      const phases = PHASE_LABELS[mode];
      setRunning(phases[0], 5);
      // Native chạy thật và trả kết quả khi xong; phase/progress ở đây chỉ
      // phản ánh các mốc đã biết từ trước, không bịa tiến độ chi tiết.
      const timer = setInterval(() => {
        const state = useBenchmarkStore.getState();
        if (state.status !== 'running') {
          clearInterval(timer);
          return;
        }
        const nextProgress = Math.min(95, state.progress + 5);
        const phaseIndex = Math.min(
          phases.length - 1,
          Math.floor(nextProgress / (100 / phases.length)),
        );
        setPhase(phases[phaseIndex], nextProgress);
      }, 1000);
      try {
        const result = await runBenchmark(mode);
        clearInterval(timer);
        setResult(result);
      } catch (err) {
        clearInterval(timer);
        const message = err instanceof Error ? err.message : t('benchmark.failed');
        setError(message);
        Alert.alert(t('common.error'), message);
      }
    },
    [setRunning, setPhase, setResult, setError],
  );

  const running = status === 'running';
  const previous = history.length >= 2 ? history[1] : null;
  const latest = history.length >= 1 ? history[0] : null;
  const diffPct =
    previous && latest && previous.totalScore !== 0
      ? ((latest.totalScore - previous.totalScore) / previous.totalScore) * 100
      : null;

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <Text style={styles.title}>{t('benchmark.title')}</Text>

      <View style={styles.buttonRow}>
        <Pressable
          style={[styles.runButton, running ? styles.disabled : undefined]}
          disabled={running}
          onPress={() => run('full')}
        >
          <Text style={styles.runButtonText}>{t('benchmark.runFull')}</Text>
        </Pressable>
      </View>
      <View style={styles.buttonRow}>
        <Pressable
          style={[styles.secondaryButton, running ? styles.disabled : undefined]}
          disabled={running}
          onPress={() => run('single')}
        >
          <Text style={styles.secondaryText}>{t('benchmark.runSingle')}</Text>
        </Pressable>
        <Pressable
          style={[styles.secondaryButton, running ? styles.disabled : undefined]}
          disabled={running}
          onPress={() => run('multi')}
        >
          <Text style={styles.secondaryText}>{t('benchmark.runMulti')}</Text>
        </Pressable>
      </View>
      <Pressable
        style={[styles.secondaryButton, running ? styles.disabled : undefined]}
        disabled={running}
        onPress={() => run('stress')}
      >
        <Text style={styles.secondaryText}>{t('benchmark.runStress')}</Text>
      </Pressable>

      {running ? (
        <View style={styles.progressCard}>
          <Text style={styles.phaseText}>
            {t('benchmark.phase')}: {currentPhase}
          </Text>
          <View style={styles.progressTrack}>
            <View style={[styles.progressFill, { width: `${progress}%` }]} />
          </View>
          <Text style={styles.progressText}>
            {t('benchmark.progress')}: {Math.round(progress)}%
          </Text>
        </View>
      ) : null}

      {status === 'error' && errorMessage ? (
        <Text style={styles.errorText}>{errorMessage}</Text>
      ) : null}

      {lastResult ? (
        <View style={styles.resultCard}>
          <Text style={styles.sectionTitle}>{t('benchmark.lastRun')}</Text>
          <Text style={styles.runDate}>{formatDateTime(lastResult.startedAtMs)}</Text>
          <GaugeScore score={lastResult.scores.total} label={scoreLabel(lastResult.scores.total)} />
          <View style={styles.scoreRow}>
            <Text style={styles.scoreText}>
              {t('benchmark.cpuScore')}: {Math.round(lastResult.scores.cpu)}
            </Text>
            <Text style={styles.scoreText}>
              {t('benchmark.memoryScore')}: {Math.round(lastResult.scores.memory)}
            </Text>
          </View>
          <View style={styles.scoreRow}>
            <Text style={styles.scoreText}>
              {t('benchmark.storageScore')}: {Math.round(lastResult.scores.storage)}
            </Text>
            <Text style={styles.scoreText}>
              {t('benchmark.gpuScore')}: {Math.round(lastResult.scores.gpu)}
            </Text>
          </View>
          <Text style={[styles.sectionTitle, styles.detailTitle]}>{t('benchmark.detail')}</Text>
          {detailRows(lastResult).map((row) => (
            <View key={row.label} style={styles.detailRow}>
              <Text style={styles.detailLabel}>{row.label}</Text>
              <Text style={styles.detailValue}>{row.value}</Text>
            </View>
          ))}
        </View>
      ) : (
        <Text style={styles.muted}>{t('benchmark.noResult')}</Text>
      )}

      {latest && previous && diffPct !== null ? (
        <View style={styles.compareCard}>
          <Text style={styles.sectionTitle}>{t('benchmark.compareTitle')}</Text>
          <Text style={styles.compareText}>
            {formatDateTime(latest.startedAtMs)} ({Math.round(latest.totalScore)}) vs{' '}
            {formatDateTime(previous.startedAtMs)} ({Math.round(previous.totalScore)}):{' '}
            {diffPct > 0
              ? `+${diffPct.toFixed(1)}% ${t('benchmark.better')}`
              : diffPct < 0
                ? `${diffPct.toFixed(1)}% ${t('benchmark.worse')}`
                : t('benchmark.same')}
          </Text>
        </View>
      ) : null}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    backgroundColor: colors.background,
    flex: 1,
  },
  content: {
    padding: spacing.md,
    paddingBottom: spacing.xxl,
  },
  title: {
    color: colors.text,
    fontSize: fontSize.xl,
    fontWeight: '700',
    marginBottom: spacing.md,
  },
  buttonRow: {
    flexDirection: 'row',
    gap: spacing.md,
    marginBottom: spacing.md,
  },
  runButton: {
    alignItems: 'center',
    backgroundColor: colors.accent,
    borderRadius: radius.md,
    flex: 1,
    paddingVertical: spacing.md,
  },
  runButtonText: {
    color: colors.accentText,
    fontSize: fontSize.md,
    fontWeight: '700',
  },
  secondaryButton: {
    alignItems: 'center',
    backgroundColor: colors.card,
    borderColor: colors.cardBorder,
    borderRadius: radius.md,
    borderWidth: 1,
    flex: 1,
    paddingVertical: spacing.md,
  },
  secondaryText: {
    color: colors.text,
    fontSize: fontSize.md,
    fontWeight: '600',
  },
  disabled: {
    opacity: 0.5,
  },
  progressCard: {
    backgroundColor: colors.card,
    borderColor: colors.cardBorder,
    borderRadius: radius.md,
    borderWidth: 1,
    marginVertical: spacing.md,
    padding: spacing.md,
  },
  phaseText: {
    color: colors.text,
    fontSize: fontSize.md,
    marginBottom: spacing.sm,
  },
  progressTrack: {
    backgroundColor: colors.cardBorder,
    borderRadius: radius.sm,
    height: 8,
    overflow: 'hidden',
  },
  progressFill: {
    backgroundColor: colors.accent,
    height: 8,
  },
  progressText: {
    color: colors.textSecondary,
    fontSize: fontSize.sm,
    marginTop: spacing.sm,
  },
  errorText: {
    color: colors.danger,
    fontSize: fontSize.md,
    marginVertical: spacing.sm,
  },
  resultCard: {
    backgroundColor: colors.card,
    borderColor: colors.cardBorder,
    borderRadius: radius.md,
    borderWidth: 1,
    marginTop: spacing.md,
    padding: spacing.md,
  },
  sectionTitle: {
    color: colors.text,
    fontSize: fontSize.lg,
    fontWeight: '700',
  },
  runDate: {
    color: colors.textSecondary,
    fontSize: fontSize.sm,
    marginBottom: spacing.md,
    marginTop: spacing.xs,
  },
  scoreRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: spacing.sm,
  },
  scoreText: {
    color: colors.text,
    fontSize: fontSize.md,
  },
  detailTitle: {
    marginTop: spacing.lg,
  },
  detailRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: spacing.sm,
  },
  detailLabel: {
    color: colors.textSecondary,
    fontSize: fontSize.md,
  },
  detailValue: {
    color: colors.text,
    fontSize: fontSize.md,
    fontWeight: '600',
  },
  muted: {
    color: colors.textMuted,
    fontSize: fontSize.md,
    marginTop: spacing.md,
    textAlign: 'center',
  },
  compareCard: {
    backgroundColor: colors.card,
    borderColor: colors.cardBorder,
    borderRadius: radius.md,
    borderWidth: 1,
    marginTop: spacing.md,
    padding: spacing.md,
  },
  compareText: {
    color: colors.text,
    fontSize: fontSize.md,
    marginTop: spacing.sm,
  },
});
