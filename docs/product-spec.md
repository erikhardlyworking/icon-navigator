# Product specification — version 0.6.4

## Current request, 2026-11-15

Offer **Grid** and **Thumb icon selector**. Grid rows and columns can form a strip. A thumb widget can switch between right and left in settings. Never show pagination on the widget. Fit spacing and artwork to the actual dimensions; settings warn about assignment overflow or small icons. This supersedes the seven-entry picker and adaptive thumb capacity in version 0.4.1.

## Grip and label controls

Basic settings has **Thumb center height (%)**, from 0 (bottom) through 50 (middle) to 100 (top). It turns the fan around the chosen vertical grip position while preserving assignment order and exact handedness mirroring. The live preview updates immediately; the saved widget uses the same geometry. Fresh thumb profiles start at 17%; older records without this field retain 0%.

Basic settings uses heading and labeled controls without explanatory paragraphs or a separate default-label switch. **Show icon labels** offers exactly **Follow defaults**, **Yes**, and **No** for both Grid and Thumb. Follow defaults uses the stored app default (initially off); Yes/No override it for this widget. Preview and Save preserve that policy without changing the app default. Existing defaults and explicit label choices remain intact. Appearance presets preserve the policy.

In **Thumb reach → Fit widget** mode, **Layer spacing (%)** remains in Basic settings, from 25 to 150. Lower values reduce radial separation while retaining the angular pattern; 100% is the neutral reference. Fresh custom profiles retain 67% for a later switch to Fit widget; older records retain their saved value (or legacy 100% when absent). Visual options no longer has the redundant Thumb spacing / side-edge-distance section. Saved side insets remain effective for compatibility. Default outer padding stays zero.

## Inner and outer reach

**Thumb reach → Set inner and outer reach** adds separate **Inner reach (%)** (1–99) and **Outer reach (%)** (2–100) controls in Basic settings. They set radial distances from the thumb center: the first and last populated layers sit at the selected endpoints, with circular arcs and proportional radial progression between them. The sliders cannot cross. **100%** is the largest circular reach fitting the available widget at the selected grip position. Outer reach is not stretched back to an edge after editing. With one populated layer, Inner sets its radius and Outer is retained for future layers.

Older records without a reach mode keep **Fit widget** and their saved Layer spacing. Existing custom profiles remain custom. Fresh thumb profiles start in custom mode, with inner/outer reach **34/95%**, center height **17%**, requested icon size **47 dp**, and layer scales **150/121/105/100%**. The default layer counts stay **2/5/7/9**, with no apps preselected. This reproduces the chosen grip geometry without copying anyone's app assignments or artwork. Grid defaults stay unchanged. Both sliders fit side by side beneath the pinned preview.

Custom reach intentionally leaves the space selected by its endpoints; both modes still tighten icon padding before uniformly shrinking artwork, without dropping apps or changing ordering. The same production geometry drives preview and rendering. Presets, saving and Activity recreation preserve mode and endpoints.

## Widget picker and settings

Grid starts at 3 columns × 4 rows in a requested 2 × 3 launcher footprint. Both dimensions are editable from 1–12. All Grid instances can become strips; separate strip/square/wide entries are unnecessary.

Thumb icon selector starts right-handed, with **2 / 5 / 7 / 9** positions in four layers, nearest the lower corner first. Choose **Thumb side** (Right/Left), **Number of layers** (1–6), and **Icons in layer N** (1–16) in Basic settings. Its picker image shows the actual 23-position arrangement. The requested footprint is 2 × 3 launcher cells; actual cell sizes and insets belong to the launcher. A small 40 dp minimum resize hint permits one-cell placement on typical hosts. There is no large fixed geometry minimum and no placeholder replacing icons at smaller sizes. The earlier fixed size restriction remains removed.


Old Grid square/wide/strip and left/right thumb components remain registered with Android's `hide_from_picker` feature, preserving existing bindings. The flag is a launcher hint; launchers decide whether to honor it. New drafts and pin requests offer only the two current entries. A bound provider fixes the family, while every thumb provider permits changing handedness.

## Editor

