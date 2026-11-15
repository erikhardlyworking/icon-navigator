# Validation — version 0.6.4

Verified on 2026-11-15 using JDK 17, Gradle 8.14, Android SDK 36, and isolated Android 15 / API 35 emulator **emulator-5580** (1080 × 1920, 420 dpi). Version code **12**.

| Check | Result |
| --- | --- |
| Debug APK and Android test APK builds | Passed |
| JVM unit tests | 29 passed |
| Android integration/UI tests | 28 passed |
| Android lint | 0 errors, 14 warnings |
| Two offered entries and six compatibility entries | Eight providers bound; only Grid/Thumb lack hide_from_picker |
| Inner/outer reach | Independent radial endpoints; ordered controls; preview, preset, recreation and Save persistence; schema 6 keeps Fit widget |
| Saved handedness and grip | Left/right, center height, layer spacing, and edge inset round-trip independently |
| Icon-label dropdown | Follow defaults / Yes / No preview and persist without changing the stored default |
| Pagination | No control views or navigation actions, including old overflow/page requests |
| Fresh thumb profile | Center 17%, custom inner/outer 34/95%, scales 150/121/105/100%, 2/5/7/9 positions, no apps preselected; saved profiles retained |
| Independent layer scales | Individual overrides win; automatic fitting preserves relative sizes |
| Overflow warnings | Visible in all editor views; Save disabled until all selected apps fit |
| Small thumb | All app targets remain clickable at 80 × 120 dp; Save succeeds with a nonblocking notice |
| Data migration | Old counts/hand/artwork retained; missing scales default to 100%; duplicate packages keep first entry |
| Zero outer padding | Retained; no app-owned root/container insets; Advanced settings remains editable |

## Geometry and rendering coverage

- **1,008 provider/count/size cases:** eight registered types × seven small/large/tall/wide dimensions × nine counts (0, 1, 8, 9, 12, 23, 31, 144, 400) × labels off/on. Actual thumb dimensions are used without any minimum-size enlargement. Tests check bounds, hit testing, ordering, no pagination, assignment retention, capacity, and Save validation. Additional tests verify spacing-before-size fitting, sparse selections, occupied edge bounds, and unchanged arc patterns/size ratios during resizing.
- **150 dense Grid cases:** 1 × 1, 8 × 1, 1 × 10, 3 × 4, 12 × 12; five dimensions including thin strips; padding 0/8/64 dp; labels off/on. Widgets retain full content width/height without a navigation strip or column.
- **1,440 actual-size thumb geometries:** 1–6 layers, 1/2/5/9/16 icons per layer, labels off/on, icon padding 0/16/32 dp, outer padding 0/64 dp, and 40 × 40 / 160 × 240 / 500 × 180 / 64 × 400 dp widgets. Targets stay inside actual bounds without overlap, and artwork shrinks below the old 32 dp floor when necessary. Empty layers reserve no footprint.
- **1,200 grip cases**, checked for both hands: center heights 0/25/50/75/100%, layer spacing 25/100/150%, side distances 0/96 dp, four dimensions, five app counts, labels off/on. All assignments remain ordered, targets stay in bounds without overlap, and hands mirror exactly. Tests verify lower spacing actually reduces radial distances and edge distance moves the innermost layer inward.
- Additional checks verify default shared angular anchors, exact mirroring, independent layer scaling, individual overrides, normalized arc patterns across unequal resizes, package uniqueness, reorder identity, and preset preservation of layer scales/counts/type/apps.
- **160 rendered provider/count/size cases:** eight providers × four shapes × five counts. Inflated RemoteViews retain app ordering, clickable targets, positive dimensions, and container bounds. All overlay modes keep backgrounds decorative. Rendering intentionally displays only configured capacity for legacy overflow data; all assignments remain stored.
- Provider binding, resize callbacks, and four responsive variants succeed. Imported rectangular artwork keeps its aspect ratio at native output density; tiles use FIT_CENTER and shared immutable bitmaps. A 400-assignment, 12 × 12 Grid rendered in four variants stays below one screen of bitmap memory and a 900 KB inline Binder payload.

The [thumb study](thumb-layout-study.md), [sizing figure](simulations/thumb-layouts.png), [grip comparison](simulations/thumb-grips.png), and their CSV exports use production geometry. The figure compares sparse and full selections on compact widgets, with no navigation strip or required minimum footprint.

Custom reach adds **360 range/grip/size/count/label cases**, each mirrored, and **18 one-to-six-layer cases** with up to 96 assigned apps and individual sizes. Endpoint tests verify first/last radii move independently instead of refilling the widget. Resizing preserves angles and size ratios. Bounds, collisions, ordering and assignment retention are checked. Renderer comparisons cover both modes for all three current/compatibility Thumb providers. The new UI test edits both endpoints, checks the real preview, applies a preset, recreates the Activity and saves; version 6 migration stays in Fit widget.

## Editor coverage

