# WORKING.md

This document explains **how SensePhone derives and delivers each piece of
functionality** — the exact math, the exact lifecycle, and the exact wiring
from the hardware sensor up to the pixel on screen. Read this alongside the
code; every paragraph maps to a file you can open.

```
┌────────────┐   registerListener     ┌─────────────────────┐
│ SensorManager │ ───────────────────────▶ │ AndroidSensorRepository │
│ (hardware)  │   events@NORMAL/FAST  │  (callbackFlow)     │
└────────────┘                        └──────────┬──────────┘
                                                 │ Cold Flow<SensorSample>
                                                 ▼
                    ┌────────────────────────────┘
                    ▼
        ┌─────────────────────┐   combine()   ┌─────────────────────┐
        │  ViewModels          │ ◀──────────── │  MetricsEngine       │
        │  (SensorBoundViewModel)              │  (pure Kotlin maths) │
        └─────────┬───────────┘                └─────────────────────┘
                  │ StateFlow<UiState>
                  ▼
        ┌─────────────────────┐   Canvas draw ┌──────────────┐
        │  Compose Screens    │ ─────────────▶ │ LiveChart /   │
        │  (collectAsState...)│   per frame    │ level dials   │
        └─────────────────────┘                └──────────────┘
```

**The one rule that governs everything:** a sensor is only listened to while the
screen that shows it is composed. No screen → no listener → no battery drain.

---

## 1. How sensor data is turned into a Flow

`app/src/main/java/com/sensephone/app/data/sensor/`

### 1.1 `SensorKind` — the catalog
Each supported sensor is an enum entry carrying:

```kotlin
ACCELEROMETER(Sensor.TYPE_ACCELEROMETER, "Accelerometer", "m/s²", 3, 0)
//                  androidType           displayName       unit   valueCount  priority
```

- `valueCount` is **1 for scalar sensors** (light, proximity…) and **3 for
  vector sensors** (accel, gyro, mag…).
- `isVector = valueCount >= 3` — this decides how a raw `values[]` array is
  interpreted (see 1.3).
- `priority` orders the full sensor list: accel(0) → gyro(1) → mag(2) → light(3)
  → proximity(4) → pressure(5) → gravity(6) → linear accel(7) → rotation
  vector(8) → humidity(9).

### 1.2 `SensorSample` — one immutable reading
Packs a single `SensorEvent`:

```kotlin
data class SensorSample(
    val timestampNanos: Long,   // event.timestamp, used for ordering
    val x: Float, val y: Float, val z: Float,   // vector axes (0 for scalar)
    val scalar: Float                            // value for scalar sensors
) {
    val magnitude: Float
        get() = sqrt(x² + y² + z²)               // √(·), computed on demand
}
```

**Derivation:** `magnitude` is the Euclidean norm of the vector
`√(x² + y² + z²)`. For the accelerometer this is total accel including gravity
(≈9.81 at rest); for the gyroscope it is total angular speed (rad/s); for the
magnetometer it is total field strength (µT).

### 1.3 `AndroidSensorRepository.observe()` — the heart
`callbackFlow { ... }` is the contract between coroutines and the
`SensorEventListener`:

1. When a collector starts, `registerListener(listener, sensor, rate)` is
   called — **the sensor starts sampling**.
2. `onSensorChanged` builds a `SensorSample` and `trySend(sample)` pushes it
   downstream:
   ```kotlin
   x = if (kind.isVector) event.values[0] else 0f
   y = if (kind.isVector) event.values[1] else 0f
   z = if (kind.isVector) event.values[2] else 0f
   scalar = if (kind.isVector) 0f else event.values[0]
   ```
   So vector sensors populate x/y/z; scalar sensors populate `scalar`.
3. `awaitClose { sensorManager.unregisterListener(listener) }` — **when the
   collector is cancelled, the sensor stops immediately**.

Because the flow is **cold**, each subscriber gets its own listener. The
repository never registers anything globally.

> If the sensor is missing (`getDefaultSensor` == null), the flow just `close()`s
> and never emits — the UI decides how to present that (see §4).

### 1.4 `SamplingRate` — how aggressive
Maps an app-level cadence to Android delay constants:

| Rate | Android constant | Used by |
|------|------------------|---------|
| `FAST`   | `SENSOR_DELAY_GAME`  | experiments (bubble level, compass, mag, light) |
| `NORMAL` | `SENSOR_DELAY_NORMAL`| dashboards + sensor graphs |
| `UI`     | `SENSOR_DELAY_UI`    | glanceable overviews |

---

## 2. The lifecycle rule: only sample while visible

`presentation/SensorBoundViewModel.kt` + `presentation/components/SensorLifecycle.kt`

