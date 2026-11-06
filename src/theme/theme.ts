// Design system: dark developer console. Flat surfaces, 1px borders,
// one accent colour, monospace numerals. No gradients, no blur, no
// decorative illustration, no emoji in UI. If it looks like a
// monitoring console, it is correct; if it looks like a marketing
// landing page, it is wrong.
export const colors = {
  background: '#0A0A0B',
  card: '#131314',
  surface: '#1A1A1C',
  cardBorder: '#232326',
  borderStrong: '#343438',
  text: '#F2F2F7',
  textSecondary: '#A1A1AA',
  textMuted: '#71717A',
  accent: '#FFB800',
  accentText: '#101012',
  danger: '#FF453A',
  warning: '#FF9F0A',
  success: '#30D158',
  info: '#0A84FF',
  chartCpu: '#FFB800',
  chartThermal: '#FF453A',
  chartMemory: '#0A84FF',
  chartBattery: '#30D158',
  unavailable: '#71717A',
} as const;

export type ColorName = keyof typeof colors;

// Monospace for every number that updates: values must not jump
// horizontally when a digit changes. Labels stay on the system font.
export const fonts = {
  mono: 'monospace',
  sans: 'System',
} as const;

export const spacing = {
  xs: 4,
  sm: 8,
  md: 12,
  lg: 16,
  xl: 24,
  xxl: 32,
} as const;

export const fontSize = {
  xs: 11,
  sm: 13,
  md: 15,
  lg: 18,
  xl: 22,
  xxl: 28,
  huge: 36,
} as const;

export const radius = {
  sm: 6,
  md: 10,
  lg: 12,
  xl: 16,
} as const;

export const theme = {
  colors,
  fonts,
  spacing,
  fontSize,
  radius,
  dark: true,
} as const;

export type Theme = typeof theme;
