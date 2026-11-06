import React, { useMemo } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import Svg, { Line, Path, Text as SvgText } from 'react-native-svg';
import { colors, fonts, fontSize, radius, spacing } from '../theme/theme';

interface RealtimeChartProps {
  title: string;
  data: number[];
  color: string;
  unit: string;
  min?: number;
  max?: number;
  windowLabel: string;
  height?: number;
}

const CHART_WIDTH = 340;
const CHART_HEIGHT = 140;
const PADDING_LEFT = 36;
const PADDING_RIGHT = 8;
const PADDING_TOP = 10;
const PADDING_BOTTOM = 20;

function buildPath(values: number[], min: number, max: number): string {
  if (values.length === 0) {
    return '';
  }
  const innerWidth = CHART_WIDTH - PADDING_LEFT - PADDING_RIGHT;
  const innerHeight = CHART_HEIGHT - PADDING_TOP - PADDING_BOTTOM;
  const range = max - min || 1;
  const points = values.map((value, index) => {
    const x =
      PADDING_LEFT +
      (values.length === 1 ? innerWidth / 2 : (index / (values.length - 1)) * innerWidth);
    const clamped = Math.min(max, Math.max(min, value));
    const y = PADDING_TOP + (1 - (clamped - min) / range) * innerHeight;
    return { x, y };
  });
  return points
    .map((point, index) => `${index === 0 ? 'M' : 'L'}${point.x.toFixed(1)},${point.y.toFixed(1)}`)
    .join(' ');
}

export function RealtimeChart({
  title,
  data,
  color,
  unit,
  min,
  max,
  windowLabel,
  height = CHART_HEIGHT,
}: RealtimeChartProps): React.JSX.Element {
  const resolvedMin = useMemo(() => {
    if (min !== undefined) {
      return min;
    }
    if (data.length === 0) {
      return 0;
    }
    return Math.min(...data);
  }, [data, min]);

  const resolvedMax = useMemo(() => {
    if (max !== undefined) {
      return max;
    }
    if (data.length === 0) {
      return 1;
    }
    const peak = Math.max(...data);
    return peak <= resolvedMin ? resolvedMin + 1 : peak;
  }, [data, max, resolvedMin]);

  const path = useMemo(
    () => buildPath(data, resolvedMin, resolvedMax),
    [data, resolvedMin, resolvedMax],
  );
  const current = data.length > 0 ? data[data.length - 1] : null;
  const midY = PADDING_TOP + (CHART_HEIGHT - PADDING_TOP - PADDING_BOTTOM) / 2;

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>{title.toUpperCase()}</Text>
        <Text style={styles.current}>
          {current === null ? '—' : `${current.toFixed(1)} ${unit}`}
        </Text>
      </View>
      <Svg width="100%" height={height} viewBox={`0 0 ${CHART_WIDTH} ${CHART_HEIGHT}`}>
        <Line
          x1={PADDING_LEFT}
          y1={PADDING_TOP}
          x2={PADDING_LEFT}
          y2={CHART_HEIGHT - PADDING_BOTTOM}
          stroke={colors.cardBorder}
          strokeWidth={1}
        />
        <Line
          x1={PADDING_LEFT}
          y1={CHART_HEIGHT - PADDING_BOTTOM}
          x2={CHART_WIDTH - PADDING_RIGHT}
          y2={CHART_HEIGHT - PADDING_BOTTOM}
          stroke={colors.cardBorder}
          strokeWidth={1}
        />
        <Line
          x1={PADDING_LEFT}
          y1={midY}
          x2={CHART_WIDTH - PADDING_RIGHT}
          y2={midY}
          stroke={colors.cardBorder}
          strokeWidth={0.5}
          strokeDasharray="4,4"
        />
        <SvgText x={2} y={PADDING_TOP + 4} fill={colors.textMuted} fontSize={10}>
          {resolvedMax.toFixed(0)}
        </SvgText>
        <SvgText x={2} y={CHART_HEIGHT - PADDING_BOTTOM} fill={colors.textMuted} fontSize={10}>
          {resolvedMin.toFixed(0)}
        </SvgText>
        <SvgText
          x={CHART_WIDTH - PADDING_RIGHT}
          y={CHART_HEIGHT - 4}
          fill={colors.textMuted}
          fontSize={10}
          textAnchor="end"
        >
          {windowLabel}
        </SvgText>
        {path !== '' ? (
          <Path d={path} fill="none" stroke={color} strokeWidth={2} strokeLinejoin="round" />
        ) : null}
      </Svg>
      {data.length === 0 ? <Text style={styles.empty}>Chưa có dữ liệu</Text> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    backgroundColor: colors.card,
    borderColor: colors.cardBorder,
    borderRadius: radius.md,
    borderWidth: 1,
    marginBottom: spacing.md,
    padding: spacing.md,
  },
  header: {
    alignItems: 'baseline',
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: spacing.sm,
  },
  title: {
    color: colors.textSecondary,
    fontSize: fontSize.xs,
    fontWeight: '700',
    letterSpacing: 0.8,
  },
  current: {
    color: colors.text,
    fontFamily: fonts.mono,
    fontSize: fontSize.md,
    fontWeight: '700',
  },
  empty: {
    color: colors.textMuted,
    fontSize: fontSize.sm,
    textAlign: 'center',
  },
});