Every ViewModel extends `SensorBoundViewModel`, which owns the repository and a
list of active collection **Jobs**:

```kotlin
protected fun <T> collect(flow: Flow<T>, onEach) {
    val job = viewModelScope.launch { flow.collect { onEach(it) } }
    activeJobs += job          // remembered
}
fun onScreenInactive() { activeJobs.forEach { it.cancel() }; activeJobs.clear() }
```

The composable `SensorLifecycle(viewModel)` binds this to composition:

```kotlin
LaunchedEffect(Unit) { viewModel.onScreenActive() }        // start streaming
DisposableEffect(Unit) { onDispose { viewModel.onScreenInactive() } } // stop
```

**Derivation:** `onScreenActive()` calls `repository.observe(...)` -> the
callbackFlow registers the listener -> hardware delivers events. Leaving the
screen cancels every Job -> `awaitClose` unregisters the listener -> hardware
goes quiet. Result: **a sensor is sampled exactly when, and only when, its
screen is on screen.**

---

## 3. V1 — Home: what the dashboard shows and why

`presentation/home/HomeViewModel.kt`

- `availableKinds` is `repository.discoverAvailable()` — every `SensorKind`
  whose `getDefaultSensor(type) != null`, sorted by `priority`.
- `featured` is a fixed subset: **accelerometer, gyroscope, magnetometer,
  light**. Each available featured sensor gets its own `NORMAL`-rate collector.
- `metrics` is a `MetricsSnapshot` from the `MetricsEngine` (see §6).

Each render shows:
- The **latest sample** per featured sensor (x/y/z or scalar + magnitude).
- A **LIVE pill** if the sensor exists; `N/A` (red) otherwise.
- The **derived metrics panel** (movement / rotation / stability / orientation).

---

## 4. V2 — Sensor detail screen & the live graph

`presentation/sensors/SensorDetailViewModel.kt`, `presentation/components/LiveChart.kt`

### 4.1 Windowing (bounded memory)
```kotlin
const val MAX_HISTORY_SAMPLES = 240
const val HISTORY_THROTTLE_MS = 100L
```
- Only **one sample every 100 ms** (≈10 Hz) is appended to history, no matter
  how fast the sensor fires.
- History is capped at **240 samples** — `(history + sample).takeLast(240)`.
- `latest` is updated on *every* event; `history` (the graph) on the throttle.
  At 10 Hz the 240-sample window spans **~24 seconds** — the detail card titles
  the graph "Live History (last ~24s)".

### 4.2 Drawing — `LiveChart` (one `Canvas`, no chart library)
For each incoming state:
- Series = one list per channel (X / Y / Z for vectors, the scalar alone for
  scalar sensors).
- **Dynamic y-range:** min/max is computed across *all* channels, padded by
  10%: `minV -= range*0.1; maxV += range*0.1`. If flat, it widens ±1 so the
  line never sits on an edge.
- **x mapping:** `x = i * (width/(n-1))`; series are **right-aligned**
  (`startOffset = numSamples - series.size`) so the newest sample is always at
  the right edge.
- **y mapping:** `y = height - ((v - minV)/range) * height` (inverted so up = +
  in the UI).
- Horizontal grid = 3 lines; each channel drawn as a stroked `Path` with a dot
  at the last point.

### 4.3 Stat strip
- **Current Values card** (`SensorDetailScreen`): latest `X/Y/Z` (each axis tinted
  X=cyan, Y=green, Z=amber) plus **magnitude** for vector sensors, or the scalar
  value for scalar sensors, before any sample arrives showing "Waiting for first
  reading…".
- **Window Statistics card**: the **average** of each channel over the *current
  bounded window* (`history.map { it.x }.average()`, etc.), displayed per axis,
  with the caption "Average of last N/240 samples" — the N is live, so the
  number grows until the window fills.

---

## 5. V3 — Experiments: each equation, each branch

### 5.1 Bubble Level — `presentation/experiments/BubbleLevelViewModel.kt`

Goal: answer "is the phone flat?" as pitch/roll angles.

1. **Gravity isolation:** a `LowPassFilter(alpha = 0.1)` is applied to raw
   accelerometer → `(gx, gy, gz)`, the gravity vector.
2. **Two derivation paths:**
   - **With magnetometer** (rotation-matrix fusion, ported from the reference
     project):
     ```kotlin
     SensorManager.getRotationMatrix(rm, null, [gx,gy,gz], magnetVector)
     SensorManager.getOrientation(rm, angles)
     pitch = degrees(angles[1]); roll = degrees(angles[2])
     ```
   - **Without magnetometer** (pure gravity trigonometry):
     ```kotlin
     pitch = degrees(atan2(-gy, gz))   // rotations about X axis
     roll  = degrees(atan2( gx, gz))   // rotations about Y axis
     ```
     Both are 0 when flat because `g ≈ (0, 0, 9.81)`.
