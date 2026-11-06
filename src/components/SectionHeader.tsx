import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { colors, fontSize, spacing } from '../theme/theme';

// Micro section label, developer-console style: uppercase, tracked,
// muted, with a hairline rule. No icon, no colour block.
export function SectionHeader({ label, right }: { label: string; right?: string }): React.JSX.Element {
  return (
    <View style={styles.row}>
      <Text style={styles.label}>{label.toUpperCase()}</Text>
      <View style={styles.rule} />
      {right ? <Text style={styles.right}>{right}</Text> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    alignItems: 'center',
    flexDirection: 'row',
    gap: spacing.sm,
    marginBottom: spacing.sm,
    marginTop: spacing.lg,
  },
  label: {
    color: colors.textMuted,
    fontSize: fontSize.xs,
    fontWeight: '700',
    letterSpacing: 1.2,
  },
  rule: {
    backgroundColor: colors.cardBorder,
    flex: 1,
    height: 1,
  },
  right: {
    color: colors.textMuted,
    fontFamily: 'monospace',
    fontSize: fontSize.xs,
  },
});
