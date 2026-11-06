module.exports = {
  preset: '@react-native/jest-preset',
  testMatch: ['**/src/__tests__/**/*.test.ts?(x)'],
  transformIgnorePatterns: [
    'node_modules/(?!(react-native|@react-native|@react-navigation|react-native-reanimated|react-native-svg|react-native-gesture-handler|react-native-safe-area-context|react-native-screens|zustand|@react-native-async-storage)/)',
  ],
};
