import React from 'react';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { DashboardScreen } from '../screens/DashboardScreen';
import { BenchmarkScreen } from '../screens/BenchmarkScreen';
import { HistoryScreen } from '../screens/HistoryScreen';
import { DeviceScreen } from '../screens/DeviceScreen';
import { SettingsScreen } from '../screens/SettingsScreen';
import { colors, fontSize } from '../theme/theme';
import { t } from '../i18n';

export type RootTabParamList = {
  Dashboard: undefined;
  Benchmark: undefined;
  History: undefined;
  Device: undefined;
  Settings: undefined;
};

const Tab = createBottomTabNavigator<RootTabParamList>();

// Label-only tabs, developer-console style. No glyph icons: on some
// devices a geometric glyph renders as a colour emoji, which breaks
// the flat dev look this app is supposed to have.
export function AppNavigator(): React.JSX.Element {
  return (
    <Tab.Navigator
      screenOptions={{
        headerStyle: { backgroundColor: colors.background, borderBottomColor: colors.cardBorder, borderBottomWidth: 1 },
        headerTintColor: colors.text,
        headerTitleStyle: { fontWeight: '700', fontSize: fontSize.lg },
        tabBarStyle: {
          backgroundColor: colors.background,
          borderTopColor: colors.cardBorder,
          borderTopWidth: 1,
          height: 60,
          paddingBottom: 8,
          paddingTop: 8,
        },
        tabBarActiveTintColor: colors.accent,
        tabBarInactiveTintColor: colors.textMuted,
        tabBarLabelStyle: { fontSize: 11, fontWeight: '700', letterSpacing: 0.5 },
        tabBarIcon: () => null,
      }}
    >
      <Tab.Screen
        name="Dashboard"
        component={DashboardScreen}
        options={{ title: t('tabs.dashboard'), tabBarLabel: t('tabs.dashboard').toUpperCase() }}
      />
      <Tab.Screen
        name="Benchmark"
        component={BenchmarkScreen}
        options={{ title: t('tabs.benchmark'), tabBarLabel: t('tabs.benchmark').toUpperCase() }}
      />
      <Tab.Screen
        name="History"
        component={HistoryScreen}
        options={{ title: t('tabs.history'), tabBarLabel: t('tabs.history').toUpperCase() }}
      />
      <Tab.Screen
        name="Device"
        component={DeviceScreen}
        options={{ title: t('tabs.device'), tabBarLabel: t('tabs.device').toUpperCase() }}
      />
      <Tab.Screen
        name="Settings"
        component={SettingsScreen}
        options={{ title: t('tabs.settings'), tabBarLabel: t('tabs.settings').toUpperCase() }}
      />
    </Tab.Navigator>
  );
}
