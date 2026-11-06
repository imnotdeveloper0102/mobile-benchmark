import React, { useState } from 'react';
import { Alert, Pressable, StyleSheet, Text, View } from 'react-native';
import Slider from '@react-native-community/slider';
import { colors, fontSize, radius, spacing } from '../theme/theme';
import { t } from '../i18n';
import { requestOverlayPermission, startOverlay, stopOverlay } from '../services/bridge';
import { useSettingsStore, OVERLAY_METRIC_KEYS, type OverlayMetricKey } from '../store/settingsStore';

const METRIC_LABELS: Record<OverlayMetricKey, string> = {
  cpu: 'CPU',
  ram: 'RAM',
  temp: 'Nhiệt',
  fps: 'FPS',
  battery: 'Pin',
};

export function OverlayControls(): React.JSX.Element {
  const overlay = useSettingsStore((state) => state.overlay);
  const setOverlay = useSettingsStore((state) => state.setOverlay);
  const [running, setRunning] = useState(false);
  const [busy, setBusy] = useState(false);

  const toggleMetric = (key: OverlayMetricKey): void => {
    const enabled = overlay.enabledMetrics.includes(key);
    const next = enabled
      ? overlay.enabledMetrics.filter((item) => item !== key)
      : [...overlay.enabledMetrics, key];
    setOverlay({ enabledMetrics: next });
  };

  const handleToggleOverlay = async (): Promise<void> => {
    setBusy(true);
    try {
      if (running) {
        await stopOverlay();
        setRunning(false);
        return;
      }
      const granted = await requestOverlayPermission();
      if (!granted) {
        Alert.alert(
          t('settings.overlayPermission'),
          t('settings.overlayPermissionDesc'),
        );
        return;
      }
      await startOverlay(overlay);
      setRunning(true);
    } catch (err) {
      Alert.alert(
        t('common.error'),
        err instanceof Error ? err.message : t('common.errorLoading'),
      );
    } finally {
      setBusy(false);
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.sectionTitle}>{t('settings.overlayMetrics')}</Text>
      <View style={styles.chipRow}>
        {OVERLAY_METRIC_KEYS.map((key) => {
          const active = overlay.enabledMetrics.includes(key);
          return (
            <Pressable
              key={key}
              onPress={() => toggleMetric(key)}
              style={[styles.chip, active ? styles.chipActive : undefined]}
            >
              <Text style={[styles.chipText, active ? styles.chipTextActive : undefined]}>
                {METRIC_LABELS[key]}
              </Text>
            </Pressable>
          );
        })}
      </View>

      <Text style={styles.sectionTitle}>
        {t('settings.overlayOpacity')}: {Math.round(overlay.opacity * 100)}%
      </Text>
      <Slider
        value={overlay.opacity}
        minimumValue={0.3}
        maximumValue={1}
        step={0.05}
        onValueChange={(value) => setOverlay({ opacity: value })}
        minimumTrackTintColor={colors.accent}
        maximumTrackTintColor={colors.cardBorder}
      />

      <Text style={styles.sectionTitle}>{t('settings.overlaySize')}</Text>
      <View style={styles.chipRow}>
        {(['small', 'medium', 'large'] as const).map((size) => {
          const active = overlay.size === size;
          const label =
            size === 'small'
              ? t('settings.overlaySizeSmall')
              : size === 'medium'
                ? t('settings.overlaySizeMedium')
                : t('settings.overlaySizeLarge');
          return (
            <Pressable
              key={size}
              onPress={() => setOverlay({ size })}
              style={[styles.chip, active ? styles.chipActive : undefined]}
            >
              <Text style={[styles.chipText, active ? styles.chipTextActive : undefined]}>
                {label}
              </Text>
            </Pressable>
          );
        })}
      </View>

      <Pressable
        onPress={handleToggleOverlay}
        disabled={busy}
        style={[styles.button, running ? styles.buttonStop : styles.buttonStart]}
      >
        <Text style={styles.buttonText}>
          {running ? t('settings.overlayStop') : t('settings.overlayStart')}
        </Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    backgroundColor: colors.card,
    borderColor: colors.cardBorder,
    borderRadius: radius.md,
    borderWidth: 1,
    padding: spacing.md,
  },
  sectionTitle: {
    color: colors.text,
    fontSize: fontSize.md,
    fontWeight: '600',
    marginBottom: spacing.sm,
    marginTop: spacing.sm,
  },
  chipRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.sm,
  },
  chip: {
    backgroundColor: colors.background,
    borderColor: colors.cardBorder,
    borderRadius: radius.lg,
    borderWidth: 1,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
  },
  chipActive: {
    backgroundColor: colors.accent,
    borderColor: colors.accent,
  },
  chipText: {
    color: colors.text,
    fontSize: fontSize.md,
  },
  chipTextActive: {
    color: colors.accentText,
    fontWeight: '700',
  },
  button: {
    alignItems: 'center',
    borderRadius: radius.md,
    marginTop: spacing.lg,
    paddingVertical: spacing.md,
  },
  buttonStart: {
    backgroundColor: colors.accent,
  },
  buttonStop: {
    backgroundColor: colors.danger,
  },
  buttonText: {
    color: '#101012',
    fontSize: fontSize.md,
    fontWeight: '700',
  },
});
