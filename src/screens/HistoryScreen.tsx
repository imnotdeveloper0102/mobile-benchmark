import React, { useCallback, useEffect, useState } from 'react';
import { Alert, Pressable, ScrollView, Share, StyleSheet, Text, View } from 'react-native';
import RNFS from 'react-native-fs';
import { getBenchmarkHistory } from '../services/bridge';
import { useBenchmarkStore } from '../store/benchmarkStore';
import { useMetricsStore } from '../store/metricsStore';
import { colors, fontSize, radius, spacing } from '../theme/theme';
import { t } from '../i18n';
import { formatDateTime } from '../services/format';
import { detectAnomalies } from '../utils/aggregate';

function toCsv(): string {
  const history = useBenchmarkStore.getState().history;
  const header = 'id,startedAt,endedAt,totalScore,cpuScore,memoryScore,storageScore,gpuScore,deviceModel,stressDropPct';
  const rows = history.map((item) =>
    [
      item.id,
      new Date(item.startedAtMs).toISOString(),
      new Date(item.endedAtMs).toISOString(),
      Math.round(item.totalScore),
      Math.round(item.cpuScore),
      Math.round(item.memoryScore),
      Math.round(item.storageScore),
      Math.round(item.gpuScore),
      item.deviceModel,
      item.stressDropPct === null ? '' : item.stressDropPct.toFixed(1),
    ].join(','),
  );
  return [header, ...rows].join('\n');
}

export function HistoryScreen(): React.JSX.Element {
  const history = useBenchmarkStore((state) => state.history);
  const setHistory = useBenchmarkStore((state) => state.setHistory);
  const metricsHistory = useMetricsStore((state) => state.history);
  const [selectedId, setSelectedId] = useState<string | null>(null);

  useEffect(() => {
    getBenchmarkHistory()
      .then((items) => setHistory(items))
      .catch(() => undefined);
  }, [setHistory]);

  const anomalies = detectAnomalies(metricsHistory);

  const exportFile = useCallback(
    async (format: 'csv' | 'json'): Promise<void> => {
      try {
        const fileName =
          format === 'csv'
            ? `benchmark-history-${Date.now()}.csv`
            : `benchmark-history-${Date.now()}.json`;
        const content =
          format === 'csv'
            ? toCsv()
            : JSON.stringify(useBenchmarkStore.getState().history, null, 2);
        const path = `${RNFS.DocumentDirectoryPath}/${fileName}`;
        await RNFS.writeFile(path, content, 'utf8');
        await Share.share({ url: `file://${path}`, title: fileName });
      } catch (err) {
        Alert.alert(
          t('common.error'),
          err instanceof Error ? err.message : t('history.exportFailed'),
        );
      }
    },
    [],
  );

  const selected = history.find((item) => item.id === selectedId) ?? null;

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <Text style={styles.title}>{t('history.title')}</Text>

      {anomalies.length > 0 ? (
        <View style={styles.anomalyCard}>
          <Text style={styles.sectionTitle}>{t('history.anomaly')}</Text>
          {anomalies.map((anomaly, index) => (
            <Text key={`${anomaly.startedAtMs}-${index}`} style={styles.anomalyText}>
              • {anomaly.detail} ({formatDateTime(anomaly.startedAtMs)})
            </Text>
          ))}
        </View>
      ) : null}

      <View style={styles.buttonRow}>
        <Pressable style={styles.exportButton} onPress={() => exportFile('csv')}>
          <Text style={styles.exportText}>{t('history.exportCsv')}</Text>
        </Pressable>
        <Pressable style={styles.exportButton} onPress={() => exportFile('json')}>
          <Text style={styles.exportText}>{t('history.exportJson')}</Text>
        </Pressable>
      </View>

      <Text style={styles.sectionTitle}>{t('history.benchmarkRuns')}</Text>
      {history.length === 0 ? (
        <Text style={styles.muted}>{t('history.noSessions')}</Text>
      ) : (
        history.map((item) => (
          <Pressable
            key={item.id}
            onPress={() => setSelectedId(selectedId === item.id ? null : item.id)}
            style={styles.item}
          >
            <Text style={styles.itemTitle}>
              {formatDateTime(item.startedAtMs)} — {Math.round(item.totalScore)} điểm
            </Text>
            <Text style={styles.itemSub}>{item.deviceModel}</Text>
            {selected && selected.id === item.id ? (
              <View style={styles.detail}>
                <Text style={styles.detailText}>CPU: {Math.round(selected.cpuScore)}</Text>
                <Text style={styles.detailText}>Bộ nhớ: {Math.round(selected.memoryScore)}</Text>
                <Text style={styles.detailText}>Lưu trữ: {Math.round(selected.storageScore)}</Text>
                <Text style={styles.detailText}>Đồ hoạ: {Math.round(selected.gpuScore)}</Text>
                <Text style={styles.detailText}>
                  Stress: {selected.stressDropPct === null ? t('common.unavailable') : `${selected.stressDropPct.toFixed(1)}%`}
                </Text>
              </View>
            ) : null}
          </Pressable>
        ))
      )}
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
  sectionTitle: {
    color: colors.text,
    fontSize: fontSize.lg,
    fontWeight: '700',
    marginVertical: spacing.sm,
  },
  anomalyCard: {
    backgroundColor: '#3A1512',
    borderColor: colors.danger,
    borderRadius: radius.md,
    borderWidth: 1,
    marginBottom: spacing.md,
    padding: spacing.md,
  },
  anomalyText: {
    color: colors.text,
    fontSize: fontSize.md,
    marginTop: spacing.xs,
  },
  buttonRow: {
    flexDirection: 'row',
    gap: spacing.md,
    marginBottom: spacing.md,
  },
  exportButton: {
    alignItems: 'center',
    backgroundColor: colors.card,
    borderColor: colors.cardBorder,
    borderRadius: radius.md,
    borderWidth: 1,
    flex: 1,
    paddingVertical: spacing.md,
  },
  exportText: {
    color: colors.text,
    fontSize: fontSize.md,
    fontWeight: '600',
  },
  item: {
    backgroundColor: colors.card,
    borderColor: colors.cardBorder,
    borderRadius: radius.md,
    borderWidth: 1,
    marginBottom: spacing.sm,
    padding: spacing.md,
  },
  itemTitle: {
    color: colors.text,
    fontSize: fontSize.md,
    fontWeight: '600',
  },
  itemSub: {
    color: colors.textSecondary,
    fontSize: fontSize.sm,
    marginTop: spacing.xs,
  },
  detail: {
    marginTop: spacing.sm,
  },
  detailText: {
    color: colors.text,
    fontSize: fontSize.md,
    paddingVertical: 2,
  },
  muted: {
    color: colors.textMuted,
    fontSize: fontSize.md,
    textAlign: 'center',
  },
});