3. **Smoothing:** `smoothed += 0.1 * (raw - smoothed)` per axis to kill jitter.
4. **Decision:**
   ```kotlin
   isLevel = |pitch| < tolerance && |roll| < tolerance
   ```
   Tolerances: **Precision 0.5° · Standard 2° · Rough 5°** (user-selectable).
5. Bubble rendered by mapping `pitch/roll` to an offset from center; a
   LED-style dot + crosshair show how far off level you are.

### 5.2 Compass — `presentation/experiments/CompassViewModel.kt`

Goal: get azimuth (heading), smooth it, and pick a direction name.

1. **Preferred path — rotation vector:**
   ```kotlin
   SensorManager.getRotationMatrixFromVector(rm, sample.xyz)
   SensorManager.getOrientation(rm, angles)
   azimuth = degrees(angles[0])
   ```
   The rotation-vector sensor is a hardware-software fused estimate of
   orientation — the cleanest azimuth source.
2. **Fallback path — accel + mag fusion** (rotation matrix):
   ```kotlin
   SensorManager.getRotationMatrix(rm, null, accel, mag)   // then getOrientation
   azimuth = degrees(angles[0])
   ```
   Requires *both* latest accel and latest mag samples (`lastAccel ?: return`).
3. **Normalize:** `azimuth = ((azimuth % 360) + 360) % 360` → 0..360.
4. **Shortest-path smoothing** (avoid 359°→1° wrap jumps):
   ```kotlin
   diff = (normalized - smoothed + 540) % 360 - 180
   smoothed = (smoothed + 0.25 * diff + 360) % 360
   ```
5. **Direction name:**
   ```kotlin
   DIRECTIONS = ["N","NE","E","SE","S","SW","W","NW"]
   index = ((normalized + 22.5) / 45).toInt() % 8
   ```
   +22.5° centers the octants so 0°→N, 45°→NE, etc. The dial rotates by
   `-heading`.

### 5.3 Magnetic Field — `presentation/experiments/MagneticFieldViewModel.kt`

Straightforward reading + two derived indicators:

- `magnitude = sample.magnitude` (√(x²+y²+z²), µT).
- `maxObserved = max(previous, magnitude)` — the peak since screen opened.
- **Intensity fraction:**
  ```kotlin
  intensityFraction = (magnitude / 200f).coerceIn(0f, 1f)
  ```
  Rationale: Earth's field ≈ 45 µT sits in the lower half (~22%); a strong
  magnet (100–200 µT+) saturates toward 100%. Great for probing magnets.

### 5.4 Light Meter — `presentation/experiments/LightMeterViewModel.kt`

- `lux = sample.scalar` (ambient, lx). `maxObserved` tracks the peak.
- **Category** from thresholds:
  ```kotlin
  Very Dark  < 5 | Dark 5–50 | Normal 50–300 | Bright 300–1000 | Very Bright ≥ 1000
  ```
  Implemented as: largest `LightLevel` whose `lowerBoundLux <= lux`.
- **Log-scale bar** so indoor light and full sun are both visible:
  ```kotlin
  fraction = (log10(lux + 1) / 5).coerceIn(0f, 1f)
  ```
  `log10` compresses 0…10⁵ lux into 0…5, and +1 keeps `log10(0)=0`. So dimmer
  indoors (≈1–100 lx) gets a meaningful bar span instead of being crushed by
  sunlight numbers.

---

## 6. V4 — Derived metrics: the math, end to end

`domain/metrics/` — this package imports **no Android classes**, it is pure
math over Flows.

`MetricsEngine.observe()` `combine()`s the accelerometer flow with the gyroscope
flow (`flowOf(null)` substituted when there is no gyro) and emits a
`MetricsSnapshot` on every accelerometer event. If there is no accelerometer at
all, it emits a single `MetricsSnapshot(hasAccelerometer = false)` so the UI
always has a well-defined state.

### 6.1 `LowPassFilter` (the foundation)
First-order exponential smoother — the gravity estimator:
```kotlin
out += alpha * (raw - out)        // alpha ≈ 0.1–0.15
```
**Why:** raw accelerometer = gravity + movement. At rest, α-persistence over
many samples converges on gravity alone; the residual
`linear = raw - gravity` is the movement signal.

