# SensePhone

A completely offline, sensor-first Android app that turns the sensors already in
your phone into a live dashboard, real-time oscilloscope, hands-on experiments,
and derived movement/rotation/stability metrics.

**100% on-device.** No network, no backend, no accounts, no ads, no external
APIs, no AI. All computation happens locally and privately on the device.

---

## Table of Contents

1. [Overview](#1-overview)
2. [Why SensePhone](#2-why-sensephone)
3. [Features](#3-features)
4. [Tech Stack](#4-tech-stack)
5. [Architecture](#5-architecture)
6. [Project Structure](#6-project-structure)
7. [Sensor Model & Sampling](#7-sensor-model--sampling)
8. [Live Sensor Explorer (V1 & V2)](#8-live-sensor-explorer-v1--v2)
9. [Experiments (V3)](#9-experiments-v3)
10. [Derived Metrics (V4)](#10-derived-metrics-v4)
11. [Design System](#11-design-system)
12. [Privacy, Permissions & Battery](#12-privacy-permissions--battery)
13. [Build Environment](#13-build-environment)
14. [Build & Run](#14-build--run)
15. [Testing](#15-testing)
16. [Known Limitations](#16-known-limitations)
17. [Development History](#17-development-history)
18. [Dependencies](#18-dependencies)
19. [References, Acknowledgments & License](#19-references-acknowledgments--license)
20. [How to Test on Your Phone](#20-how-to-test-on-your-phone)

---

## 1. Overview

SensePhone is organized in four progressive feature "versions" that build on the
`android.hardware.SensorManager` stack:

| Version | Feature | What you get |
|---------|---------|--------------|
| **V1**  | Sensor Explorer | A dashboard of every sensor on the device (accelerometer, gyroscope, magnetometer, light, proximity, pressure, gravity, linear acceleration, rotation vector, humidity) with live values and availability. |
| **V2**  | Sensor Detail + Real-time Graph | A detail screen per sensor with a bounded, in-memory live graph rendered with a custom Compose `Canvas` — no charting library. |
| **V3**  | Experiments | Bubble Level, Compass, Magnetic Field probe, and Light Meter — practical, visual uses of the raw sensors. |
| **V4**  | Derived Metrics | Movement Intensity, Rotation Intensity, Stability, and Device Orientation computed from the accelerometer and gyroscope with a pure, deterministic, on-device engine. |

All sensor sampling is **only active while the corresponding screen is visible**
— there is no background or 24/7 monitoring.

---

## 2. Why SensePhone

The reference project — [AndroidSensors](https://github.com/AkshayAshokCode/AndroidSensors)
by AkshayAshokCode — demonstrated the same ideas using a hybrid XML
(Fragments) + Jetpack Compose stack that also pulled in Firebase, analytics, an
online-onboarding flow, and network dependencies.

SensePhone is a clean-room rewrite that keeps the intent (learn your phone's
sensors) while:

- Going **100% offline** — zero permissions, zero network calls.
- Rebuilding the UI entirely in **Jetpack Compose** with a dark, futuristic
  Material 3 design system.
- Introducing a **clean, layered architecture** (UI → ViewModel → Use-Case-style
  metric engine → Repository → `SensorManager`).
- Adding **derived metrics** (V4) with fully transparent, documented formulas —
  no black boxes.

The companion reference project's `BubbleLevel` experiment (rotation-matrix
fusion) is ported and credited below.

---

## 3. Features

- **Sensor dashboard (V1)** — tile-based list of all ten supported sensor kinds;
  each tile shows live primary values, units and LIVE/UNAVAILABLE status.
- **Real-time sensor graphs (V2)** — per-axis scrolling plots for vector
  sensors, scalar plots for single-axis sensors, with a stat strip
  (latest/min/max) and a bounded `240`-sample in-memory window sampled at ~10 Hz.
- **Experiments (V3)**:
  - **Bubble Level** — precision leveling with three tolerance modes
    (Precision 0.5°, Standard 2°, Rough 5°).
  - **Compass** — dial compass with smoothing and 8-way headings.
  - **Magnetic Field** — live X/Y/Z, total magnitude, peak tracking and a
    normalized intensity meter for probing magnets.
  - **Light Meter** — ambient-light levels with a logarithmic intensity bar.
- **Derived metrics (V4)** — Movement Intensity, Rotation Intensity, Stability
  and Device Orientation, plus pitch/roll and a "level" indicator.
- **Offline-first** — no INTERNET permission; all analysis runs on-device.
- **Lifecycle-aware sampling** — collectors are started and cancelled around
  screen visibility; nothing runs in the background.
- **Dark futuristic UI** — custom color system, glass cards, dot grids,
  monospace readouts, animated live indicators, no emoji-as-icon shortcuts
  (real Material icon set).

---

## 4. Tech Stack

| Layer | Choice |
|-------|--------|
| Language | **Kotlin** 2.0.21 |
| UI | **Jetpack Compose** (BOM 2024.12.01) with **Material 3** |
| Async | **Kotlin Coroutines + Flow** (`callbackFlow`, `combine`, `flowOf`) |
| Navigation | **Navigation Compose** 2.8.5 |
| State | `StateFlow` + `collectAsStateWithLifecycle` |
| Charting | Custom `Canvas`/`DrawScope` (`LiveChart.kt`) — **no third-party chart lib** |
| DI | none — manual constructor wiring (kept deliberately simple) |
| Build | AGP 8.7.3, Gradle 8.13, JDK 17 |
| Min/Target SDK | `minSdk 26` (Android 8.0), `targetSdk`/`compileSdk 35` |

---

## 5. Architecture

Clean, layered, "ring" architecture. Dependencies point **inward**: the UI never
talks to `SensorManager` directly, and the metric engine has no Android imports.

```
┌────────────────────────────────────────────────────────────────────┐
│  Presentation  (com.sensephone.app.presentation)                    │
│  Screens (Compose) · ViewModels · NavHost · Components             │
│  ─── collects StateFlow; calls onScreenActive()/onScreenInactive() │
└───────────────────────────────┬────────────────────────────────────┘
                                │
                                ▼
┌────────────────────────────────────────────────────────────────────┐
│  Domain  (com.sensephone.app.domain.metrics)                        │
│  Pure-kotlin metric engine: calculators + low-pass filters,         │
│  classifies levels, produces MetricsSnapshot. NO android.* imports. │
└───────────────────────────────┬────────────────────────────────────┘
                                │
                                ▼
┌────────────────────────────────────────────────────────────────────┐
│  Data  (com.sensephone.app.data.sensor)                              │
│  SensorRepository (interface)                                       │
│    └─ AndroidSensorRepository : wraps SensorManager via callbackFlow│
└───────────────────────────────┬────────────────────────────────────┘
                                │
                                ▼
┌────────────────────────────────────────────────────────────────────┐
│  Framework  (android.hardware.SensorManager · Sensors · NDK-none)   │
└────────────────────────────────────────────────────────────────────┘
```

ViewModels extend a base `SensorBoundViewModel` that owns the repository and
tracks active collection `Job`s. The UI's `SensorLifecycle` composable calls
`onScreenActive()` when a screen enters composition and `onScreenInactive()`
when it leaves, so **sensor listeners only exist while the screen is visible**.

---

## 6. Project Structure

```
.
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml            (no permissions!)
│       ├── res/                           themes, colors, strings, launcher icon
│       └── java/com/sensephone/app/
│           ├── MainActivity.kt
│           ├── data/sensor/
│           │   ├── SensorKind.kt          (enum: 10 sensors, androidType mapping)
│           │   ├── SensorSample.kt        (timestampNanos, x/y/z, scalar, magnitude)
│           │   ├── SensorRepository.kt    (interface + SamplingRate)
│           │   └── AndroidSensorRepository.kt
│           ├── domain/metrics/
│           │   ├── MetricsTypes.kt        (levels, MetricsSnapshot, orientation)
│           │   ├── Filters.kt             (LowPassFilter)
│           │   ├── MovementIntensityCalculator.kt
│           │   ├── RotationIntensityCalculator.kt
│           │   ├── StabilityCalculator.kt
│           │   ├── OrientationDetector.kt
│           │   └── MetricsEngine.kt
│           └── presentation/
│               ├── SensorBoundViewModel.kt
│               ├── components/            (GlassCard, StatusPill, MetricBar,
│               │                           LiveChart, SensorLifecycle, …)
│               ├── home/                  HomeScreen + ViewModel
│               ├── sensors/               SensorsScreen, SensorDetailScreen + VMs
│               ├── metrics/               DerivedMetricsScreen + VM
│               ├── experiments/           ExperimentsScreen + 4 experiment screens/VMs
│               ├── navigation/            SenseAppNavHost.kt (routes)
│               └── theme/                 Color.kt, Theme.kt
├── gradle/libs.versions.toml
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 7. Sensor Model & Sampling

`SensorKind` normalizes the ten supported hardware sensors:

| Kind | Android type | Units (primary) |
|------|--------------|------------------|
| Accelerometer | `TYPE_ACCELEROMETER` | m/s² |
| Gyroscope | `TYPE_GYROSCOPE` | rad/s |
| Magnetic Field | `TYPE_MAGNETIC_FIELD` | µT |
| Light | `TYPE_LIGHT` | lx |
| Proximity | `TYPE_PROXIMITY` | cm |
| Pressure | `TYPE_PRESSURE` | hPa |
| Gravity | `TYPE_GRAVITY` | m/s² |
| Linear Acceleration | `TYPE_LINEAR_ACCELERATION` | m/s² |
| Rotation Vector | `TYPE_ROTATION_VECTOR` | unit quaternion |
| Relative Humidity | `TYPE_RELATIVE_HUMIDITY` | % RH |

- Availability is probed once per repository via
  `SensorManager.getDefaultSensor(type) != null`.
- Samples are boxed into `SensorSample` with a `magnitude` (√(x²+y²+z²)) and a
  `scalar` (the meaningful value for one-axis sensors such as light).
- `SamplingRate` maps app-level (NORMAL/FAST) to `SensorDelay.GAME`/`UI`.
- Each collector gets its **own** `callbackFlow`, fully cancelled via
  `awaitClose` when the screen becomes invisible.

---

## 8. Live Sensor Explorer (V1 & V2)

### V1 — Dashboard (`Home`, `Sensors`)
- **Home** pages four featured sensors plus Quick Links to Experiments,
  Derived Metrics, and All Sensors.
- **Sensors** lists every sensor with live primary values and a
  LIVE/UNAVAILABLE pill.
- Each tile opens its detail screen via `sensor/{androidType}`.

### V2 — Detail screen with real-time graph
- Latest sample values + `min/max` seen on this screen visit.
- A scrolling, auto-scaling live graph drawn by `LiveChart`:
  - vector sensors → three colored axis traces
  - scalar sensors → a single mono trace
  - a labeled legend (X/Y/Z or unit)
- Data model is a **bounded in-memory window**: at most `MAX_HISTORY_SAMPLES =
  240` entries, appended at ~10 Hz (`HISTORY_THROTTLE_MS = 100`) so
  recomposition and drawing stay cheap.

---

## 9. Experiments (V3)

### Bubble Level
- Pitch/roll computed via **accelerometer + magnetometer rotation-matrix
  fusion** when a magnetometer exists (matching the reference project's
  `BubbleLevel`), falling back to pure gravity-vector trigonometry otherwise.
- Gravity is isolated with a low-pass filter (`alpha = 0.1`); pitch/roll are
  additionally exponential-smoothed (`alpha = 0.1`).
- Bubble rendered on a custom Canvas with a crosshair; LED-style
  level/drop indicator.
- Tolerances: **Precision 0.5°** · **Standard 2°** · **Rough 5°**.

### Compass
- Prefers the **rotation-vector** sensor (fused azimuth).
- Falls back to **accelerometer + magnetometer** rotation-matrix fusion.
- Shortest-path heading smoothing (`alpha = 0.25`) avoids north-wrap jumps.
- 8-way direction labels; dial rotates by `-heading`; heading readout is
  monospace numeric.

### Magnetic Field
- Live X/Y/Z, total magnitude (µT), peak (`maxObserved`).
- Intensity fraction = `magnitude / 200` clamped to [0, 1] — Earth's field
  (~45 µT) sits in the lower half; bringing a magnet near pushes toward 100%.

### Light Meter
- Live lux, category, log-scale meter `fraction = log10(lux + 1) / 5`.
- Categories (lux):

| Category | Range |
|----------|-------|
| Very Dark | < 5 |
| Dark | 5 – 50 |
| Normal | 50 – 300 |
| Bright | 300 – 1000 |
| Very Bright | ≥ 1000 |

- Tracks `maxObserved` lux.

---

## 10. Derived Metrics (V4)

A single pure-Kotlin `MetricsEngine` combines accelerometer + gyroscope flows
(with `flowOf(null)` substitution when the gyro is missing) and emits a
`MetricsSnapshot` of percentages, levels, pitch, roll and orientation. All
formulas are documented in source.

### Movement Intensity — accelerometer
1. Low-pass filter (α = 0.15) extracts gravity `g`; linear acceleration is
   `l = raw − g`.
2. Magnitude `m = |l|` is exponentially smoothed (`α = 0.15`).
3. Saturating curve: `percent = 100 · m / (m + 4)`.

| Level | Threshold (percent) |
|-------|---------------------|
| Still | < 5 |
| Low | 5 – 20 |
| Moderate | 20 – 45 |
| High | 45 – 75 |
| Very High | ≥ 75 |

### Rotation Intensity — gyroscope (rad/s)
- Magnitude of the angular-velocity vector, exponentially smoothed (α = 0.15).
- `percent = 100 · m / (m + 3)`.

| Level | Threshold (percent) |
|-------|---------------------|
| None | < 5 |
| Low | 5 – 20 |
| Moderate | 20 – 45 |
| High | 45 – 75 |
| Very High | ≥ 75 |

### Stability — accelerometer (+ gyro penalty)
- Rolling window (24 samples) of smoothed linear-acceleration magnitude.
- `instability = mean + stdDev` then `percent = 100 − clamp(instability · 18, 0, 100)`.
- When a gyroscope is present, rotational activity is penalized:
  `percent −= rotationPercent · 0.15` so a spinning-but-still phone does not
  read as perfectly stable.

| Level | Threshold (percent) |
|-------|---------------------|
| Very Stable | ≥ 85 |
| Stable | 65 – 85 |
| Moving | 40 – 65 |
| Unstable | < 40 |

### Device Orientation — gravity vector
- Dominant axis of the low-passed gravity vector:
  `|gz|` largest → Face Up / Face Down; `|gy|` → Portrait; `|gx|` → Landscape;
  otherwise Tilted.
- Smoothed with a **3-sample hysteresis** so the label does not flicker while
  moving between orientations.

### Pitch / Roll / Level
- `pitch = atan2(−gy, gz)`, `roll = atan2(gx, gz)` in degrees.
- `isLevel = |pitch| < 2° && |roll| < 2°`.
- Convention: roll > 0 ⇒ right side lower; pitch > 0 ⇒ top edge lower; both 0
  when flat.

---

## 11. Design System

Dark-first, futuristic Material 3 built on a custom palette (`SenseColors`):

- **Void** background with dot-grid + vertical gradient ambience.
- **Cyan / Purple** neon accents, glass cards (translucent filled surfaces with
  subtle accent borders), rounded 18 dp corners.
- Monospace numeric readouts, uppercase section labels, animated LIVE pips.
- Navigation: bottom bar with Home / Experiments / Sensors + Derive (metrics)
  quick entry; detail screens are full-screen surfaces with back navigation.
- Theming lives in `resentation/theme/` (`Color.kt`, `Theme.kt`); icons come from
  `material-icons-extended` (no emoji-as-icon).

---

## 12. Privacy, Permissions & Battery

- **Zero permissions.** `AndroidManifest.xml` declares none — no INTERNET, no
  location. The app cannot make network requests.
- **No background work.** Sensor collectors exist only while a screen is
  composed (`SensorLifecycle`); leaving a screen cancels the collection jobs.
- **Bounded memory.** History graphs keep at most 240 samples in memory.
- **No analytics, no crash-reporting SDKs, no advertising IDs.**

---

## 13. Build Environment

Requirements:

- **JDK 17+** (OpenJDK 17 tested on macOS arm64),
  e.g. `brew install openjdk@17`.
- **Android SDK** with `platform-tools`, `platforms;android-35`,
  `build-tools;35.0.0`. On macOS:
  ```bash
  brew install --cask android-commandlinetools
  sdkmanager --sdk_root=$ANDROID_HOME "platform-tools" \
      "platforms;android-35" "build-tools;35.0.0"
  ```
- **No NDK** is required (pure Kotlin/Java JNI-free).
- SDK location: `local.properties`:
  ```
  sdk.dir=/opt/homebrew/share/android-commandlinetools
  ```

---

## 14. Build & Run

```bash
# 1. Point at your JDK/SDK once per shell (adjust paths to your machine)
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools

# 2. Debug build → app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:assembleDebug --no-daemon

# 3. Release build (R8 minify + resource shrinking enabled)
./gradlew :app:assembleRelease --no-daemon

# 4. Install on a connected device/emulator
./gradlew :app:installDebug
```

Open the project in Android Studio (**Ladybug** or newer) or run the plugin
from command line — there is no wrapper-commit issue; the Gradle wrapper (8.13)
is included.

---

## 15. Testing

### Verification performed (on this machine, from clean builds)

| Step | Result |
|------|--------|
| `:app:assembleDebug` | **BUILD SUCCESSFUL** — Kotlin compile + AAPT2 + Dex + APK |
| `:app:assembleRelease` | **BUILD SUCCESSFUL** — R8 minify + resource shrink |
| Debug APK size | 16 MB |
| Release APK size | 1.0 MB (95% R8 shrinking) |
| `:app:lintDebug` | **BUILD SUCCESSFUL** — 0 code-quality issues |
| `:app:compileDebugKotlin` | **0 warnings** — all deprecated icons fixed to AutoMirrored variants |
| APK badge (`aapt dump`) | `com.sensephone.app`, `minSdk 26`, `targetSdk 35`, 0 permissions declared |
| Adaptive icon | monochrome tag present, foreground + background set |
| Commit | `618f910` — 56 files, clean working tree |

### Not yet verified
- **No physical Android device or emulator was available**, so no on-hardware
  runtime, sensor-behavior, or UI-interaction tests were run. The sensor
  formulas above are *implemented and documented*, not empirically calibrated
  against a real phone. See §20 for how to test on your device.

Suggested manual checklist on a real device:
1. Home shows four featured sensors with values; rotate/light the phone and
   watch them change.
2. Sensors tab lists all available sensors; UNAVAILABLE is shown for missing
   ones.
3. Sensor detail: graph scrolls and scales; min/max update; back works.
4. Bubble Level: place phone flat → "Level" lights up within the chosen
   tolerance.
5. Compass: heading tracks physical north (compare with a real compass).
6. Magnetic Field: bring a magnet near → intensity rises toward 100%.
7. Light Meter: cover sensor → "Very Dark"; window light → "Normal/Bright".
8. Derived Metrics: hold still → Stable/Very Stable, Still/None; shake →
   movements/rotation rise; screen-off while on a screen doesn't keep sampling.

---

## 16. Known Limitations

- **Hardware-dependent availability.** Experiments and metrics gracefully show
  "unavailable" when required sensors are missing, but the UI experience is
  best on a phone with a full sensor suite.
- **Formulas are heuristics**, not calibrated scientific instruments. All
  thresholds (see §10) are chosen for useful, legible behavior on typical
  phones and are constants in a single place for easy tuning.
- **No compass calibration UI.** Magnetic interference can't be filtered
  without access to the "Low-Check" / calibration APIs.
- **Graph window is bounded** (240 samples / ~10 Hz); it is a rolling
  visualization, not a recorder.
- **Not yet field-tested.** No physical device was available for runtime
  validation (§15). Performance on low-end hardware (graph recomposition) was
  not benchmarked.
- **Single-activity Compose.** Multi-window/split-screen behavior keeps the
  lifecycle hooks simple (screen = composition); this is intentional.

---

## 17. Development History

| Phase | What happened |
|-------|---------------|
| **Phase 0 — Recon** | Worked from the `AndroidSensors` reference repo, extracted its BubbleLevel rotation-matrix approach and overall sensor-catalog idea; everything else rewritten. |
| **Phase 1 — Foundation** | Created the Gradle root + `:app` module: version catalog, AGP/Kotlin/Compose wiring, theme/color/string resources, adaptive launcher icon, empty-no-permission manifest. |
| **Phase 2 — Core layers** | Implemented the data layer (SensorKind, SensorSample, SensorRepository, `AndroidSensorRepository` via `callbackFlow`) and the pure-Kotlin metric domain layer (`MetricsEngine`, calculators, filters, orientation detection). |
| **Phase 3 — ViewModels** | Base `SensorBoundViewModel` lifecycle contract plus VMs for Home, Sensors, Sensor Detail, Derived Metrics, and all four experiments. |
| **Phase 4 — Screens & navigation** | Compose screens for all features, custom graph/level/compass canvases, shared components, and `SenseAppNavHost` with type-safe-enough routes. |
| **Phase 5 — Build verification** | JDK 17 + Android SDK 35 installed locally; fixed compile errors (material-icon variants, missing Compose imports, non-composable `LocalContext` access inside `remember`); debug and release builds green. |
| **Phase 6 — Lint cleanup** | Fixed all Kotlin deprecation warnings (`Icons.Filled.*` → `Icons.AutoMirrored.Filled.*`), added monochrome launcher icon tag for Android 13 themed icons, removed unused XML color resources, suppressed `ObsoleteSdkInt` false positive. Zero code-quality lint issues confirmed. |

---

## 18. Dependencies

Everything comes from the AndroidX/Google ecosystem. No third-party runtime
libraries.

| Artifact | Version |
|----------|---------|
| Android Gradle Plugin | 8.7.3 |
| Kotlin + Compose compiler plugin | 2.0.21 |
| Gradle (wrapper) | 8.13 |
| core-ktx | 1.15.0 |
| lifecycle-runtime-ktx / viewmodel-compose / runtime-compose | 2.8.7 |
| activity-compose | 1.9.3 |
| Compose BOM | 2024.12.01 |
| material3 | (BOM) 1.3.1 |
| material-icons-extended | (BOM) 1.7.6 |
| navigation-compose | 2.8.5 |

Full catalog: `gradle/libs.versions.toml`.

---

## 19. References, Acknowledgments & License

### Reference project
[Sense-Premium's AndroidSensors](https://github.com/AkshayAshokCode/AndroidSensors)
by **AkshayAshokCode** — the inspiration for the sensor catalog and the
BubbleLevel rotation-matrix technique. Released under the **MIT License**.

### License

SensePhone itself is released under the MIT License. See the
`LICENSE` file in this repository.

```
MIT License

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND.
```

---

## 20. How to Test on Your Phone

### Prerequisites

1. An Android phone running **Android 8.0** (API 26) or newer.
2. A **USB cable** (or Wi-Fi ADB for wireless).
3. **Android Studio** (for easiest path) OR just the **APK file** for manual install.

### Step 1 — Enable Developer Options on your phone

- Go to **Settings → About Phone**.
- Tap **Build Number** 7 times.
- You'll see "You are now a developer!".

### Step 2 — Enable USB Debugging

- Go to **Settings → Developer Options** (now visible).
- Turn on **USB Debugging**.
- Confirm the dialog that appears when you connect your phone.

### Step 3 — Connect your phone

Connect your phone to your Mac via USB cable. When prompted on the phone, tap
**Allow** (for USB debugging authorization).

---

### Method A — Install via Android Studio (Recommended)

1. Open the project: **File → Open** → `/Users/shahabas/Documents/GitHub/sensePhone`
2. Wait for Gradle sync to finish (bottom status bar).
3. Your phone appears in the device dropdown (top toolbar).
4. Click the green **Run** button.
5. SensePhone builds, installs, and launches on your phone.

### Method B — Install via Command Line (USB)

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools

# Verify phone is connected
adb devices

# Build + install in one step
cd /Users/shahabas/Documents/GitHub/sensePhone
./gradlew :app:installDebug --no-daemon

# Or install a pre-built APK directly
adb install app/build/outputs/apk/debug/app-debug.apk
```

SensePhone will appear in your app drawer.

### Method C — Transfer the APK (No USB Required)

1. **Build the APK** on your Mac:
   ```bash
   export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
   export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
   cd /Users/shahabas/Documents/GitHub/sensePhone
   ./gradlew :app:assembleDebug --no-daemon
   ```
   Output: `app/build/outputs/apk/debug/app-debug.apk` (~16 MB)

2. **Transfer to your phone** via:
   - Google Drive / Dropbox / OneDrive — upload, open the link on phone, download
   - Email — email the APK to yourself, open the attachment on phone
   - USB file copy — connect phone, copy APK to its Downloads folder

3. **Install on your phone:**
   - Open the phone's **File Manager** app → navigate to **Downloads**
   - Tap `app-debug.apk`
   - If prompted "Install from unknown source?", tap **Settings** → allow
     the source → go back → **Install**
   - Open **SensePhone** from the app drawer

---

### What to Test on Your Phone

**Home Screen:**
- Four featured sensors should show live values.
- Rotate your phone — accelerometer values change.
- Tap a sensor card → opens detail screen with live graph.
- Bottom navigation: Home | Experiments | Sensors.

**Sensors Tab:**
- Lists all 10 sensor kinds with LIVE/UNAVAILABLE badges.
- Tap any sensor → detail screen with real-time graph and min/max values.
- Graph scrolls smoothly, axis auto-scales.

**Experiments Tab:**
- **Bubble Level** — place phone flat on a table; bubble should center
  within tolerance. Tap tolerance modes to change sensitivity.
- **Compass** — rotate the phone; heading and dial should track. Compare
  with a real compass.
- **Magnetic Field** — bring a magnet near the phone; intensity bar should
  rise toward 100%.
- **Light Meter** — cover the light sensor → "Very Dark"; shine light →
  "Normal/Bright".

**Derived Metrics Tab:**
- Hold phone still → Movement: Still, Rotation: None, Stability: Very Stable.
- Shake phone → values rise.
- Orientation changes when you rotate the device.

### Troubleshooting

| Problem | Solution |
|---------|----------|
| `adb devices` shows nothing | Ensure USB debugging is on, phone is connected, and you tapped "Allow" on the phone |
| App crashes on launch | Check Logcat in Android Studio (or `adb logcat`) for the stack trace |
| "Install from unknown source" | Settings → Security → enable the source you're installing from |
| Graph is flat/not moving | Ensure you're on a real device (emulators may have limited sensor data) |
| Sensor shows UNAVAILABLE | That sensor isn't on your phone — this is normal behavior, not a bug |

---

*Built offline, for offline — SensePhone.*