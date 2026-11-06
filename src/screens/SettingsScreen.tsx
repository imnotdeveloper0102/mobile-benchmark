import React from 'react';
import { Linking, Pressable, ScrollView, StyleSheet, Switch, Text, View } from 'react-native';
import Slider from '@react-native-community/slider';
import { OverlayControls } from '../components/OverlayControls';
import { useSettingsStore } from '../store/settingsStore';
import { setAlertThresholds } from '../services/bridge';
import { colors, fontSize, radius, spacing } from '../theme/theme';
import { setLanguage, t } from '../i18n';
import type { Language } from '../i18n';

export function SettingsScreen(): React.JSX.Element {
  const thresholds = useSettingsStore((state) => state.thresholds);
  const setThresholds = useSettingsStore((state) => state.setThresholds);
  const samplingHz = useSettingsStore((state) => state.samplingHz);
  const setSamplingHz = useSettingsStore((state) => state.setSamplingHz);
  const darkMode = useSettingsStore((state) => state.darkMode);
  const setDarkMode = useSettingsStore((state) => state.setDarkMode);
  const language = useSettingsStore((state) => state.language);
  const setLanguageStore = useSettingsStore((state) => state.setLanguage);

  const changeLanguage = (next: Language): void => {
    setLanguage(next);
    setLanguageStore(next);
  };

  const applyThresholds = (): void => {
    setAlertThresholds(thresholds).catch(() => undefined);
  };

  const openAppSettings = (): void => {
    Linking.openSettings().catch(() => undefined);
  };

  const openBatterySettings = (): void => {
    Linking.sendIntent('android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS').catch(() =>
      Linking.openSettings().catch(() => undefined),
    );
  };

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <Text style={styles.title}>{t('settings.title')}</Text>

      <Text style={styles.sectionTitle}>{t('settings.permissions')}</Text>
      <Text style={styles.desc}>{t('settings.permissionsDesc')}</Text>
      <View style={styles.card}>
        <View style={styles.row}>
          <Text style={styles.rowLabel}>{t('settings.notificationPermission')}</Text>
          <Pressable onPress={openAppSettings} style={styles.smallButton}>
            <Text style={styles.smallButtonText}>{t('settings.openSettings')}</Text>
          </Pressable>
        </View>
        <Text style={styles.desc}>{t('settings.overlayPermissionDesc')}</Text>
        <View style={styles.row}>
          <Text style={styles.rowLabel}>{t('settings.overlayPermission')}</Text>
          <Pressable onPress={openAppSettings} style={styles.smallButton}>
            <Text style={styles.smallButtonText}>{t('settings.openSettings')}</Text>
          </Pressable>
        </View>
        <View style={styles.row}>
          <Text style={styles.rowLabel}>{t('settings.usageStatsPermission')}</Text>
          <Pressable
            onPress={() =>
              Linking.sendIntent('android.settings.USAGE_ACCESS_SETTINGS').catch(() =>
                Linking.openSettings().catch(() => undefined),
              )
            }
            style={styles.smallButton}
          >
            <Text style={styles.smallButtonText}>{t('settings.openSettings')}</Text>
          </Pressable>
        </View>
      </View>

      <Text style={styles.sectionTitle}>{t('settings.alerts')}</Text>
      <View style={styles.card}>
        <Text style={styles.rowLabel}>
          {t('settings.tempThreshold')}: {thresholds.tempC}°C
        </Text>
        <Slider
          value={thresholds.tempC}
          minimumValue={35}
          maximumValue={60}
          step={1}
          onValueChange={(value) => setThresholds({ tempC: Math.round(value) })}
          onSlidingComplete={applyThresholds}
          minimumTrackTintColor={colors.accent}
          maximumTrackTintColor={colors.cardBorder}
        />
        <Text style={styles.rowLabel}>
          {t('settings.ramThreshold')}: {thresholds.ramAvailPct}%
        </Text>
        <Slider
          value={thresholds.ramAvailPct}
          minimumValue={5}
          maximumValue={40}
          step={1}
          onValueChange={(value) => setThresholds({ ramAvailPct: Math.round(value) })}
          onSlidingComplete={applyThresholds}
          minimumTrackTintColor={colors.accent}
          maximumTrackTintColor={colors.cardBorder}
        />
        <Text style={styles.rowLabel}>
          {t('settings.batteryThreshold')}: {thresholds.batteryPct}%
        </Text>
        <Slider
          value={thresholds.batteryPct}
          minimumValue={5}
          maximumValue={50}
          step={1}
          onValueChange={(value) => setThresholds({ batteryPct: Math.round(value) })}
          onSlidingComplete={applyThresholds}
          minimumTrackTintColor={colors.accent}
          maximumTrackTintColor={colors.cardBorder}
        />
      </View>

      <Text style={styles.sectionTitle}>{t('settings.sampling')}</Text>
      <View style={styles.card}>
        <Text style={styles.rowLabel}>
          {t('settings.samplingHz')}: {samplingHz.toFixed(1)}
        </Text>
        <Slider
          value={samplingHz}
          minimumValue={0.5}
          maximumValue={2}
          step={0.5}
          onValueChange={(value) => setSamplingHz(value)}
          minimumTrackTintColor={colors.accent}
          maximumTrackTintColor={colors.cardBorder}
        />
      </View>

      <Text style={styles.sectionTitle}>{t('settings.batteryOptimization')}</Text>
      <View style={styles.card}>
        <Text style={styles.desc}>{t('settings.batteryOptimizationDesc')}</Text>
        <Pressable onPress={openBatterySettings} style={styles.smallButton}>
          <Text style={styles.smallButtonText}>{t('settings.openBatterySettings')}</Text>
        </Pressable>
      </View>

      <Text style={styles.sectionTitle}>{t('settings.overlay')}</Text>
      <OverlayControls />

      <Text style={styles.sectionTitle}>{t('settings.language')}</Text>
      <View style={styles.card}>
        <View style={styles.row}>
          <Pressable
            onPress={() => changeLanguage('vi')}
            style={[styles.langButton, language === 'vi' ? styles.langActive : undefined]}
          >
            <Text style={styles.rowLabel}>{t('settings.vietnamese')}</Text>
          </Pressable>
          <Pressable
            onPress={() => changeLanguage('en')}
            style={[styles.langButton, language === 'en' ? styles.langActive : undefined]}
          >
            <Text style={styles.rowLabel}>{t('settings.english')}</Text>
          </Pressable>
        </View>
        <View style={styles.row}>
          <Text style={styles.rowLabel}>{t('settings.darkMode')}</Text>
          <Switch value={darkMode} onValueChange={setDarkMode} trackColor={{ true: colors.accent }} />
        </View>
        <View style={styles.row}>
          <Text style={styles.rowLabel}>{t('settings.version')}</Text>
          <Text style={styles.rowLabel}>{t('settings.versionValue')}</Text>
        </View>
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
  desc: {
    color: colors.textSecondary,
    fontSize: fontSize.md,
    marginBottom: spacing.sm,
  },
  card: {
    backgroundColor: colors.card,
    borderColor: colors.cardBorder,
    borderRadius: radius.md,
    borderWidth: 1,
    padding: spacing.md,
  },
  row: {
    alignItems: 'center',
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: spacing.sm,
  },
  rowLabel: {
    color: colors.text,
    fontSize: fontSize.md,
  },
  smallButton: {
    backgroundColor: colors.accent,
    borderRadius: radius.md,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
  },
  smallButtonText: {
    color: colors.accentText,
    fontSize: fontSize.sm,
    fontWeight: '700',
  },
  langButton: {
    borderColor: colors.cardBorder,
    borderRadius: radius.md,
    borderWidth: 1,
    flex: 1,
    padding: spacing.md,
  },
  langActive: {
    borderColor: colors.accent,
  },
});