### 6.2 Movement Intensity — `MovementIntensityCalculator`
```kotlin
g             = lowPass(raw, 0.15)                 // gravity
linear        = raw - g                            // movement-only signal
m             = |linear|                           // magnitude, m/s²
smoothedMag  += 0.15 * (m - smoothedMag)           // exponential smoothing
percent       = 100 * smoothedMag / (smoothedMag + 4)
```
**Derivation of the curve:** it's a saturating function that lands at 0% for a
still phone and approaches 100% as sustained linear acceleration grows:
- `m=0.0 → 0%`, `m=1 → 20%`, `m=4 → 50%`, `m=36 → 90%`.

Thresholds: Still <5% · Low <20% · Moderate <45% · High <75% · Very High ≥75%.

### 6.3 Rotation Intensity — `RotationIntensityCalculator`
```kotlin
m           = |gyroVector|                          // total angular speed, rad/s
smoothedMag += 0.15 * (m - smoothedMag)
percent     = 100 * smoothedMag / (smoothedMag + 3)
```
Same idea on the gyroscope. Note the denominators differ (4 vs 3) because gyro
units (rad/s) and accel units (m/s²) live on different scales.

Thresholds: None <5% · Low <20% · Moderate <45% · High <75% · Very High ≥75%.

### 6.4 Stability — `StabilityCalculator`
```kotlin
sliding window (24) of smoothed linear-accel magnitudes
mean    = average(window)
stdDev  = sqrt(Σ(xᵢ - mean)² / n)
instability = (mean + stdDev) * 18, clamped 0..100
percent     = 100 - instability
```
**Derivation:** a still phone has tiny mean *and* tiny variance → instability≈0
→ ~100% stable. Shaking raises both the average movement and how much it jumps
around → score drops.

In `MetricsEngine`, when a gyroscope exists, rotational activity is penalized so
a *spinning-but-still* phone doesn't read as stable:
```kotlin
stability = (base - rotationPercent * 0.15).coerceIn(0f, 100f)
```

Thresholds: Very Stable ≥85% · Stable ≥65% · Moving ≥40% · Unstable <40%.

### 6.5 Device Orientation — `OrientationDetector`
Classifies the **gravity vector** axis dominance:
```kotlin
|gz| largest (>7.5)  → gz>0 → FACE_UP,   else FACE_DOWN
|gy| largest (>7.0)  → PORTRAIT
|gx| largest (>7.0)  → LANDSCAPE
otherwise            → TILTED
```
**Hysteresis (3 consecutive samples)** — a candidate must be seen 3 times in a
row before the reported orientation changes, preventing flicker when the phone
is between orientations.

### 6.6 Pitch, Roll, Level — `MetricsEngine`
```kotlin
pitch = degrees(atan2(-gy, gz))     // rotation about X
roll  = degrees(atan2( gx, gz))     // rotation about Y
isLevel = |pitch| < 2° && |roll| < 2°
```
Conventions: `roll > 0` ⇒ right side lower; `pitch > 0` ⇒ top edge lower; both
`0` flat. `isLevel` is the "level" flag shown on Home/metrics.

### 6.7 `MetricsSnapshot`
A single immutable value object carrying every derived field (percentages,
classified levels, orientation, pitch/roll, isLevel, and availability flags
`hasAccelerometer/hasGyroscope/hasMagnetometer`) so the UI binds to one thing.

---

## 7. Navigation & experiment availability

`presentation/navigation/SenseAppNavHost.kt`

- Bottom-tab routes: `home` · `experiments` · `sensors`; plus full-screen
  routes `metrics`, `sensor/{androidType}`, and
  `experiment/{bubble|compass|magnetic|light}`.
- `rememberExperimentAvailability()` builds a `SensorRepository` once and maps:
  | Experiment | Requires |
  |------------|----------|
  | Bubble Level | accelerometer |
  | Compass | rotation vector **OR** (accelerometer AND magnetometer) |
  | Magnetic Field | magnetometer |
  | Light Meter | light sensor |

  Because compass has a fallback, it can be available even when rotation-vector
  is missing. The experiment tile is shown enabled/disabled based on this.

---

## 8. State → pixels (summary of the render path)

1. ViewModel holds a `StateFlow<UiState>`.
2. Screen calls `collectAsStateWithLifecycle()` — **only updates while resumed**.
3. `SensorLifecycle(viewModel)` keeps collection in lock-step with composition.
4. `LiveChart` draws every recomposition with whatever history is in state;
   experiments draw their dials/bubbles/meters directly onto `Canvas` with the
   same tap of data.

No event bus, no shared mutable singletons, no background loops. The pipeline is
one-way: **Flow → ViewModel state → Compose.**

---

*SensePhone — everything above is derived on-device, from the hardware up.*