**Basic settings**, **Select apps**, and **Visual options** remain freely accessible. The live preview stays above the scrollable controls. Drafts, selected view, app edits, and per-view scroll positions survive navigation and Activity recreation. Save never depends on visiting all views.

Select apps supports add/remove/replace, labels, individual sizes, and drag reorder in the actual preview. Labeled move/remove controls and move-to-position remain alternatives. Each package appears once, including alternate launcher activities. Migration retains the first occurrence and its artwork/identity. Up to 400 assignments can be retained in a draft.

Saving requires at least one app and no more apps than configured positions. Overflow produces a visible settings warning in every view, with the selected count, capacity, and how many apps must be removed or accommodated. No assignments are silently deleted. Legacy overflow widgets display the first configured positions until corrected in settings.

## Adaptive thumb sizing in Fit widget mode

Configured capacity remains the sum of layer counts, independent of widget dimensions. The actual selection fills layers in order; the last populated layer uses only its assigned apps, and later empty layers reserve no footprint. Resizing keeps all displayed apps, assignment order, and the symmetric arc pattern. Left and right are exact mirrors.

Unit quarter-circle geometry establishes shared angular anchors. Complete rings move outward to avoid unit collisions, then grip rotation and layer-spacing adjustment are applied before occupied centers are normalized. The fan rotates by atan2(height, 100 − height), so outer endpoints follow the chosen vertical proportion. Radial offsets from the innermost ring are scaled by Layer spacing. Requested icon sizes honor layer scales and individual overrides, up to 48 dp. The occupied artwork/label bounds are fitted to the widget's available width and height, without hidden top/left inset or reserved space for absent apps. An explicitly configured side-edge distance reduces usable width on the selected side. A single icon starts at the outer top edge; grip height moves it vertically instead of reserving a whole fan.

Fitting first reduces the configurable spacing between icons while retaining requested artwork size. If zero spacing still cannot fit the selection, one common multiplier shrinks all icons and their target bounds. It preserves relative layer/individual sizes and never drops apps, changes capacity, creates pages, or forces a large minimum. Text labels hide below 34 dp rather than forcing larger targets; the user's label preference remains saved. Affine fitting retains the arc pattern while its center spacing adapts to available dimensions and actual tile sizes.

Settings show a nonblocking notice when automatic shrinking is needed, plus a label-visibility explanation where applicable. Saving a small widget is permitted as long as its selected app count fits capacity. Tiny widgets can have correspondingly small icons and tap areas. Explicit saved outer padding and borders remain effective; default outer padding remains zero.

There are no dots, arrows, page indicators, swipe pages, or navigation actions. Old pagination flags and page indices are ignored.

## Visual options

Keep presets, original/grayscale/tint colors, optional labels, default and individual icon sizes, opacity, borders, padding, custom artwork, text color/shadow, gradients, photos, wash, and collage. Overlays remain decorative and behind app targets.

Thumb **Layer N icon scale (%)** sliders independently scale each layer's default size. Individual icon overrides take priority. Automatic shrinking applies one common multiplier to preserve relative sizes; capacity and assignment order remain unchanged. The existing optional Exaggeration control provides additional bounded variation. Presets preserve handedness, counts, scales, apps, ordering, artwork, and individual sizes.

Default outer padding remains zero with no extra app-owned margins. Previously saved spacing is preserved. Advanced settings and Background & overlays expand inline and scroll new controls into view while keeping the preview visible.

## Rendering and migration

Every foreground icon directly launches its assigned activity. Source artwork preserves aspect ratio, density-aware output, filtered sampling, and a shared bitmap budget across responsive variants. No trampoline Activity or production dependency is introduced.

JSON schema 7 adds custom reach mode and ordered endpoints to schema 6's grip/label fields. Missing mode remains Fit widget, preserving existing geometry. Older counts, orientation, artwork, and labels are retained. Missing scales default to 100%. Removed experimental shapes still migrate to Grid. The original NavigatorWidget provider remains registered. Android 12+ is required. Work profiles, icon packs, export/import, localization, and broad accessibility/launcher testing remain future work.
