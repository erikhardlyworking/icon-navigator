# Google Play materials

Prepared listing assets for App Grid & Thumb Select Widget **0.6.6 (14)**, rebuilt **19 November 2026**. Start with [the release checklist](RELEASE-CHECKLIST.md), which maps every upload field to a file and explains the signed bundle, privacy policy, key backup and remaining Console choices. [App content answers](APP-CONTENT.md) cover privacy/data safety, app access, ads and rating facts. [Publication status](PUBLICATION.md) records the public source status.

Eight real screenshots show the two main uses—**fit more apps into the launcher's grid** and **keep frequent apps near your chosen thumb corner**—alongside appearance controls. Captured on a dedicated Android 15 / API 35 AOSP emulator. Upload the PNGs in `phone/`, in filename order; open `index.html` to compare them or use `icon-navigator-google-play-screenshots.zip`. `icon-navigator-google-play-kit.zip` includes screenshots, icon, feature graphic, listing copy, policy, declarations and the signed AAB. `icon-navigator-public-source.zip` is a separate source export without private keys or personal screenshots.

Five screenshots are **1440 × 2560** (9:16). Images **03, 04 and 05** use a tall **1440 × 3120** large-phone viewport (19.5:9). All are **24-bit RGB PNG without alpha**, captured at 480 dpi with gesture navigation. Both formats meet Google’s dimension/aspect limits; the five 9:16 images also supply its recommended standard portrait format. The kit also includes the **512 × 512 store icon** and **1024 × 500 feature graphic**. See [Google Play preview-asset requirements](https://support.google.com/googleplay/android-developer/answer/9866151). Tablet images and Play Console submission are outside this phone package.

The release includes the blank-tap fix: app icons launch their assigned activities, and the widget's settings are available through the launcher's long-press menu. Outer padding remains zero by default; existing saved spacing is retained.

| Image | What it demonstrates |
| --- | --- |
| 01 | **9 apps** with optional app names on a transparent 3 × 3 Grid, using **2 × 2 launcher cells** |
| 02 | **12 apps** in a 3 × 4 Grid using that same **2 × 2 footprint**, with a rounded **Light** background |
| 03 | **16 apps** in a 4 × 4 Grid using a **2 × 2 footprint** on a tall **1440 × 3120** phone |
| 04 | **13 apps** using the default **transparent right-thumb selector** on a tall **1440 × 3120** phone, low on plain black with **no ordinary shortcuts or dock icons**; actual Android widget host at **250 × 300 dp** |
| 05 | A customized **12-app** selector in the **lower left** of a tall **1440 × 3120** phone, with **grayscale icons**, Forest and a subtle border |
| 06 | **8 apps** in a **5 × 1** strip, with a **Dark** background, border, rounded corners and 80% icon opacity |
| 07 | The **Visual options** screen with the **Icon colors** choices open and a live gradient preview |
| 08 | All three independent layer-size controls and a live gradient preview |

The first three images put four ordinary shortcuts on the left and the compact Grid on the right, in equal launcher footprints. Normal shortcuts at the top provide further context: a widget's rows and columns are independent of the launcher's rows and columns. All apps within each widget are distinct. The same app can appear in a widget and as an ordinary launcher shortcut for this comparison.

The Thumb examples use only the lower part of the screen, rather than spreading icons over most of its height. They show selectable placement/handedness options; users choose the widget size, position, vertical center, and layer spacing to suit their own grip. The examples do not claim a universally reachable area for every hand or phone.

## Artwork and capture provenance

**No personal artwork, screenshots or installed-app inventory is included.** The chosen thumb geometry inspired the factory profile; all sample app assignments are independently selected emulator fixtures. Targets use original icons returned by PackageManager: **14 Fossify apps** with their original supported color-variant launcher aliases, Android Settings, and App Grid & Thumb Select Widget's packaged icon. Grayscale and opacity examples use the app's supported controls on that artwork. No imported/custom icon files are used. `icon-provenance.json` lists all 16 sample targets. Ordinary comparison shortcuts and the dock use those same modern packaged icons.

The sample apps are original [Fossify](https://github.com/FossifyOrg) releases: Phone, Messages, Camera, Gallery, Clock, Calendar, Contacts, File Manager, Calculator, Notes, Music Player, Voice Recorder, Paint and Keyboard. `sample-apps.json` pins each official release URL, verified SHA-256 hash, selected launcher component and original color variant. APKs are installed only on the dedicated emulator, cached under the system temporary directory, and excluded from this bundle and the production app.

These are actual `UiAutomation.takeScreenshot()` captures of the editor and Android widgets, using production RemoteViews. Images 1–3, 5 and 6 use native Launcher3 placement. Image 4 uses a debug-only real AppWidgetHost with a plain black background and removable-dock-style placement; that host is absent from the release AAB. No mock controls, captions, touch-reach overlays or altered widget geometry are composited into screenshots. Android demo mode sets 9:41/full battery; other launcher examples use an original indigo/teal gradient wallpaper with fine orbital lines. Lossless format conversion removes the fully opaque alpha channel; captures are not cropped or rescaled. Gallery captions are outside the images. The separate feature graphic is an original vector illustration, not a UI screenshot.

The right-hand example uses the fresh **2/5/7/9** profile with 13 selected apps, center height **17%**, inner/outer reach **34/95%**, requested artwork **47 dp**, layer scales **150/121/105/100%**, and zero outer padding/border. Only populated layers reserve space. The left-hand example uses a customized **2/4/6** arrangement, 34 dp artwork, Forest, Grayscale and a 1 dp border. Settings examples use 30 dp artwork, center height **35%**, automatic-fit spacing **70%**, a retained legacy side inset **8 dp**, Dusk gradient and scales **80/100/115%**. The redundant Thumb spacing section is removed from Visual options; saved side insets still render unchanged. The live preview uses actual bound dimensions. `manifest.json` records descriptions, dimensions, format and checksums.

## Reproduce

Use a **dedicated rootable AOSP Android 15 emulator with Launcher3**, JDK 17, SDK Platform 36 / Build Tools 35.0.0, and Python 3.10+/Pillow. Capture updates that emulator's sample widgets, comparison shortcuts, draft, wallpaper, display, animation, label-default and status-bar settings. It does not erase app data. Both the host script and opt-in runner reject physical devices.

```sh
export JAVA_HOME=/path/to/jdk17
export ANDROID_HOME=/path/to/android-sdk
python3 tools/capture-play-screenshots.py --serial emulator-5580
```

The script builds the debug app and opt-in runner, verifies/downloads the 14 pinned original sample APKs, enables their selected original color aliases, pins widgets through Launcher3, and positions them using emulator-only root. It creates real comparison shortcuts. Checks confirm expected widgets, icon counts, compact bounds and low placement; image 4 additionally rejects ordinary shortcuts. It validates PNGs and builds the gallery and screenshot ZIP. The ordinary AndroidJUnitRunner test APK is restored afterward. Capture code lives only in androidTest/debug source sets and is excluded from release builds.

To revalidate/package existing captures without connecting to any device:

```sh
python3 tools/capture-play-screenshots.py --package-only
```
