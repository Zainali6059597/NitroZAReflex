# NitroZAReflex

A lightweight, native Android reflex-training and click-speed app. Offline-first, with no WebView or third-party runtime dependencies.

## Current features

- Reflex reaction test with four configurable random-delay ranges (1.5–4.5s, 2–6s, 3–7s, 5–10s)
- False-start detection and configurable 3, 5 or 10-round sessions
- Best, average and median reaction-time summaries
- CPS tests with 1, 5, 10, 30 and 60-second durations
- Six color themes: Neon, Violet, Mint, Ember, Ice and Crimson
- Sound and vibration feedback toggles
- Persistent session history, CPS personal best, theme and test preferences
- Custom NitroZAReflex launcher icon
- Android monotonic-clock timing
- Automated GitHub Actions debug APK builds

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
