import React, { useEffect } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import Animated, { useAnimatedProps, useSharedValue, withTiming } from 'react-native-reanimated';
import Svg, { Circle } from 'react-native-svg';
import { colors, fonts, fontSize } from '../theme/theme';
import { scoreLabel } from '../utils/scoring';

const AnimatedCircle = Animated.createAnimatedComponent(Circle);

interface GaugeScoreProps {
  score: number;
  size?: number;
  label?: string;
}

export function GaugeScore({ score, size = 200, label }: GaugeScoreProps): React.JSX.Element {
  const clamped = Math.min(1000, Math.max(0, score));
  const progress = useSharedValue(0);

  useEffect(() => {
    progress.value = withTiming(clamped / 1000, { duration: 900 });
  }, [clamped, progress]);

  const radius = size / 2 - 14;
  const circumference = 2 * Math.PI * radius;
  const animatedProps = useAnimatedProps(() => ({
    strokeDashoffset: circumference * (1 - progress.value),
  }));

  return (
    <View style={[styles.container, { width: size, height: size }]}>
      <Svg width={size} height={size}>
        <Circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          stroke={colors.cardBorder}
          strokeWidth={12}
          fill="none"
        />
        <AnimatedCircle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          stroke={colors.accent}
          strokeWidth={12}
          fill="none"
          strokeLinecap="round"
          strokeDasharray={circumference}
          animatedProps={animatedProps}
          rotation={-90}
          origin={`${size / 2}, ${size / 2}`}
        />
      </Svg>
      <View style={styles.center}>
        <Text style={styles.score}>{Math.round(clamped)}</Text>
        <Text style={styles.max}>/ 1000</Text>
        <Text style={styles.label}>{label ?? scoreLabel(clamped)}</Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    alignSelf: 'center',
    position: 'relative',
  },
  center: {
    alignItems: 'center',
    bottom: 0,
    justifyContent: 'center',
    left: 0,
    position: 'absolute',
    right: 0,
    top: 0,
  },
  score: {
    color: colors.text,
    fontFamily: fonts.mono,
    fontSize: 40,
    fontWeight: '800',
  },
  max: {
    color: colors.textSecondary,
    fontFamily: fonts.mono,
    fontSize: fontSize.sm,
  },
  label: {
    color: colors.accent,
    fontSize: fontSize.md,
    fontWeight: '600',
    marginTop: 4,
  },
});
