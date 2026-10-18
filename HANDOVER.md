# AR-Stride — handover

Everything needed to pick this project back up cold: in a new session, on a new
machine, or under a different account. Written 18 October 2026, against versionCode 3
(versionName 1.3).

**Owner / contact:** Abdur Rehman (AR) — **gabdurehman493@gmail.com** —
GitHub [ABDURREHMAN629](https://github.com/ABDURREHMAN629)

---

## 1. What this is

An offline-first Android fitness app, built for one person (male, 82 kg, 162.6 cm /
5 ft 4 in — all editable in the app). It does:

- **Step counting** in the background, all day, that **refuses to count while riding**
  a bike or sitting in a vehicle. This is the hard requirement the whole design turns
  around.
- **Run / walk recording** with a GPS route, drawn on **offline map tiles** downloaded
  once per city.
- **Strength training**: full-body home exercises, two standing programmes, and a
  **30-day plan**.
- **Yoga** and **posture correction** routines.
- **Mind**: box breathing (4-4-4) and three other paced patterns, 5-4-3-2-1 grounding
  at two minutes per sense, and short attention drills.

No account, no server, no analytics. The only network call in the entire app fetches
map tiles.

## 2. State of play

| Area | State |
|---|---|
| Builds, installs, launches | verified on device |
| 15 unit tests | all pass (`:app:testDebugUnitTest`) |
| Background step service | holds with the `health` foreground type |
| Still / desk detection | correct: reports `Still`, zero rhythm |
| Walking vs riding classification | **fixed but not field-tested** — see §6 |
| Run distance / pace | **fixed but not field-tested** — see §6 |
| Offline map download | blocked once by a ROM permission; see §5 |
| Mind, training, yoga, posture, profile | present and working in the shipped APK |

## 3. Build and install

Needs the Android SDK (platform 37) and a JDK — Android Studio's bundled one works.

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleRelease
```

The APK lands at `app/build/outputs/apk/release/app-release.apk`. AGP 9 does **not**
create a `testReleaseUnitTest` task, so use the debug one.

Install over USB:

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

### Signing

`keystore.properties` and `keystore/` are **not in version control**. Without them the
release build still compiles, it just comes out unsigned. To sign:

```bash
cp keystore.properties.example keystore.properties
keytool -genkeypair -v -keystore keystore/my-release.jks -alias myalias -keyalg RSA -keysize 2048 -validity 10000
```

Then put the path, alias and passwords into `keystore.properties`.

Android only installs an update over an existing copy if both are signed by the **same**
key. The key this project's own builds used (CN=Stride, SHA-256 starting `a59822`)
exists only on AR's machine, at `keystore/stride-release.jks`. If it is lost, the next
install has to be a fresh one, and uninstalling wipes the recorded step and run history
with it. **Back that file up somewhere private.**

### Toolchain it was actually built with

Gradle 9.7.1 · AGP 9.4.0 · Kotlin 2.4.20 · KSP 2.3.11 · Compose BOM 2026.08.00 ·
Room 2.8.4 · compileSdk 37 / minSdk 26 / targetSdk 36 · Java 17.

Two traps that cost time before:

- **AGP 9 has built-in Kotlin support.** Applying `org.jetbrains.kotlin.android`
  alongside it fails the build. The `kotlin-android` alias is still in
  `libs.versions.toml` but must stay unused.
- **KSP versions independently** of Kotlin — `2.3.11`, not `2.4.20-something`.

## 4. The accuracy design (the part worth understanding first)

A phone in your pocket while you pedal produces a rhythmic, roughly 1 Hz signal that
looks like walking to any naive pedometer, and the hardware step counter inside the
phone counts it. So hardware steps are treated as **candidates**, not truth, and a
batch is credited only if the gate agreed with it at the time those steps happened.

Pipeline, in order:

1. **`sensors/StepService.kt`** — foreground service. Listens to `TYPE_STEP_COUNTER`
   (batched, so the CPU can sleep) plus a decimated ~50 Hz accelerometer stream.
2. **`sensors/MotionClassifier.kt`** — once a second, over a rolling window, computes
   five features: standard deviation, robust peak-to-peak, kurtosis, autocorrelation
   **periodicity**, and **dominantHz** (cadence). Returns `STILL`, `WALKING`,
   `RUNNING`, `CYCLING`, `VEHICLE` or `UNKNOWN`.
3. **`sensors/PedestrianGate.kt`** — hysteresis (two consecutive windows to open,
   three to close) plus a veto from Play Services activity recognition. Keeps a
   **verdict history**, because batched steps arrive *after* the fact and have to be
   judged against what the phone was doing when they were taken.
4. **`data/repo/StrideRepository.kt`** — credits the batch, deriving distance and
   calories from that batch's own cadence, and separately records what was **rejected**
   so the filter is visible rather than silent (Today screen, accuracy card).

### The octave bug — read this before retuning anything

A pocketed phone's **strongest** autocorrelation repeat is the *stride* (two steps),
not the step. Take it at face value and a 1.8 Hz walk reads as 0.9 Hz, which lands
squarely inside the cycling band. That is exactly why half of a real 5,500-step walk
was thrown away on 17 Oct 2026: 2,189 steps counted, 2,765 rejected as riding, 608
unclear — 39 % of a walk that was entirely on foot.

The fix, in `MotionClassifier.classify()`: after finding the best lag, look near
**half** that lag; if that peak is strong in absolute terms (`OCTAVE_MIN_SCORE = 0.20`)
and relative to the best (`OCTAVE_RATIO = 0.55`), the step rate is the half-lag.
`MotionClassifierTest` asserts the pocketed-walk case reads 1.5–2.1 Hz, so a regression
here fails the tests instead of silently eating steps again.

The cycling rule was also too greedy, and now requires a *smooth* signal rather than
merely a slow one:

```
dominantHz in 0.75..1.45 && periodicity >= 0.30 && p2p < walkP2p
  && sd < walkSd * 1.6 && kurtosis < walkKurtosis
```

Walking has heel strikes, so its peak-to-peak and kurtosis are both high; pedalling is
a smooth wave. The `VEHICLE` fallback likewise requires `periodicity < 0.35`, so
genuine gait cannot fall into it.

`PedestrianGate.isConfidentGait()` lets an unmistakable gait (`periodicity >= 0.45`,
`kurtosis >= 2.3`, `p2p >= 5.0`, `dominantHz` 1.3–3.8) override the activity-recognition
veto, because that API lags and sometimes insists you are still cycling minutes after
you got off the bike.

### Retuning knobs

All in `MotionClassifier.kt`, per `Sensitivity` (STRICT / BALANCED / RELAXED, selectable
in the app under Accuracy). BALANCED is `walkP2p = 4.0`, `walkSd = 0.95`,
`walkPeriodicity = 0.26`, `walkKurtosis = 2.15`. Change one, run the tests, then field
test — and **use the log, do not guess**.

### The motion log

`sensors/MotionLog.kt` writes a CSV of every non-still window and every step batch with
the verdict it received:

```bash
adb pull /sdcard/Android/data/com.stride.fit/files/motion-log.csv
```

It rotates at 4 MB and never leaves the phone otherwise. This is the evidence for any
future tuning: walk for ten minutes, pull the file, and look at what the features
actually were in the windows that were misjudged.

## 5. Run tracking and offline maps

**`run/RunMath.kt`** — `GeoKalman` smooths fixes using the reported accuracy as the
measurement variance. `RouteAccumulator` turns them into distance behind four guards:
drop fixes worse than `MAX_ACCURACY_M = 40`, ignore movement below `MIN_STEP_M` or
below `JITTER_FRACTION = 0.35` of the fix's own error, drop impossible speeds, and
smooth altitude before counting any climb.

`MAX_ACCURACY_M` was 25 and is now 40, because this phone averaged **75 m** reported
accuracy and nearly every fix was being binned.

**`run/RunService.kt`** also listens to the step counter, so a run still gets distance
when GPS is unavailable (`DistanceSource.GPS` / `STEPS` / `MIXED`, shown in the UI).
Step distance is only added while GPS is **not** healthy (no fix for 10 s), and
`RouteAccumulator.reanchor()` is called when GPS returns so that the gap the steps have
already covered is not counted a second time as one straight-line jump. There is a
regression test for exactly that.

On 17 Oct 2026 a 1 h 28 min run recorded 0 m: **location was switched off for the whole
activity** (0 locations, 0 s active duration) and the app said nothing. `RunScreen` now
warns before and during, offers "Turn on location", and offers "Start without GPS".

**Maps** (`map/`): Web Mercator tile arithmetic, a file-backed tile store under
`filesDir` (not `cacheDir`, so Android will not quietly delete a downloaded city), and
a deliberately polite downloader — one request at a time, 120 ms apart, a 2,500-tile
ceiling per area, retries, and an early abort after repeated DNS failures.

> **The ROM trap.** On this phone (Voltage OS) the first download failed on all 2,112
> tiles with `UnknownHostException`. LineageOS-based ROMs turn **INTERNET into a
> per-app, revocable permission**, and it starts **off** for sideloaded apps. Fix:
> Settings → Apps → AR-Stride → **Mobile data and Wi-Fi** → allow. The downloader now
> detects this case and says so, instead of grinding through thousands of failures.

## 6. Open items, in priority order

1. **Field-test the step classifier.** Walk 10+ minutes with the phone pocketed, then
   `adb pull` the motion log and check how many windows came out `WALKING` versus
   `CYCLING`. This is the only thing that proves the octave fix works in the real
   world. Then ride a bike for ten minutes and confirm those steps are still rejected.
2. **Field-test run tracking** with location **on**: distance, pace, and whether the
   route actually draws.
3. **Full map download** for a ~10 km radius, after allowing the network permission.
4. **Battery check** across a full day of 50 Hz sampling.
5. Device-level whitelist, to be run by the phone's owner:
   ```bash
   adb shell dumpsys deviceidle whitelist +com.stride.fit
   ```
   and in Settings: **App battery usage → Unrestricted**. There is no Autostart
   setting on LineageOS-based ROMs; that advice only applies to stock Xiaomi firmware.

## 7. Test device

Xiaomi Redmi 10A (`fog`, 220333QAG) running **Voltage OS** (`voltage_fog-user`), a
LineageOS-based ROM on Android 17 / SDK 37, arm64. **Not stock MIUI or HyperOS** — do
not apply Xiaomi-specific advice to it. Google Play runs sandboxed
(`app.grapheneos.gmscompat`), and Play Services is present, so the activity-recognition
veto is live. The hardware pedometer and step detector are both present, so the
accelerometer-only fallback is not in use here.

Two quirks worth knowing: USB data is blocked while the phone is locked (unlock before
`adb`), and the ROM occasionally sweeps background apps — exit info once showed
AR-Stride and GmsCore both `STOP_APP`'d in the same second, which is the ROM, not the
user pressing stop.

## 8. Code map

```
Brand.kt            display name ("AR-Stride"); must match app_name in strings.xml
MainActivity.kt     nav host: today / run / maps / train / posture / mind / you / accuracy
StrideApp.kt        Application, notification channels, service start

sensors/            MotionClassifier, PedestrianGate, StepService, MotionLog,
                    ActivityGateReceiver, BootReceiver   <- the accuracy engine
run/                RunMath (Kalman + route accumulator), RunService
map/                TileMath, TileStore, TileDownloader, RouteMap
training/           Exercises, Posture, Yoga, Programs, Challenge30, Mindfulness
health/             HealthMath (BMR, METs, ACSM, stride, HR zones, 1RM)
data/db/            Entities, Daos, StrideDatabase (v2, hand-written MIGRATION_1_2)
data/repo/          StrideRepository (mutex-guarded day totals), SettingsStore
ui/                 ViewModels, MapViewModel, MindViewModel, components/, screens/, theme/
util/               Time + Fmt, Notifications

app/src/test/       MotionClassifierTest (11 tests), RouteAccumulatorTest (4 tests)
```

**Database:** version 2. Adding a column or a table means writing a new migration —
never `fallbackToDestructiveMigration()`, because none of this data can be re-downloaded
from anywhere.

**Naming:** the display name is AR-Stride everywhere; the package id is `com.stride.fit`
and deliberately does **not** follow it. Changing the package id would make Android
treat this as a different app and orphan every recorded step, run and workout.

## 9. The maths, and where it came from

| Quantity | Method |
|---|---|
| BMR | Mifflin-St Jeor |
| Walking and activity energy | Compendium of Physical Activities METs, interpolated |
| Running energy | ACSM running equation (VO2 = 0.2·S + 0.9·S·grade + 3.5) |
| Stride length | Wilkie ratio, 0.415 × height, stretched by cadence |
| Max HR | Tanaka (208 − 0.7 × age) |
| HR zones | Karvonen, percentage of heart-rate reserve |
| Lean body mass | Boer |
| One-rep max | Epley, capped where the formula stops behaving |
| Load progression | Double progression |
| VO2max | Daniels-Gilbert VDOT, from a recent best 5 k |

Each is named in a comment at its own function, so the source is checkable from the
code rather than from this table.

## 10. If you are an AI assistant resuming this

- Read §4 before touching `MotionClassifier.kt`. The octave correction is subtle and
  easy to break; `MotionClassifierTest` is the guard that catches it.
- **Verify the APK you deliver.** A stale `app-release.apk` was once copied out before
  the build had finished, and the installed build was missing an entire feature. Check
  the dex before handing one over:
  ```bash
  unzip -p app/build/outputs/apk/release/app-release.apk 'classes*.dex' | grep -a "Box breathing"
  ```
- Claims about this phone need evidence from this phone. "Xiaomi, therefore MIUI" was
  wrong, and so was "you must have pressed stop". Check `dumpsys`, `adb shell getprop`,
  exit info, or the motion log before asserting anything about the device.
