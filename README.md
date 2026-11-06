# Benchmark Performance

App Android theo doi hieu nang theo thoi gian thuc va benchmark.
React Native (TypeScript strict) cho giao dien, Kotlin cho doc so lieu,
overlay va benchmark. Package `com.mdevice.benchmark`.

## Kien truc

```
src/                 React Native: screens, components, store (Zustand),
                     services/bridge, hooks, i18n (vi/en), theme, utils
android/app/src/main/java/com/mdevice/benchmark/
  bridge/            BenchmarkModule (NativeModule) + BenchmarkPackage
  metrics/           CPU, RAM, nhiet, pin, FPS, luu tru, mang, thiet bi
  service/           MetricsService (ForegroundService, lay mau 1 Hz)
  overlay/           OverlayService + OverlayView (SYSTEM_ALERT_WINDOW)
  benchmark/         BenchmarkRunner, ScoreCalculator, GpuBenchmarkView
  storage/           Room (MetricEntity, BenchmarkResultEntity)
  widget/            PulseWidget (RemoteViews, cap nhat toi da 15 phut)
  worker/            AlertWorker, BootReceiver
  shizuku/           ShizukuHelper (module rieng, khong bat buoc)
```

Du lieu realtime day ve JS theo lo moi 750 ms (`onMetricsBatch`),
khong gui tung mau le de tranh nghen bridge. Service lay mau 1 Hz,
tat man hinh giam con 0,2 Hz.

## Cong thuc tinh diem

Moi thanh phan chuan hoa ve 0..1000, roi cong co trong so:

```
total = 0.4 * cpu + 0.2 * memory + 0.2 * storage + 0.2 * gpu
cpuScore     = trung binh co trong so cac bai CPU (prime don 0.25,
               prime da 0.25, SHA-256 0.15, GZIP 0.10, ma tran 0.15,
               sort 0.10), moi bai = 1000 * (refMs / thoi gian that)
memoryScore  = 1000 * (bang thong trung binh doc/ghi / 12000 MB/s),
               tru toi da 20% neu do tre ngau nhien > 300 ns
storageScore = 1000 * (toc do trung binh doc/ghi / 800 MB/s)
gpuScore     = 1000 * (fps / 60) * (1 - jankPct / 200)
```

Cac hang REF_* nam trong `ScoreCalculator.kt`. Chay don nhan / da nhan
chi chay 3 bai cua mode do, cac thanh phan khong chay = 0 nen diem tong
khong so duoc voi lan chay full. Stress test tinh `% giam = (diem dau -
diem cuoi) / diem dau`.

## Gioi han that cua Android (khong giau)

- Android 8+ chan `/proc/stat` nen tai CPU chi la **uoc tinh** tu ti le
  tan so + thoi gian CPU cua tien trinh (`Process.getElapsedCpuTime`).
- FPS chi do duoc **app nay** (Choreographer). FPS app khac can Shizuku
  + `dumpsys gfxinfo` / SurfaceFlinger; khong co Shizuku thi tra
  "Khong kha dung", khong bia.
- Nhiet do CPU/GPU doc tu `/sys/class/thermal/*`, tuy may: khong co
  thermal zone thi "Khong kha dung".
- Thoi gian pin con lai chi uoc tinh khi dang xa pin va doc duoc dong xa;
  ban nay dung moc tham khao 4000 mAh, UI phai ghi "uoc tinh tham khao".
- GPU o mode full hien tra 0 vi `GpuBenchmarkView` chua gan vao bridge.
- Widget cap nhat toi da 15 phut/lan (gioi han WorkManager).
- So nao khong doc duoc thi tra `null` + ly do trong `unavailable[]`,
  UI hien "Khong kha dung". Tuyet doi khong dung du lieu gia/mock.

## Che do Shizuku (tuy chon, khong bat buoc)

Cai app Shizuku, cap quyen cho Benchmark Performance trong Shizuku.
`ShizukuHelper` kiem tra qua reflection, khong bat buoc dependency:
khong co Shizuku thi moi tinh nang nang cao tra "Khong kha dung".

## Quyen

Xin dung luc, co man hinh giai thich: `POST_NOTIFICATIONS`,
`SYSTEM_ALERT_WINDOW` (mo man cai dat), `FOREGROUND_SERVICE_DATA_SYNC`,
`PACKAGE_USAGE_STATS` (neu can), `READ_PHONE_STATE` (cuong do song).
Huong dan tat toi uu pin va khoi dong lai sau BOOT_COMPLETED nam o
man Cai dat (chi khoi dong lai neu nguoi dung da bat).

## Giao dien

Dark mac dinh, phong cach console cua dev: nen phang, vien 1px, mot
mau accent duy nhat, so dung font monospace, label viet hoa nho.
Khong gradient, khong blur kinh, khong emoji trong UI.

## Build release APK

```sh
npm install
npm run typecheck
npm test
cd android
./gradlew assembleRelease
```

APK o `android/app/build/outputs/apk/release/`. Ky release: tao
`android/keystore.properties` (storeFile, storePassword, keyAlias,
keyPassword) — khong commit file nay len git. Khong co keystore thi
ban release ky bang debug key (chi de test, khong phat hanh).
CI o `.github/workflows/build_apk.yml` chay typecheck + Jest +
`assembleRelease` moi lan push len main.

## Test

```sh
npm test          # Jest: scoring, aggregate, format, bridge
```

Unit test Kotlin (`ScoreCalculatorTest`, `SysFsParserTest`) chay bang
`./gradlew testDebugUnitTest` (can Android SDK + JDK 17).

## Trang thai (2026-11-07)

Da xong: 33 file Kotlin + 32 file TS + config Android + CI.
Da kiem chung local: `tsc --noEmit`, Jest (xem PROGRESS.md).
Chua kiem chung: build APK that (can Android SDK), chay may that,
bien dich Kotlin bang Gradle, ky release.
