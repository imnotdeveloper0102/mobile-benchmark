# PROGRESS — Benchmark Performance (2026-11-07)

## Da xong
- 33 file Kotlin + 32 file TypeScript + config (package.json, gradle,
  AndroidManifest, CI). Tong 60+ file truoc vong sua nay, them
  MainActivity/MainApplication/BenchmarkPackage, widget layout rieng,
  SectionHeader.
- Sua lech bridge: runBenchmark(mode), toJson dung NativeBenchmarkResult,
  getBenchmarkHistory dung BenchmarkHistoryItem, getSnapshot/getDeviceInfo
  parse JSON string, subscribeBatch parse {batch}, runStorageSpeedTest
  map seqRead/seqWrite, DeviceInfo tra shape phang dung DeviceInfo cua TS.
- Giao dien lam lai theo phong cach dev: theme mono, MetricCard co thanh
  progress, header LIVE + gio cap nhat, SectionHeader, tab chi co label
  (bo icon glyph de tranh render thanh emoji mau), GaugeScore so mono.

## Kiem chung local (that, 2026-11-07)
- npm install pass: 560 goi (sau khi sua react 19.1.0 -> 19.3.0 vi RN 0.88.1
  doi peer react ^19.3.0; them @react-native/jest-preset@0.88.1 vi RN 0.88
  khong con ship jest preset trong goi chinh).
- npx tsc --noEmit pass (0 loi).
- npx jest --ci pass: 4 suite / 42 test (scoring, aggregate, format, bridge).
  bridge.test phai bo requireActual('react-native') vi keo DevMenu TurboModule.
- Kotlin: braces can bang o 30 file .kt truoc vong sua; chua bien dich Gradle.
- grep TODO trong src/services/bridge.ts va DashboardScreen.tsx: 0.

## Chua kiem chung
- Build APK that: can Android SDK + JDK 17 (may nay khong co Java/SDK).
- Bien dich Kotlin bang Gradle, chay unit test Kotlin.
- Chay may that (OPPO A17k / A3): lay mau, overlay, benchmark, stress.
- Ky release bang keystore that.
