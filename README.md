# AR-Stride

An offline Android fitness app: step tracking that refuses to count while you ride,
GPS run recording on offline maps, a 30-day home training plan, yoga, posture
correction, and breathing and grounding work.

The profile is seeded for the person this build was made for — **male, 82 kg,
162.6 cm (5 ft 4 in)** — and every one of those values is editable in the app.

---

## Installing

Download the APK from the
[latest release](https://github.com/ABDURREHMAN629/AR-Stride/releases/latest), or build
it yourself (see below), in which case it comes out at
`app/build/outputs/apk/release/app-release.apk`.

Open the APK on the phone to install it. Android will ask you to allow installing from
unknown sources for whichever app is doing the opening (Files, Chrome, etc.) — that
is expected for an APK that did not come from the Play Store.

Or install over USB with debugging enabled:

```bash
"$LOCALAPPDATA/Android/Sdk/platform-tools/adb" install -r "app/build/outputs/apk/release/app-release.apk"
```

**Minimum Android 8.0 (API 26).** Built against SDK 37.

### Permissions it asks for

| Permission | Why | When |
|---|---|---|
| Physical activity | Runs the background pedometer and reads the activity classifier | On first launch |
| Notifications | The ongoing notification a foreground service legally requires | On first launch |
| Location (precise) | Traces your run route from GPS satellites | Only when you first record a run |

### About the internet permission

The app requests `INTERNET` for **exactly one thing**: downloading map tiles when you
tap *Download this area* on the Offline maps screen. Nothing else in the app opens a
socket.

There is no account, no server, and no analytics. Your steps, runs, workouts, weight
and routes are written to a local SQLite database and never transmitted. If you never
download a map, the app never touches the network at all.

> **Some phones block network access per app. You may need to allow it.**
>
> `INTERNET` is normally an install-time permission that Android grants automatically.
> LineageOS-based custom ROMs (and some vendors) turn it into a per-app switch, and
> **it can start switched off for sideloaded apps**. The symptom is every map tile failing with `UnknownHostException: Unable to
> resolve host` while every other app on the phone has working internet.
>
> Fix it in **Settings → Apps → AR-Stride** and allow network access on both Wi-Fi and
> mobile data (the exact wording varies by ROM). Or over adb:
>
> ```bash
> "$LOCALAPPDATA/Android/Sdk/platform-tools/adb" shell pm grant com.stride.fit android.permission.INTERNET
> ```
>
> This was hit on a Redmi 10A running Voltage OS, a LineageOS-based ROM on Android 17.
> The app now detects it and says so rather than failing silently.

---

## The step accuracy problem

A hardware pedometer counts impacts. It cannot tell a footfall from the rhythmic
buzz of a bicycle or a motorbike, which is why most step counters quietly inflate
your total on a ride.

AR-Stride credits a step only when four independent checks agree, plus a veto:

**1. Hardware step counter** (`TYPE_STEP_COUNTER`) — cheap, accurate about how many
impacts happened, survives the screen being off. Provides the raw count.

**2. Motion classifier** — the accelerometer is read in 8-second batches (batching
lets the CPU sleep between bursts) and decimated to 55 Hz. Phones ignore the
requested sampling period and deliver whatever the hardware prefers — the Redmi 10A
this was tested on returns ~102 Hz when asked for 50 — so decimating is what keeps
the analysis window a real four seconds instead of shrinking as the rate rises.
Every second, that 4-second window is scored on four axes:

| Signal | Walking | Cycling |
|---|---|---|
| Peak-to-peak amplitude | 6–15 m/s² | 1–4 m/s² |
| Rhythm | 1.3–2.7 Hz | 0.8–1.7 Hz (overlaps!) |
| Regularity (autocorrelation) | high, metronomic | moderate |
| Impact sharpness (kurtosis) | 2.5–8, spiky heel strikes | 1.5–2.5, smooth sine |

Rhythm alone is useless — pedalling cadence sits right inside the walking band. It is
amplitude and impulse *shape* that actually separate them.

**3. Activity recognition** (Google Play services) — a veto layer. A confident
bicycle or vehicle reading latches the gate shut for 90 seconds. This catches the one
case the accelerometer finds genuinely hard: a motorbike on a broken road, where the
jolts are strong and rhythmic enough to resemble footfalls.

**4. Hysteresis** — two consecutive walking windows to open the gate, three
non-walking windows to close it, so the count does not flicker every time you pause.

Steps are held for 12 seconds before being credited, so the batched accelerometer
data covering them has definitely arrived. That is why the counter lags slightly
behind your feet.

**You can watch all of this happen.** *You → Step accuracy* shows the live
classification, the raw feature values, and a running tally of steps rejected as
riding. Take the phone on a ride with that screen open.

Three sensitivity levels (Lenient / Balanced / Strict) shift the amplitude
thresholds if the defaults over- or under-count for you.

---

## What is in it

### Steps
Ring against a daily goal, hourly breakdown, weekly chart. The goal moves itself:
once your 7-day median clears it, it rises by 500; if you fall well short, it drops.
Distance uses **your** stride (0.415 × height, stretched by cadence) rather than a
generic 0.75 m per step.

**Comparison** is its own card, because a step count on its own is a number without
meaning — the only thing it can usefully tell you is which direction you are going.
Today against yesterday, this week's seven bars against last week's, totals, daily
averages, distance, and your best day.

### Running
GPS-only positioning, so recording works with no data connection. Raw GPS wanders
several metres even standing still, so fixes go through a Kalman filter weighted by
reported accuracy, and then:

- fixes worse than 25 m accuracy are dropped
- movement below 2.5 m is treated as jitter, not progress
- implied speeds above 9 m/s are dropped as glitches
- altitude is smoothed before any climb is counted
- auto-pause after 12 s of no movement

Per-kilometre splits, elevation gain, and the route drawn on a real map.

### Offline maps
Download the streets around you once, over wifi. After that the map renders from
tiles on your phone and never touches the network again — your route shows over real
streets with no signal and no data. New city? Download that area once when you
arrive.

Pick a radius (2–15 km) and a detail level (roads / streets / buildings); the screen
shows the exact tile count and approximate size **before** you commit, because a
silent multi-megabyte download on mobile data is not a nice surprise. Deleting an
area removes only the tiles no other saved area still needs.

Tiles come from OpenStreetMap. Their usage policy asks for a real identifying
User-Agent, no heavy parallelism and no bulk downloading, so the downloader is
deliberately polite: one request at a time, a 120 ms gap between each, tiles already
on disk never refetched, and a hard 2,500-tile ceiling per area. Please keep areas
modest. For heavier use, point `TileDownloader.urlTemplate` at a provider you hold a
key for.

Runs recorded outside any downloaded area are never lost — the map just draws the
bare route trace instead of streets.

### Strength
Two full body programmes, three days a week:

- **Full Body Foundation** — dumbbells, a bench, a band
- **Bodyweight and Band** — nothing but a band and a chair

Full body beats a split here: every session hits every pattern, so a missed day
costs less. Both carry a deliberate **2:1 pull-to-push ratio**, which works against
the rounded-shoulder posture the app is also trying to fix.

45 exercises, each with setup steps, coaching cues, common mistakes, tempo and
breathing. Set logging with weight, reps and RPE, a rest timer that buzzes, and
**double progression**: stay at the same load until every set reaches the top of the
rep range, then add the smallest useful jump and start again at the bottom.

### The 30-day plan
A month of home training on the Train tab. Twelve strength sessions, nine yoga flows,
five posture days and four rest days, in an order that lets each one work — strength
days never fall back to back, and the days between are what make the hard days
possible.

Everything is bodyweight or a resistance band. Nothing jumps: at 82 kg and starting
out, plyometrics load the knees far more than they give back, so the conditioning is
step jacks and mountain climbers rather than burpees.

| Week | Progression |
|---|---|
| 1 | Learn the patterns, two rounds |
| 2 | Same movements, three rounds |
| 3 | Harder variations — full push-ups, Bulgarian split squats |
| 4 | Four rounds, then retest |

Day 1 records four benchmarks (max push-ups, max squats in 60 s, max plank, max wall
sit). **Day 29 repeats them exactly**, so the month ends with a measured result
rather than a feeling.

Day 1 is whenever you press start, not a fixed calendar — miss a day and the plan
shifts rather than breaking a streak. Yoga and posture days hand straight over to the
guided player.

### Yoga
Eighteen poses with Sanskrit names, plain-language instructions and the honest easier
option for each, plus five flows:

- **Sun Salutation A** — the classic warm-up
- **Morning Flow** (15 min) — spine, hips, shoulders, hamstrings
- **Evening Wind-Down** (12 min) — floor-based, no standing, helps you sleep
- **Yoga for Runners** — everything running tightens
- **Yoga for Back and Posture** — undo a day at a desk

### Posture
Five guided routines targeting the two patterns that cause most desk-related
complaints:

- **Upper-crossed** — head forward, shoulders rounded. Tight chest, upper traps and
  levator; weak deep neck flexors, lower traps and serratus.
- **Lower-crossed** — pelvis tipped forward, lower back over-arched. Tight hip
  flexors and erectors; weak glutes and deep abs.

Routines: Desk Reset (5 min), Upper Body Fix (12 min), Lower Body Fix (12 min), Full
Reset (18 min), Morning Mobility (8 min). Each **pairs a release with the
strengthening drill that holds the new position** — stretching the tight side alone
rarely sticks. The guided player shows cues while you are actually doing the drill,
which is the only moment they are any use.

### Mind
Reached from the Posture tab. Everything logged here feeds the same daily streak as
posture work.

**Breathing** — an animated pacer that grows through the inhale, holds its size
through a hold, and shrinks through the exhale, so you can follow it with your eyes
half shut and never read the countdown. A short buzz marks every phase change, so you
can close them completely. Each pattern carries its own posture setup.

| Pattern | Shape | For |
|---|---|---|
| **Box Breathing** | 4 in, 4 hold, 4 out, 4 hold | A racing mind. Four equal sides, nothing to think about but the count |
| Coherent | 6 in, 6 out | ~5 breaths/min, where heart rate variability peaks. Easiest to hold for 10 min |
| 4-7-8 | 4 in, 7 hold, 8 out | Longest exhale here, strongest downshift. Use it in bed |
| Physiological Sigh | double inhale, long sigh | Fastest reset from an acute stress spike |

The mechanism isn't mystical: a long controlled exhale raises vagal tone, which slows
the heart and pulls you out of a sympathetic stress response. Every pattern here makes
the out-breath at least as long as the in-breath.

**5-4-3-2-1 Grounding** — ten minutes, **two minutes per sense**, not the usual rushed
run-through (the speed is what makes most people say it doesn't work):

| | Sense | |
|---|---|---|
| 5 | See | Describe them, don't just name them — one thing you've never noticed |
| 4 | Touch | Four textures reachable without standing up |
| 3 | Hear | **Eyes closed.** Obvious sound first, then listen underneath it |
| 2 | Feel | Two faint sensations you can tell apart — air, temperature, heartbeat |
| 1 | Taste | Whatever is already in your mouth |

Tap a numbered dot as you find each thing, so the count is yours rather than the
app's. It works by giving attention a job: anxiety lives in the past and the future,
and naming what is physically around you forces attention into the present, where
neither exists.

**Presence of mind** — five attention drills: Breath Counting, Body Scan, the STOP
practice (60 seconds, anywhere), Single-Task Focus, and Mindful Walking. Each states
plainly why it works.

### You
BMI and healthy weight range, Mifflin-St Jeor resting burn, energy targets, protein
and water targets, Karvonen heart rate zones from a Tanaka max, weight trend chart,
and all the goal and profile settings.

---

## The maths

All in [`health/HealthMath.kt`](app/src/main/java/com/stride/fit/health/HealthMath.kt),
with references in the comments.

| Quantity | Method |
|---|---|
| Resting burn | Mifflin-St Jeor |
| Walking energy | Compendium of Physical Activities METs, interpolated between anchor speeds |
| Running energy | ACSM equation, `VO2 = 0.2·S + 0.9·S·grade + 3.5` |
| Walking stride | Wilkie ratio, 0.415 × height, cadence-adjusted |
| Max heart rate | Tanaka, `208 − 0.7 × age` (beats `220 − age` on validation) |
| Zones | Karvonen, using heart rate reserve |
| Lean mass | Boer |
| One-rep max | Epley, capped past 12 reps |

Energy figures subtract the resting cost, so what you see is *additional* burn — which
is what a calorie target actually needs.

These are estimates, not measurements. Use them as a starting point and adjust from
what the scale actually does over two or three weeks.

---

## Building from source

Needs the Android SDK (platform 37) and a JDK. Android Studio's bundled JDK works:

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
./gradlew assembleRelease
```

Release signing is read from `keystore.properties` at the project root, which is **not**
in version control (neither is `keystore/`). Without it the release build still compiles,
it just comes out unsigned. To sign your own builds:

```bash
cp keystore.properties.example keystore.properties
keytool -genkeypair -v -keystore keystore/my-release.jks -alias myalias \
  -keyalg RSA -keysize 2048 -validity 10000
# then edit keystore.properties with the path, alias and passwords you chose
```

Android only installs an update over an existing copy when both are signed by the same
key, so keep whichever keystore you build with — losing it means uninstalling before the
next update, which wipes the recorded history with it.

Run the unit tests with:

```bash
./gradlew :app:testDebugUnitTest
```

(AGP 9 does not generate a `testReleaseUnitTest` task, so use the debug one.)

### Layout

```
sensors/     MotionClassifier, PedestrianGate, StepService   <- the accuracy engine
run/         RunMath (Kalman + route accumulator), RunService
map/         TileMath, TileStore, TileDownloader, RouteMap    <- offline maps
training/    Exercises, Posture, Yoga, Programs, Challenge30, Progression
health/      HealthMath
data/        Room entities, DAOs, repository, DataStore settings
ui/          Compose screens, components, theme, view models
```

Stack: Kotlin, Jetpack Compose (Material 3), Room, DataStore, AGP 9.4 with built-in
Kotlin support.

---

## Tested on

Xiaomi Redmi 10A hardware (`fog`, 220333QAG) running **Voltage OS** (`voltage_fog-user`),
a LineageOS-based custom ROM on Android 17 / SDK 37, arm64. Not stock MIUI or HyperOS. Hardware Qualcomm
pedometer and step detector both present, so the accelerometer fallback is not in use.

Verified on device: launches clean, foreground service holds with the `health` type,
ongoing notification posts, Play services is installed so the activity-recognition
veto is active, sensor pipeline delivers at a decimated 50 Hz, and the classifier
correctly reports `Still` with zero rhythm and zero regularity on a stationary desk.

Not verified: the walking and riding classification itself. That needs you to
actually walk and actually ride — see below.

## Known limits

- **Battery.** Continuous 50 Hz accelerometer sampling is what makes the ride
  filtering possible, and it costs a few percent a day. Batching keeps it modest, but
  it is not free.
- **Play services.** The activity-recognition veto needs it. Without it the
  accelerometer classifier still works alone, just with slightly less protection
  against motorbikes on rough roads.
- **Aggressive battery managers.** Some manufacturers (Xiaomi, Oppo, Vivo, Samsung)
  kill background services regardless of foreground status. If counting stops
  overnight: on stock Xiaomi firmware turn on **Autostart** for AR-Stride; on
  LineageOS-based ROMs there is no Autostart setting, so set **App battery usage →
  Unrestricted** instead.
- **No heart rate.** There is no HR sensor input, so zones are advisory numbers to
  use with a chest strap or watch, not something the app measures.

---

## Contact

Built by **Abdur Rehman (AR)**.

- Email: **gabdurehman493@gmail.com**
- GitHub: [ABDURREHMAN629](https://github.com/ABDURREHMAN629)

Questions, bug reports, or a motion log from a walk that got misread are all welcome by
email. If you are reporting a miscount, attach `motion-log.csv` — see
[HANDOVER.md](HANDOVER.md) for how to pull it off the phone.
