# App Grid & Thumb Select Widget

An Android widget for launching apps from a compact grid or comfortable thumb arcs. Android 12 or newer.

Choose **Grid** or **Thumb icon selector** from your launcher's widget picker. Configure Grid rows/columns to make any grid or strip. Thumb starts with 2 / 5 / 7 / 9 icons and offers a left/right switch, 1–6 layers, and independent layer scales. Thumb starts around 2 × 3 launcher cells. Spacing tightens before icons shrink, keeping all assigned apps in their arc order. Empty layers reserve no footprint, with no added top/left inset or large fixed minimum size.

The editor has three freely accessible views, with a live preview always visible:

1. **Basic settings** — grid rows/columns, or thumb handedness/center height, reach mode, layer spacing/counts, and Show icon labels (Follow defaults / Yes / No).
2. **Select apps** — add, remove, replace, and reorder apps. Each app can be added once. Hold and drag in the actual widget preview, or use the labeled list controls.
3. **Visual options** — presets, grayscale/tint, icon and per-layer sizes, padding/borders, gradients, and image overlays.

Grip adjustments update the live preview and saved widget together. **Thumb reach → Set inner and outer reach** chooses first/last-layer distances from the thumb center independently. Existing widgets keep Fit widget and their saved Layer spacing. Show icon labels follows the saved default or explicitly enables/disables labels for one widget. Existing choices are retained.

The default background is transparent with **zero outer padding**. Presets are editable; Advanced settings expands and scrolls to reveal extra controls, including icon opacity, custom text color, and label shadows.

Save from any view once at least one app is selected and all selected apps fit the configured positions. Settings warn about overflow and explain automatic icon shrinking. Every foreground widget icon directly launches its assigned app. Widgets never show pagination. Thumb layouts fit spacing and icon sizes to the actual bounds without dropping icons.

[User guide](docs/user-guide.md) · [Product specification](docs/product-spec.md) · [Architecture](docs/architecture.md) · [Thumb simulations](docs/thumb-layout-study.md) · [Validation](docs/testing.md)

## Build and install

Use JDK 17 and Android SDK Platform 36 (Build Tools 35.0.0), with `ANDROID_HOME` configured:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
adb -s YOUR_DEVICE_SERIAL install -r app/build/outputs/apk/debug/app-debug.apk
```

Version **0.6.5**, version code **13**, target SDK **36**. APK: `app/build/outputs/apk/debug/app-debug.apk`. The wrapper verifies its Gradle distribution checksum.

Fresh thumb widgets start with custom inner/outer reach **34/95%**, center height **17%**, requested icon size **47 dp**, and layer scales **150/121/105/100%**. Pick your own apps; none are preselected. Existing widgets and saved drafts retain their settings. Grid keeps its transparent, label-free 3 × 4 default.

[Privacy policy](PRIVACY.md) · [Google Play release materials](publishing/google-play/RELEASE-CHECKLIST.md) · [Screenshot gallery](publishing/google-play/index.html)

The former experimental shapes and mandatory three-step wizard have been removed. Existing app selections migrate; duplicate app entries keep their first occurrence. Existing legacy widget instances become Grid widgets. Old alternate providers remain registered but are hidden from the picker where supported. New widgets use Grid or Thumb icon selector.
