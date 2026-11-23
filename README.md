# NitroZAReflex

A lightweight, native Android app for reaction-time and clicks-per-second (CPS) testing.

## Current features

- Reaction-time test with randomized 1.5–4.5 second wait
- False-start detection
- Five-round best, average and median reaction results
- CPS tests with 1, 5, 10, 30 and 60 second durations
- Monotonic-clock timing and an offline-first design
- Dark Nitro-inspired interface
- GitHub Actions APK build workflow

## Build locally

Requirements: JDK 17, Android SDK (platform 35), Gradle 8.9.

```bash
gradle assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## Build with GitHub Actions

Every push or pull request to `main` triggers **Build NitroZAReflex APK**. You can also start it manually from the Actions tab using **Run workflow**.

When the run succeeds, open it and download the `NitroZAReflex-debug` artifact.

## Measurement note

Intervals are measured with Android's monotonic clock. Display refresh, touch sampling, operating-system scheduling and device hardware affect measured results. Use scores for consistent personal comparisons rather than laboratory-grade measurement.
