# SensePhone


## 1. Tech Stack

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

## 2. Architecture

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

## 3. Project Structure

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




## 4. Build Environment

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

## 5. Build & Run

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

## 6. Testing

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


## 7. Development History

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

## 8. Dependencies

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