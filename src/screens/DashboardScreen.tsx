import React, { useMemo } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useMetrics } from '../hooks/useMetrics';
import { useMetricsStore } from '../store/metricsStore';
import { MetricCard } from '../components/MetricCard';
import { RealtimeChart } from '../components/RealtimeChart';
import { SectionHeader } from '../components/SectionHeader';
import { colors, fontSize, radius, spacing } from '../theme/theme';
import { t } from '../i18n';
import {
  formatBytes,
  formatBytesPerSec,
  formatMHz,
  formatPercent,
  formatTemp,
  formatWatts,
  isFieldUnavailable,
  unavailableReason,
} from '../services/format';
import { bucketMsForWindow, downsample, filterByWindow } from '../utils/aggregate';
import type { MetricWindow } from '../services/types';

const WINDOWS: MetricWindow[] = ['1m', '5m', '1h'];

export function DashboardScreen(): React.JSX.Element {
  const { snapshot, history, paused, window, error, pause, resume } = useMetrics();
  const setWindow = useMetricsStore((state) => state.setWindow);

  const windowHistory = useMemo(() => {
    if (history.length === 0) {
      return [];
    }
    const now = history[history.length - 1].timestampMs;
    return filterByWindow(history, window, now);
  }, [history, window]);

  const cpuSeries = useMemo(
    () =>
      downsample(
        windowHistory,
        (item) => item.cpu.loadEstimatePct,
        bucketMsForWindow(window),
      ).map((point) => point.value),
    [windowHistory, window],
  );

  const tempSeries = useMemo(
    () =>
      downsample(
        windowHistory,
        (item) => item.thermal.batteryTempC,
        bucketMsForWindow(window),
      ).map((point) => point.value),
    [windowHistory, window],
  );

  const windowLabel =
    window === '1m' ? t('dashboard.window1m') : window === '5m' ? t('dashboard.window5m') : t('dashboard.window1h');

  if (error && !snapshot) {
    return (
      <View style={styles.center}>
        <Text style={styles.errorText}>{error}</Text>
      </View>
    );
  }

  if (!snapshot) {
    return (
      <View style={styles.center}>
        <Text style={styles.muted}>{t('dashboard.noData')}</Text>
      </View>
    );
  }

  const avgFreq =
    snapshot.cpu.freqMHz.length > 0
      ? snapshot.cpu.freqMHz.reduce((acc, value) => acc + value, 0) / snapshot.cpu.freqMHz.length
      : 0;
  const ramUsedPct =
    snapshot.memory.totalBytes > 0
      ? (snapshot.memory.usedBytes / snapshot.memory.totalBytes) * 100
      : null;

  const updatedAt = new Date(snapshot.timestampMs).toLocaleTimeString('vi-VN', { hour12: false });

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <View style={styles.liveRow}>
        <View style={[styles.liveDot, paused ? styles.liveDotPaused : undefined]} />
        <Text style={styles.liveText}>{paused ? t('dashboard.paused') : t('dashboard.live')}</Text>
        <Text style={styles.liveTime}>{updatedAt}</Text>
        <Text style={styles.liveTime}>1 Hz</Text>
      </View>

      <View style={styles.toolbar}>
        <View style={styles.windowRow}>
          {WINDOWS.map((item) => (
            <Pressable
              key={item}
              onPress={() => setWindow(item)}
              style={[styles.windowButton, window === item ? styles.windowButtonActive : undefined]}
            >
              <Text style={[styles.windowText, window === item ? styles.windowTextActive : undefined]}>
                {item === '1m'
                  ? t('dashboard.window1m')
                  : item === '5m'
                    ? t('dashboard.window5m')
                    : t('dashboard.window1h')}
              </Text>
            </Pressable>
          ))}
        </View>
        <Pressable onPress={paused ? resume : pause} style={styles.pauseButton}>
          <Text style={styles.pauseText}>
            {paused ? t('dashboard.resume') : t('dashboard.pause')}
          </Text>
        </Pressable>
      </View>

      {snapshot.thermal.overheating ? (
        <View style={styles.alertBanner}>
          <Text style={styles.alertText}>{t('dashboard.overheating')}</Text>
        </View>
      ) : null}
      {snapshot.memory.lowMemory ? (
        <View style={styles.alertBanner}>
          <Text style={styles.alertText}>{t('dashboard.lowMemory')}</Text>
        </View>
      ) : null}

      <SectionHeader label={t('dashboard.metrics')} right={t('dashboard.live')} />
      <View style={styles.grid}>
        <MetricCard
          label={t('dashboard.cpuLoad')}
          value={formatPercent(snapshot.cpu.loadEstimatePct)}
          subNote={`${snapshot.cpu.cores} nhân · ${t('dashboard.cpuFreq')} ${formatMHz(avgFreq)}`}
          unavailableReason={unavailableReason(snapshot, 'cpu.load')}
          progress={snapshot.cpu.loadEstimatePct}
        />
        <MetricCard
          label={t('dashboard.memory')}
          value={formatPercent(ramUsedPct)}
          subNote={`${t('dashboard.memoryUsed')} ${formatBytes(snapshot.memory.usedBytes)} / ${formatBytes(snapshot.memory.totalBytes)}`}
          progress={ramUsedPct}
          progressColor={colors.chartMemory}
        />
      </View>
      <View style={styles.grid}>
        <MetricCard
          label={t('dashboard.batteryTemp')}
          value={formatTemp(snapshot.thermal.batteryTempC)}
          subNote={snapshot.thermal.cpuTempC !== null ? `${t('dashboard.cpuTemp')}: ${formatTemp(snapshot.thermal.cpuTempC)}` : snapshot.thermal.thermalStatus}
          unavailableReason={unavailableReason(snapshot, 'thermal.batteryTemp')}
        />
        <MetricCard
          label={t('dashboard.batteryLevel')}
          value={formatPercent(snapshot.battery.levelPct, 0)}
          subNote={`${snapshot.battery.charging ? t('dashboard.charging') : t('dashboard.discharging')} · ${formatWatts(snapshot.battery.powerWatts)}`}
          progress={snapshot.battery.levelPct}
          progressColor={colors.chartBattery}
        />
      </View>
      <View style={styles.grid}>
        <MetricCard
          label={t('dashboard.fps')}
          value={snapshot.fps.fps.toFixed(1)}
          unit="FPS"
          subNote={`${t('benchmark.gpuJank')}: ${formatPercent(snapshot.fps.jankPct)} · ${snapshot.fps.refreshRateHz.toFixed(0)} Hz — ${snapshot.fps.note}`}
        />
        <MetricCard
          label={t('dashboard.network')}
          value={formatBytesPerSec(snapshot.network.rxBytesPerSec)}
          subNote={`${t('dashboard.networkUp')} ${formatBytesPerSec(snapshot.network.txBytesPerSec)} · ${snapshot.network.type}${snapshot.network.signalDbm !== null ? ` · ${snapshot.network.signalDbm} dBm` : ''}`}
          unavailableReason={
            isFieldUnavailable(snapshot, 'network.signal') && snapshot.network.signalDbm === null
              ? unavailableReason(snapshot, 'network.signal')
              : null
          }
        />
      </View>
      <View style={styles.grid}>
        <MetricCard
          label={t('dashboard.storage')}
          value={formatBytes(snapshot.storage.internalAvailBytes)}
          subNote={`${t('dashboard.storageAvail')} / ${formatBytes(snapshot.storage.internalTotalBytes)}`}
        />
        <MetricCard
          label={t('dashboard.memoryAvail')}
          value={formatBytes(snapshot.memory.availBytes)}
          subNote={`${t('dashboard.memory')} app: ${formatBytes(snapshot.memory.appPssBytes)}`}
        />
      </View>

      <SectionHeader label={t('dashboard.charts')} right={windowLabel} />
      <RealtimeChart
        title={t('dashboard.chartCpuLoad')}
        data={cpuSeries}
        color={colors.chartCpu}
        unit="%"
        min={0}
        max={100}
        windowLabel={windowLabel}
      />
      <RealtimeChart
        title={t('dashboard.chartBatteryTemp')}
        data={tempSeries}
        color={colors.chartThermal}
        unit="°C"
        min={20}
        max={60}
        windowLabel={windowLabel}
      />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    backgroundColor: colors.background,
    flex: 1,
  },
  liveRow: {
    alignItems: 'center',
    flexDirection: 'row',
    gap: spacing.sm,
    marginBottom: spacing.sm,
  },
  liveDot: {
    backgroundColor: colors.success,
    borderRadius: 4,
    height: 8,
    width: 8,
  },
  liveDotPaused: {
    backgroundColor: colors.warning,
  },
  liveText: {
    color: colors.text,
    fontSize: fontSize.xs,
    fontWeight: '700',
    letterSpacing: 1.2,
  },
  liveTime: {
    color: colors.textMuted,
    fontFamily: 'monospace',
    fontSize: fontSize.xs,
    marginLeft: 'auto',
  },
  content: {
    padding: spacing.md,
    paddingBottom: spacing.xxl,
  },
  center: {
    alignItems: 'center',
    backgroundColor: colors.background,
    flex: 1,
    justifyContent: 'center',
    padding: spacing.lg,
  },
  errorText: {
    color: colors.danger,
    fontSize: fontSize.md,
    textAlign: 'center',
  },
  muted: {
    color: colors.textMuted,
    fontSize: fontSize.md,
    textAlign: 'center',
  },
  toolbar: {
    alignItems: 'center',
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: spacing.md,
  },
  windowRow: {
    flexDirection: 'row',
    gap: spacing.sm,
  },
  windowButton: {
    backgroundColor: colors.card,
    borderColor: colors.cardBorder,
    borderRadius: radius.lg,
    borderWidth: 1,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
  },
  windowButtonActive: {
    backgroundColor: colors.accent,
    borderColor: colors.accent,
  },
  windowText: {
    color: colors.text,
    fontSize: fontSize.sm,
  },
  windowTextActive: {
    color: colors.accentText,
    fontWeight: '700',
  },
  pauseButton: {
    backgroundColor: colors.card,
    borderColor: colors.cardBorder,
    borderRadius: radius.lg,
    borderWidth: 1,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
  },
  pauseText: {
    color: colors.text,
    fontSize: fontSize.sm,
    fontWeight: '600',
  },
  alertBanner: {
    backgroundColor: '#3A1512',
    borderColor: colors.danger,
    borderRadius: radius.md,
    borderWidth: 1,
    marginBottom: spacing.sm,
    padding: spacing.md,
  },
  alertText: {
    color: colors.danger,
    fontSize: fontSize.md,
    fontWeight: '700',
  },
  grid: {
    flexDirection: 'row',
    gap: spacing.md,
    marginBottom: spacing.md,
  },
});
