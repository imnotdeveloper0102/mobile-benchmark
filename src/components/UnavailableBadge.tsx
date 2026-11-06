import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { colors, fontSize, radius, spacing } from '../theme/theme';
import { t } from '../i18n';

interface UnavailableBadgeProps {
  reason: string;
}

export function UnavailableBadge({ reason }: UnavailableBadgeProps): React.JSX.Element {
  return (
    <View style={styles.container}>
      <Text style={styles.text}>{t('common.unavailableReason', { reason })}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    backgroundColor: '#262628',
    borderRadius: radius.sm,
    marginTop: spacing.xs,
    paddingHorizontal: spacing.sm,
    paddingVertical: spacing.xs,
  },
  text: {
    color: colors.textSecondary,
    fontSize: fontSize.xs,
  },
});
