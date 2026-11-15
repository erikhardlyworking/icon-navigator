# Thumb layout geometry study — Fit widget reference

The figures retain the 0.6.0 automatic-fit profile (40 dp, 100% layer scales, corner center, spacing 100%). The exporter sets these values explicitly so the study remains reproducible after 0.6.3 changed fresh thumb defaults to custom reach. These are geometry comparisons, not pictures of the current factory profile. The picker and Play artwork are separately exported from the current production defaults using `tools/PlayGeometry.java`.

The [figure](simulations/thumb-layouts.png) and [CSV](simulations/thumb-layouts.csv) are generated directly from production LayoutEngine geometry. They compare both hands with 2 apps at 80 × 120 dp, 7 at 120 × 160 dp, 23 at 160 × 240 dp, and 23 with labels at 400 × 500 dp. Configured layers remain **2 / 5 / 7 / 9** in every case. Empty positions reserve no footprint, and pagination is never used.

Dashed rectangles show actual fitted tap targets; circles show artwork bounds. Indices indicate assignment order. At 160 × 240 dp the default 23-app selection uses approximately **55%** of requested icon size (about 22 dp from a 40 dp default), while the two-app selection retains its requested 40 dp icons. These are count- and dimension-dependent results, not minimum-size rules.

## Construction and fitting

Unit geometry establishes symmetric angular anchors. One icon uses 45°; two use 22.5°/67.5°; larger rings span 0–90° evenly. Only populated layers contribute. The last populated layer uses its actual assigned count. Unit radii prevent coincident positions and preserve the arc pattern independently of dp sizes, labels, padding, and widget dimensions.

The occupied centers are normalized and fitted to available width/height using actual artwork/label bounds. An affine stretch fills each axis, with occupied bounds shifted to the explicit outer inset. This removes template-owned top/left whitespace. A lone icon starts at the outer top edge; grip height moves it vertically without reserving a complete fan. Left/right remain exact mirrors.

Requested artwork honors per-layer scales and individual overrides, up to 48 dp. Fitting squeezes requested icon spacing first. If zero spacing still cannot fit, it finds one common artwork multiplier that keeps all targets nonoverlapping and inside the widget. Relative layer/individual sizes are retained. App names hide below 34 dp so labels do not impose a size cutoff. There is no 32 dp icon floor, 48 dp target floor, or 365 dp widget minimum.

Small icons are a nonblocking settings notice. Assignment overflow remains a Save error, and all stored assignments are retained. A small nonempty widget keeps its app icons instead of replacing them with Open settings.

## Grip position, density, and edge distance

The [grip comparison](simulations/thumb-grips.png) and [grip CSV](simulations/thumb-grips.csv) use the same 23 apps at 400 × 500 dp for both hands: the default corner, center height 50%, center height 50% with layer spacing 50%, then a 32 dp side-edge distance. All cases retain every target and requested 40 dp artwork at these dimensions.

After unit-ring construction, layer spacing scales radial offsets from the innermost ring (100% is unchanged). Grip height rotates the fan by atan2(height, 100 − height), making 0% bottom, 50% middle, and 100% top. Points can therefore lie above and below a raised center. Normal occupied-bounds fitting then applies, preserving arc symmetry and mirrored hands. The explicit edge distance reserves space at the corresponding side before fitting; it is capped at 75% of usable width on narrow widgets.

The added **1,200 grip cases**, each checked for both hands, span four actual dimensions, center heights 0/25/50/75/100%, spacing 25/100/150%, insets 0/96 dp, 0/1/2/23/400 selected apps, and labels off/on. They check target bounds, no overlap, unchanged capacity/ordering, and exact mirroring. Separate tests prove that lower spacing reduces distances between shared radial rays and that a side inset moves the innermost layer inward.

## Independent inner and outer reach (0.6.2)

The existing figures/export commands above show **Fit widget**. The new **Set inner and outer reach** mode uses the same unit-ring angular anchors, but maps the first/last populated radii independently to percentages of the largest fitting circle around the selected grip. It preserves circles rather than stretching occupied bounds to fill both axes. A single populated ring uses Inner reach. Custom reach can intentionally leave space around the fan.

Geometry tests cover 360 range/grip/size/count/label combinations, each mirrored, plus 18 one-to-six-layer cases with up to 96 assigned apps. Independent endpoint tests verify that moving Inner leaves the outside ring unchanged and moving Outer leaves the inside ring unchanged when artwork fits. Other checks cover resizing angles/size ratios, bounds, collision fitting, identity/order, and individual sizes. Android renderer checks compare both modes against production preview geometry for all current/compatibility thumb providers. These establish geometry, not suitability for every hand.

## Coverage and limits

The unit sweep includes **1,440 actual-size geometries**: 1–6 layers, 1/2/5/9/16 icons per layer, labels off/on, icon padding 0/16/32 dp, outer padding 0/64 dp, and dimensions 40 × 40, 160 × 240, 500 × 180, and 64 × 400 dp. Tests never enlarge these dimensions to make the layout pass. They check target bounds, nonoverlap, hit testing, stable capacity, and positive artwork sizes.

Additional tests verify spacing tightens before artwork changes, sparse selections keep larger icons, unused layers do not reserve space, occupied top/outer edges have no added inset, resizing preserves the normalized arc pattern and relative sizes, shared angular rays, exact mirroring, and individual size precedence. Android tests check actual inflated targets after pixel rounding and saving a bound 23-icon widget at 80 × 120 dp.

This verifies geometry, not ergonomic comfort. Dense/small widgets have small tap targets. Launcher-owned insets and transparent space inside source artwork remain outside geometry-owned margins.

## Reproduce

From the project root with Java 17:

```sh
mkdir -p /tmp/icon-navigator-simulation
javac --release 17 -d /tmp/icon-navigator-simulation \
  app/src/main/java/dev/iconnavigator/WidgetConfig.java \
  app/src/main/java/dev/iconnavigator/LayoutEngine.java \
  tools/ThumbSimulation.java
java -cp /tmp/icon-navigator-simulation ThumbSimulation > docs/simulations/thumb-layouts.csv
MPLCONFIGDIR=/tmp/icon-navigator-matplotlib python3 tools/render-thumb-simulation.py
```

Plotting requires matplotlib. To regenerate the grip comparison as well:

```sh
java -cp /tmp/icon-navigator-simulation ThumbSimulation grips > docs/simulations/thumb-grips.csv
MPLCONFIGDIR=/tmp/icon-navigator-matplotlib python3 tools/render-thumb-simulation.py --grips
```

Python does not reimplement placement equations.
