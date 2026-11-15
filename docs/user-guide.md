# Configure a widget

Long-press your home screen, open **Widgets**, and find **Icon Navigator**. Choose **Grid** or **Thumb icon selector**. You can also open the app, choose a type for a new draft, save, then choose **Add widget**.

Old alternate Grid and left/right entries are retained for existing widgets and marked hidden. Some launchers may still list them.

## Basic settings

For Grid, set **Columns** and **Rows**, independently from the launcher's footprint. A single row creates a horizontal strip; a single column creates a vertical strip. Both range from 1–12. The default is a transparent 3 × 4 grid without app names.

For Thumb icon selector, choose **Thumb side** (Right or Left), **Number of layers** (1–6), and **Icons in layer N** (1–16). Layer 1 is nearest the lower corner. Defaults are **2 / 5 / 7 / 9**. Left/right are mirrored versions of the same widget; the side remains editable after placement.

Use **Thumb center height (%)** to match your grip: **0** keeps the bottom corner, **50** places the fan around mid-height, and **100** turns it around the top. The preview updates immediately and both hands remain mirrors.

Choose **Show icon labels** in Basic settings: **Follow defaults**, **Yes**, or **No**. Follow defaults uses the app's saved default, initially off. Yes/No override it for this widget. Previous defaults and label choices are preserved; saving a widget does not change the shared default.

Choose **Thumb reach** in Basic settings. **Fit widget** keeps the existing layout and shows **Layer spacing (%)**. **100** keeps the current density; lower values bring the rings closer together. The range is 25–150%, with changes visible immediately in the preview.

**Thumb reach → Set inner and outer reach** adds separate **Inner reach (%)** (1–99) and **Outer reach (%)** (2–100) controls in Basic settings. They set radial distances from the thumb center: the first and last populated layers sit at the selected endpoints, with circular arcs and proportional radial progression between them. The sliders cannot cross. **100%** is the largest circular reach fitting the available widget at the selected grip position. Outer reach is not stretched back to an edge after editing. With one populated layer, Inner sets its radius and Outer is retained for future layers.

New thumb widgets start with center height **17%**, custom inner/outer reach **34/95%**, requested icon size **47 dp**, and layer scales **150/121/105/100%**. Select your own apps; none are preselected. Existing widgets and saved drafts retain their choices. Older records without a reach mode keep **Fit widget** and their saved Layer spacing.

Custom reach intentionally leaves the space selected by its endpoints; both modes still tighten icon padding before uniformly shrinking artwork, without dropping apps or changing ordering. The same production geometry drives preview and rendering. Presets, saving and Activity recreation preserve mode and endpoints.

Thumb widgets fit the space you give them. Spacing between icons tightens first; when that is no longer enough, the icons shrink together. Layer-scale and individual-size ratios remain intact. All assigned apps remain visible in their arc order, without extra pages. Fit widget reserves no space for empty layers and adds no top/left inset. Custom reach leaves the space explicitly selected by its endpoints.

There is no 365 dp minimum. The new thumb entry starts at roughly **2 × 3 launcher cells** and supports smaller resizing. Actual dimensions and launcher-owned insets depend on your launcher. Resize on the home screen; the live preview uses the same available dimensions.

## Warnings and saving

Widgets never show pagination: there are no dots, arrows, page numbers, or swipe pages.

If you select more apps than configured positions, a warning stays visible in all settings views. Increase Grid rows/columns, increase thumb layer counts, or remove apps. Save becomes available when every selected app has a position. At least one app is required.

When a thumb widget needs smaller icons, settings explain that they shrink to fit. This notice does not block Save. Enlarge the widget for larger artwork and tap areas, or reduce the app count/layers. App names hide below 34 dp icon size so text does not overlap; the Show icon labels choice remains saved. Small nonempty widgets always retain their app icons.


Older configurations retain all apps. If one has too many assignments, the widget shows its first configured positions until you resolve the warning in settings; nothing is silently deleted.

## Select apps

Tap **+ Add apps**, search, check apps, and choose **Add selected**. Already selected packages are hidden, including alternate launcher activities. Removing an app makes it available again.

Hold and drag an icon in the actual preview to reorder it. The list supplies labeled earlier/later/remove controls. Tap an entry for replacement, move-to-position, label, individual size, or removal.

The three views—**Basic settings**, **Select apps**, and **Visual options**—are always available. Switching preserves edits, selection, and scroll positions. The live preview stays visible. Save from any view when the layout is valid.

## Visual options

Thumb widgets have independent **Layer N icon scale (%)** sliders directly in Visual options. Each scales that layer's requested default artwork. An individual icon size overrides the layer default. All icons shrink together only when space requires it, preserving their relative sizes. Advanced settings also has the default icon size and optional Exaggeration control.

Appearance presets retain handedness, center height, reach mode/endpoints, layer spacing, side-edge distance, layer scales, label policy, apps, artwork, ordering, and individual sizes. Transparent retains original icon colors; Clean minimal uses grayscale; Show icon labels remains controlled in Basic settings. Light/Dark supply adjustable pale/dark backgrounds, Forest adds green tones and 8 dp outer padding, and Dusk gradient supplies two colors. Every preset value is editable.

Choose Original colors, Grayscale only, or Single-color tint. Set Show icon labels in Basic settings. Expand **Background & overlays** for base color/opacity, wash, collage, gradient, or imported image overlays; overlays stay behind app targets. Expand **Advanced settings** for default icon size/opacity, icon padding/borders/artwork, text color/shadow, widget padding/borders, and corner radius. Expansion scrolls new controls into view while the preview stays visible.

**Default outer padding is 0 dp, with no additional app-owned layout margin.** Change **Padding around the widget** in Advanced settings. Icon spacing has separate beside/above/below controls. Existing saved padding remains unchanged during updates. Transparent and Clean minimal reset outer padding to zero; Forest deliberately starts at 8 dp. Launcher-owned insets remain outside the app's control.

Custom icon artwork keeps its aspect ratio. Imported background photos are center-cropped behind icons. Grayscale/tint affects foreground artwork and icon-derived overlays; manually chosen photos and gradients keep their own colors.

## Launch and update

Every foreground app icon on a saved widget opens its assigned activity directly. Decorative backgrounds have no actions. Use the top widget selector to edit another instance; each keeps independent settings.

Version 0.6.4 retains older widget bindings, counts, handedness, artwork, and assignments. Missing layer scales become 100%. Older label settings remain explicit overrides, and absent grip controls retain the previous corner position and spacing. Old page settings are ignored. Removed experimental shapes still migrate to Grid. An ordinary compatible update preserves data.

Open **Privacy & support** at the bottom of Visual options to read the offline policy or open the policy/support links in another app. Icon Navigator works without an account, ads, analytics, or internet permission.

Android 12+ is required. Thumb reach depends on hand size and placement; [the simulation study](thumb-layout-study.md) verifies geometry rather than comfort for every hand.