The ten UI tests exercise freely accessible views, app removal, Save validation, draft/view/preview restoration after Activity recreation, actual picker deduplication and re-availability after removal, real preview drag reorder, six editable presets, inline expansion/scrolling, advanced appearance controls, overflow warnings in every view, handedness, a fifth layer, and independent layer scales surviving recreation. A bound 23-icon widget at 80 × 120 dp uses its actual preview dimensions, shows a fitting notice, and can save from every view. Grip sliders change the real preview geometry and persist through recreation. The Show icon labels dropdown exposes exactly Follow defaults / Yes / No, previews each policy, retains saved selection through recreation, and never changes the stored default. The old Default app names switch is absent. Layer spacing is checked in Basic settings and absent from Visual options; it still updates the preview and survives recreation. Thumb spacing and Distance from side edge are absent from Visual options; stored legacy insets remain covered by persistence and geometry tests.

The new renderer comparison covers all three current/compatibility thumb providers at several raised grip heights and sizes. It permits two physical pixels for Android's complex-dimension quantization and integer margin conversion; geometry checks remain strict in dp. [Android RemoteViews margin conversion](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android15-release/core/java/android/widget/RemoteViews.java).

## Repeat

Use JDK 17 and set ANDROID_HOME to an SDK with Platform 36 / Build Tools 35.0.0:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
adb -s EMULATOR_SERIAL install -r app/build/outputs/apk/debug/app-debug.apk
adb -s EMULATOR_SERIAL install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s EMULATOR_SERIAL shell am instrument -w dev.iconnavigator.test/androidx.test.runner.AndroidJUnitRunner
```

Use an explicit isolated emulator serial, never the user's phone. Binding tests clean up their own host/widget IDs; UI tests restore the draft. Instrumentation force-stops the app on completion. Simulation reproduction is documented in the thumb study.

## Phone installation and limits

A Samsung **SM-S938B**, Android 17 / API 37, received **0.6.4 (12)** on **2026-11-15** through an authorized compatible `adb -s PHONE_SERIAL install -r`. Installation succeeded and package version verification confirmed 0.6.4 (12). App data, preferences and existing widget configurations were preserved. Automated suites and capture configurations stayed on the isolated emulator. This is installation verification, not a complete API 37 UI test pass.

There is no large fixed thumb minimum. The new entry requests 2 × 3 launcher cells with a 40 dp minimum resize hint; geometry fits all nonempty configurations to actual bounds. Small widgets can have small icons/targets, and labels hide below 34 dp. Actual comfort, Samsung placement/resizing behavior, Android 12, large fonts, TalkBack, other launchers, custom artwork providers, and large-collection performance on physical devices still need hands-on coverage. Launcher insets and whether the launcher honors hide_from_picker are host-controlled.

The 14 lint warnings concern Gradle version, API 33 Back manifest opt-in on min 31, custom-view construction/draw allocation, unused/obsolete icon resources, English programmatic strings and intentional physical left/right placement. There are no lint errors. A narrow GestureBackNavigation suppression keeps only the API 31/32 onBackPressed fallback; API 33+ uses the framework dispatcher. No production dependency was added.

The required four Gradle checks and release bundle build passed for 0.6.4. All **29 JVM** and **28 Android** tests passed. A previous 0.6.3 run observed an existing transient resize-callback assertion; its full repeat passed. Current integration coverage is on API 35, with target SDK 36; a full API 36/37 runtime suite and Play's pre-launch report remain release-stage checks.

## Screenshots

The current [Google Play materials](../publishing/google-play/README.md) contain eight real 0.6.4 captures at 1080 × 1920, 24-bit RGB PNG. The first three compare four ordinary shortcuts with 9/12/16 widget apps in an equal 2 × 2 Launcher3 footprint. They show optional labels, Light and transparent styles. Image 4 shows 13 stock-icon apps using the current factory thumb geometry, transparent on plain black, low/right with no ordinary shortcuts or dock. It uses a debug-only real Android AppWidgetHost at 250 × 300 dp. Forest/grayscale/border styling appears on a customized 12-app left selector. A Dark 8-app strip, actual icon-color choices, and independent layer-size sliders complete the set.

All sample artwork comes from original AOSP/Fossify app icons and Icon Navigator's packaged icon; no personal artwork or screenshots are used. Four hash-pinned official Fossify release APKs are emulator fixtures, never production dependencies. The opt-in capture runner refuses phones. Capture assertions check exact app counts, real hosts, compact bounds and low placement, with no ordinary shortcuts in image 4. PNG dimensions/modes, distinct checksums and archive integrity are checked; the ordinary test runner is restored afterward.

The 512 × 512 RGBA store icon matches the adaptive launcher icon. The 1024 × 500 RGB feature graphic is original vector artwork based on production thumb geometry. The public source and kit use an explicit allowlist excluding private signing material, local paths, device identifiers, build outputs and historical personal screenshots. The signed release AAB targets API 36; signature/ZIP verification and absence of debug capture/test classes are checked. The release checksum is in `publishing/google-play/release/release-manifest.json`.
