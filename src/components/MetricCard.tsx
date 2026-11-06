import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { colors, fonts, fontSize, radius, spacing } from '../theme/theme';
import { UnavailableBadge } from './UnavailableBadge';

interface MetricCardProps {
  label: string;
  value: string;
  unit?: string;
  subNote?: string;
  unavailableReason?: string | null;
  accent?: boolean;
  /** 0..100, draws the thin usage bar under the value. Omit when the metric has no percentage. */
  progress?: number | null;
  progressColor?: string;
}

export function MetricCard({
  label,
  value,
  unit,
  subNote,
  unavailableReason,
  accent,
  progress,
  progressColor,
}: MetricCardProps): React.JSX.Element {
  const pct = progress === null || progress === undefined ? null : Math.min(100, Math.max(0, progress));
  return (
    <View style={[styles.card, accent ? styles.accentCard : undefined]}>
      <Text style={styles.label}>{label.toUpperCase()}</Text>
      {unavailableReason ? (
        <View>
          <Text style={styles.valueUnavailable}>Không khả dụng</Text>
          <UnavailableBadge reason={unavailableReason} />
        </View>
      ) : (
        <View style={styles.valueRow}>
          <Text style={styles.value}>{value}</Text>
          {unit ? <Text style={styles.unit}>{unit}</Text> : null}
        </View>
      )}
      {pct !== null ? (
        <View style={styles.track}>
          <View style={[styles.fill, { width: `${pct}%`, backgroundColor: progressColor ?? colors.accent }]} />
        </View>
      ) : null}
      {subNote ? <Text style={styles.subNote}>{subNote}</Text> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: colors.card,
    borderColor: colors.cardBorder,
    borderRadius: radius.md,
    borderWidth: 1,
    flex: 1,
    minWidth: 150,
    padding: spacing.md,
  },
  accentCard: {
    borderColor: colors.accent,
  },
  label: {
    color: colors.textSecondary,
    fontSize: fontSize.xs,
    fontWeight: '700',
    letterSpacing: 0.8,
    marginBottom: spacing.xs,
  },
  valueRow: {
    alignItems: 'baseline',
    flexDirection: 'row',
    flexWrap: 'wrap',
  },
  value: {
    color: colors.text,
    fontFamily: fonts.mono,
    fontSize: 26,
    fontWeight: '700',
  },
  valueUnavailable: {
    color: colors.textMuted,
    fontSize: fontSize.lg,
    fontWeight: '600',
  },
  unit: {
    color: colors.textSecondary,
    fontFamily: fonts.mono,
    fontSize: fontSize.sm,
    marginLeft: spacing.xs,
  },
  track: {
    backgroundColor: colors.surface,
    borderRadius: 2,
    height: 3,
    marginTop: spacing.sm,
    overflow: 'hidden',
  },
  fill: {
    height: 3,
  },
  subNote: {
    color: colors.textMuted,
    fontFamily: fonts.mono,
    fontSize: fontSize.xs,
    marginTop: spacing.sm,
  },
});
