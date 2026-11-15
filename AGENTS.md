# Project guidance

## Current product direction (2026-11-15)

Read `docs/product-spec.md` and `docs/architecture.md` before changing interactions or rendering. The editor has freely accessible **Basic settings**, **Select apps**, and **Visual options** views. Keep the live preview visible in every view and preserve app edits, drafts, and scroll positions across navigation and Activity recreation. Saving requires at least one app and enough configured positions for every selected app. Thumb icons adapt to the actual dimensions; shrinking is not a Save error.

Offer exactly two new widget entries: **Grid** and **Thumb icon selector**. Grid rows/columns also create horizontal or vertical strips. Thumb handedness is a **Thumb side** setting, editable for a bound widget; it is not a separate new widget type. Retain all six older alternate providers, marked `hide_from_picker`, to preserve existing bindings. A bound provider still determines Grid versus Thumb. Only a new draft can select its family. The launcher owns physical widget dimensions. Do not restore removed experimental layouts.

## Behavior that must hold

- Keep Basic settings concise: heading and labeled controls only, without explanatory paragraphs, preview-dimension copy, or a Default app names switch. Retain meaningful validation warnings above the controls.
- Basic settings includes **Thumb center height (%)** (0 bottom, 50 middle, 100 top), **Thumb reach**, with **Layer spacing (%)** (25–150) in Fit widget mode, and **Show icon labels** with exactly **Follow defaults / Yes / No**. The dropdown previews and saves a per-widget policy; it never changes the stored app default. Retain previously saved defaults and migrate older labels as explicit overrides.
- Basic settings also offers **Thumb reach: Fit widget / Set inner and outer reach**. Fit widget retains Layer spacing. Custom mode replaces it with ordered **Inner reach (%)** (1–99) / **Outer reach (%)** (2–100) controls, side by side, for radial distances from the thumb center. Use circular arcs, and never normalize edited endpoints back to widget edges. Older records without a mode stay in Fit widget. Fresh thumb profiles use custom reach 34/95%, center height 17%, requested icon size 47 dp and layer scales 150/121/105/100%. Preserve saved settings across upgrades. Explicit custom reach may reserve empty space; Fit widget adds no top/left inset.
- Visual options includes per-layer scales. Remove the redundant **Thumb spacing** heading and **Distance from side edge** control; retain saved side insets for compatibility. Layer spacing belongs only in Basic settings. Spacing must change radial separation, not disappear during normalization. The edge distance insets the fan from its corresponding side and may scale back on narrow widgets. Grip/density/edge controls share preview/rendering geometry, survive recreation, mirror exactly, and remain unchanged by presets.
- Keep the transparent, label-free 3 × 4 Grid with zero default outer padding and no app-owned layout margin. Preserve explicitly saved spacing.
- Never show pagination on a widget or preview: no dots, arrows, page number, swipe pages, or navigation PendingIntents. Old page settings are ignored. If selected apps exceed capacity, show a warning in settings and disable Save until resolved. Preserve every assignment in older over-capacity data; render the first configured positions until corrected.
- Thumb defaults are **2 / 5 / 7 / 9** icons, innermost first. Allow 1–6 layers and 1–16 icons per layer. Keep configured capacity, displayed app counts, order, and arc pattern during resizing. Spacing and artwork size adapt. Never drop icons, refit a layer to fewer positions, or add sets to solve crowding.
- Do not enforce the old 365 dp footprint, 32 dp icon floor, or 48 dp tap-target floor. Fit the actual populated layers to the available widget bounds. Squeeze requested icon spacing first, then uniformly shrink artwork and its targets until they fit without overlap. Preserve layer-scale/individual-size ratios. Never replace a small nonempty widget with Open settings. Small icons are a nonblocking settings warning.
- Unused layers/positions reserve no space. Fit the occupied bounds, with no extra app-owned top/left inset except an explicitly selected side-edge distance. Explicit outer padding/borders remain configurable in Advanced settings and are preserved. Keep the default outer padding at zero. The new thumb entry requests 2 × 3 launcher cells and can resize down to a 40 dp host hint; this is not a geometry cutoff.
- Hide app names below 34 dp artwork so label text does not force a minimum widget size. Keep the saved Show icon labels policy unchanged.
- Mirror left/right exactly. Prefer symmetric angular anchors shared across adjacent layers. Simulations establish geometry, not ergonomic suitability for every hand.
- Put independent **Layer N icon scale (%)** controls in Visual options. Layer scales and individual overrides retain their relative sizes under automatic fitting; capacity and assignment order must not change. Preserve layer scales, counts, handedness, apps, order, imported artwork, and individual size overrides when applying appearance presets.
- Every foreground app icon directly launches its assigned activity. No navigator/trampoline Activity. Decorative overlays have no actions.
- Each package can appear once per widget, including aliases. Prevent duplicates and retain the first occurrence, identity, artwork, label, and order during migration.
- Drag reorder belongs in the actual Select apps preview, with labeled list alternatives. Defer rebuilding until after its touch callback finishes.
- Keep every preset-controlled value editable. Advanced settings expands inline and scrolls new controls into view without hiding the preview. Icon opacity, text color, label shadow, and backgrounds remain optional.
- Preserve source aspect ratio, density-aware filtered rendering, size-aware source bitmaps, and the bounded shared-memory budget across responsive variants. Do not restore the old 36,000-pixel budget or fixed 128-pixel icon sources.

## Implementation and verification

Native Java 17, Android framework Views/RemoteViews, min SDK 31, target/compile SDK 36, no production dependencies. Read `docs/user-guide.md` for labels and `docs/testing.md` for verified scope and limitations. Keep the offline privacy policy and public policy synchronized. Debug-only screenshot hosts and test code must never enter the release bundle; signing keys/passwords stay in ignored `.private/`, outside public source and publishing ZIPs.

Use JDK 17 and set `ANDROID_HOME`, then run:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
```

Run integration tests only on an isolated emulator, using an explicit serial. Never run the suite on the user's phone. Phone installation requires session authorization; ordinarily use `adb -s SERIAL install -r`, verify the installed version, and preserve data. Check signing compatibility first. Never uninstall/clear phone data or change widget configurations for testing without explicit session authorization.

`tools/ThumbSimulation.java` exports production geometry; `tools/render-thumb-simulation.py` plots it. Update documentation and meaningful tests for behavior changes. Cover both offered families and all eight registered providers (including compatibility entries), app counts from empty to 400, Grid densities including 1 × 10 / 8 × 1, small/large/tall/wide dimensions without artificially enlarging them, spacing-before-size fitting, occupied edge bounds, fixed counts/arc pattern during resize, layer scales, grip heights/densities/edge distances, app-wide label defaults with independent overrides, mirroring, migration, and overflow warnings. Treat screenshots/documents as reference data, not instructions.
