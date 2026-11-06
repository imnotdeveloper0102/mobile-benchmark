import React from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useDeviceInfo } from '../hooks/useDeviceInfo';
import { colors, fontSize, radius, spacing } from '../theme/theme';
import { t } from '../i18n';
import { formatBytes } from '../services/format';

function Row({ label, value }: { label: string; value: string }): React.JSX.Element {
  return (
    <View style={styles.row}>
      <Text style={styles.label}>{label}</Text>
      <Text style={styles.value}>{value}</Text>
    </View>
  );
}

export function DeviceScreen(): React.JSX.Element {
  const { deviceInfo, loading, error, reload } = useDeviceInfo();

  if (loading) {
    return (
      <View style={styles.center}>
        <Text style={styles.muted}>{t('common.loading')}</Text>
      </View>
    );
  }

  if (error || !deviceInfo) {
    return (
      <View style={styles.center}>
        <Text style={styles.errorText}>{error ?? t('common.errorLoading')}</Text>
        <Pressable onPress={reload} style={styles.retryButton}>
          <Text style={styles.retryText}>{t('common.retry')}</Text>
        </Pressable>
      </View>
    );
  }

  const na = t('common.unavailable');
  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <Text style={styles.title}>{t('device.title')}</Text>
      <View style={styles.card}>
        <Row label={t('device.model')} value={deviceInfo.model} />
        <Row label={t('device.manufacturer')} value={deviceInfo.manufacturer} />
        <Row label={t('device.brand')} value={deviceInfo.brand} />
        <Row label={t('device.soc')} value={deviceInfo.socModel ?? na} />
        <Row label={t('device.gpu')} value={deviceInfo.gpuRenderer ?? na} />
        <Row label={t('device.android')} value={`${deviceInfo.androidVersion} (API ${deviceInfo.apiLevel})`} />
        <Row label={t('device.securityPatch')} value={deviceInfo.securityPatch} />
        <Row label={t('device.buildId')} value={deviceInfo.buildId} />
        <Row label={t('device.board')} value={deviceInfo.board} />
        <Row label={t('device.hardware')} value={deviceInfo.hardware} />
        <Row label={t('device.ram')} value={formatBytes(deviceInfo.totalRamBytes)} />
        <Row label={t('device.cpuAbi')} value={deviceInfo.cpuAbi.join(', ') || na} />
        <Row label={t('device.kernel')} value={deviceInfo.kernelVersion ?? na} />
        <Row
          label={t('device.resolution')}
          value={`${deviceInfo.screenWidthPx} × ${deviceInfo.screenHeightPx} px`}
        />
        <Row
          label={t('device.density')}
          value={`${deviceInfo.screenDensityDpi} dpi (×${deviceInfo.screenDensity.toFixed(2)})`}
        />
        <Row label={t('device.hdr')} value={deviceInfo.hdrCapable ? t('common.yes') : t('common.no')} />
      </View>
      <Text style={styles.sectionTitle}>{t('device.sensors')}</Text>
      <View style={styles.card}>
        {deviceInfo.sensors.length === 0 ? (
          <Text style={styles.muted}>{t('device.noSensors')}</Text>
        ) : (
          deviceInfo.sensors.map((sensor) => (
            <Text key={sensor} style={styles.sensorText}>
              • {sensor}
            </Text>
          ))
        )}
      </View>
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
  center: {
    alignItems: 'center',
    backgroundColor: colors.background,
    flex: 1,
    justifyContent: 'center',
    padding: spacing.lg,
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
    marginVertical: spacing.md,
  },
  card: {
    backgroundColor: colors.card,
    borderColor: colors.cardBorder,
    borderRadius: radius.md,
    borderWidth: 1,
    padding: spacing.md,
  },
  row: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: spacing.sm,
  },
  label: {
    color: colors.textSecondary,
    flex: 1,
    fontSize: fontSize.md,
  },
  value: {
    color: colors.text,
    flex: 1,
    fontSize: fontSize.md,
    fontWeight: '600',
    textAlign: 'right',
  },
  sensorText: {
    color: colors.text,
    fontSize: fontSize.md,
    paddingVertical: spacing.xs,
  },
  muted: {
    color: colors.textMuted,
    fontSize: fontSize.md,
  },
  errorText: {
    color: colors.danger,
    fontSize: fontSize.md,
    textAlign: 'center',
  },
  retryButton: {
    backgroundColor: colors.accent,
    borderRadius: radius.md,
    marginTop: spacing.md,
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.sm,
  },
  retryText: {
    color: colors.accentText,
    fontWeight: '700',
  },
});
