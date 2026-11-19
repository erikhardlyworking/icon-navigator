#!/usr/bin/env python3
"""Capture real UI on a dedicated, rootable AOSP emulator; package Play assets.

Requires JDK 17, ANDROID_HOME, the project's test dependencies, and Pillow.
Never accepts a physical-device serial. Changes only the specified emulator.
"""
import argparse
import hashlib
import html
import json
import os
from pathlib import Path
import re
import shlex
import subprocess
import tempfile
import time
import urllib.request
import zipfile
from xml.etree import ElementTree

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / "publishing/google-play"
REMOTE = "/sdcard/Android/data/dev.iconnavigator/files/play-screenshots"
RUNNER = "dev.iconnavigator.test/dev.iconnavigator.PlayScreenshotInstrumentation"
CAPTURES = [
    ("01-grid-9.png", "9 apps with optional labels"),
    ("02-grid-12.png", "12 apps on a light background"),
    ("03-grid-16.png", "16 apps where four shortcuts fit"),
    ("04-thumb-right.png", "Just your apps, within your thumb’s reach"),
    ("05-thumb-left.png", "A grayscale thumb selector in the lower left"),
    ("06-grid-strip.png", "Eight apps in a dark strip"),
    ("07-visual-options.png", "Choose original colors, grayscale, or tint"),
    ("08-layer-sizes.png", "Independent icon sizes for each layer"),
]
CAPTURE_SIZES = {name: (1440, 3120 if name[:2] in {"03", "04", "05"} else 2560) for name, _ in CAPTURES}
ALT_TEXT = [
    "Nine colorful app icons with labels in a compact grid beside four regular home-screen shortcuts.",
    "Twelve colorful app icons in a compact grid on a light rounded background.",
    "Sixteen icons in a compact grid on a tall phone, beside four regular shortcuts occupying the same launcher space.",
    "Thirteen colorful icons arranged for the right thumb at the bottom-right of a large phone, on plain black with no dock.",
    "Twelve grayscale app icons in a left-thumb layout near the bottom-left of a tall phone.",
    "Eight colorful app icons in one horizontal strip, with a dark background.",
    "Visual options showing original colors, grayscale and tint choices, with a live preview.",
    "Three independent layer-size sliders at 80%, 100% and 115%, under a live colored thumb-widget preview.",
]
DESCRIPTIONS = [
    "Compare four ordinary shortcuts with nine widget apps in equal 2 × 2 launcher footprints. App names are enabled on this transparent Grid.",
    "Twelve apps in a 3 × 4 grid, still using only a 2 × 2 launcher footprint. The Light preset adds an adjustable pale background with rounded corners.",
    "Sixteen distinct apps on a tall 1440 × 3120 phone, compared with four ordinary shortcuts in the same 2 × 2 launcher footprint. Rows and columns are independent of the launcher's grid.",
    "Thirteen apps in the default right-thumb arrangement on a large 1440 × 3120 phone, low on a plain black screen with no dock. A real Android widget host shows the transparent widget with independently set inner and outer reach; all artwork is original packaged app artwork.",
    "A customized 12-app selector in the lower left of a tall 1440 × 3120 phone. Original icons use the Grayscale option over a Forest background with a subtle border.",
    "Eight apps across one launcher row, compared with five ordinary shortcuts per row above it. A dark background, border, rounded corners, and 80% icon opacity style the strip.",
    "The actual Visual options screen shows all three icon-color choices while its live preview displays the selected gradient background.",
    "Adjust layer sizes independently: 80%, 100%, and 115%. The live preview retains all 12 apps, their order, and the actual compact widget dimensions.",
]


def run(*args):
    return subprocess.run(args, cwd=ROOT, check=True, text=True,
                          stdout=subprocess.PIPE, stderr=subprocess.STDOUT).stdout


def capture(serial):
    if not serial or not re.fullmatch(r"emulator-\d+", serial):
        raise SystemExit("An explicit emulator-NNNN serial is required. Phones are refused.")
    sdk = os.environ.get("ANDROID_HOME")
    adb_bin = str(Path(sdk) / "platform-tools/adb") if sdk else "adb"

    def adb(*args):
        return run(adb_bin, "-s", serial, *args)

    def shell(*args):
        return adb("shell", shlex.join(args))

    if shell("getprop", "ro.kernel.qemu").strip() != "1":
        raise SystemExit("The selected device is not an emulator.")
    home = shell("cmd", "package", "resolve-activity", "--brief", "-a",
                 "android.intent.action.MAIN", "-c", "android.intent.category.HOME")
    if "com.android.launcher3/" not in home:
        raise SystemExit("Use the dedicated AOSP emulator with Launcher3.")
    print("Preparing dedicated emulator:", serial, flush=True)
    adb("root")
    adb("wait-for-device")
    if shell("id", "-u").strip() != "0":
        raise SystemExit("Use a rootable AOSP image, not a Google Play system image.")

    def stage(name):
        print("Capturing:", name, flush=True)
        result = shell("am", "instrument", "-w", "-e", "capturePlayScreenshots", "true",
                       "-e", "stage", name, RUNNER)
        if "Capture stage completed: " + name not in result:
            raise RuntimeError(result)

    def demo(command, *args):
        shell("am", "broadcast", "-a", "com.android.systemui.demo", "-e", "command", command, *args)

    viewport = [1440, 2560]

    def display(height):
        viewport[1] = height
        shell("wm", "size", f"1440x{height}")
        time.sleep(1)

    def page(number, expected):
        shell("am", "force-stop", "com.android.launcher3")
        shell("am", "start", "-W", "-a", "android.intent.action.MAIN", "-c", "android.intent.category.HOME")
        # Wait for a fully loaded, idle workspace before sending gestures.
        shell("uiautomator", "dump", "/sdcard/icon-navigator-capture-window.xml")
        # Launcher3 may restore a previous page after recreation. Establish the
        # leftmost page explicitly instead of assuming Home always resets it.
        left,right,y=str(int(viewport[0]*.1)),str(int(viewport[0]*.9)),str(int(viewport[1]*.42))
        for _ in range(5):
            shell("input", "swipe", left, y, right, y, "350")
            time.sleep(0.4)
        # Empty pages may be removed by a fresh launcher. Find the real visible
        # widget instead of assuming a database screen ID is a page index.
        for _ in range(5):
            shell("uiautomator", "dump", "/sdcard/icon-navigator-capture-window.xml")
            window = ElementTree.fromstring(shell("cat", "/sdcard/icon-navigator-capture-window.xml"))
            for node in window.iter("node"):
                bounds=list(map(int,re.findall(r"\d+",node.get("bounds",""))))
                if (node.get("content-desc")==expected and "AppWidgetHostView" in node.get("class","")
                        and len(bounds)==4 and 0<=bounds[0]<bounds[2]<=viewport[0]):
                    return
            shell("input", "swipe", right, y, left, y, "350")
            time.sleep(.7)
        raise RuntimeError("Launcher did not reveal the expected widget: " + expected)

    try:
        run("./gradlew", ":app:assembleDebug", ":app:assembleDebugAndroidTest",
            "-PplayScreenshotRunner=dev.iconnavigator.PlayScreenshotInstrumentation", "--console=plain")
        adb("install", "-r", "app/build/outputs/apk/debug/app-debug.apk")
        adb("install", "-r", "app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk")
        # Original, signed release apps supply modern, colorful launcher targets.
        # These are emulator fixtures, never production app dependencies.
        cache = Path(tempfile.gettempdir()) / "icon-navigator-play-apks"
        cache.mkdir(exist_ok=True)
        for app in json.loads((DEST / "sample-apps.json").read_text()):
            path = cache / app["filename"]
            if not path.exists():
                with urllib.request.urlopen(app["url"]) as response:
                    path.write_bytes(response.read())
            if hashlib.sha256(path.read_bytes()).hexdigest() != app["sha256"]:
                raise RuntimeError("Sample APK checksum mismatch: " + str(path))
            adb("install", "-r", str(path))
            if app.get("component"):
                shell("pm", "enable", app["component"])
                if not app["component"].endswith(".Green"):
                    shell("pm", "disable", app["component"].rsplit(".",1)[0]+".Green")
        display(2560)
        shell("wm", "density", "480")
        shell("cmd", "overlay", "enable-exclusive", "--category", "com.android.internal.systemui.navbar.gestural")
        for namespace, key, value in [
            ("system", "accelerometer_rotation", "0"), ("system", "user_rotation", "0"),
            ("global", "window_animation_scale", "0"), ("global", "transition_animation_scale", "0"),
            ("global", "animator_duration_scale", "0"), ("global", "sysui_demo_allowed", "1"),
        ]:
            shell("settings", "put", namespace, key, value)
        demo("enter")
        demo("clock", "-e", "hhmm", "0941")
        demo("network", "-e", "wifi", "show", "-e", "level", "4", "-e", "fully", "true", "-e", "mobile", "hide")
        demo("notifications", "-e", "visible", "false")
        demo("battery", "-e", "level", "100", "-e", "plugged", "false")
        stage("prepare")
        stage("pin-grid")
        stage("pin-thumb")
        ids = json.loads(shell("cat", REMOTE + "/widget-ids.json"))
        apps = json.loads(shell("cat", REMOTE + "/icon-provenance.json"))["apps"]
        uri = "content://com.android.launcher3.settings/favorites"
        dock = shell("content", "query", "--uri", uri, "--projection", "_id:rank", "--where", "container=-101")
        dock_rows=sorted(((int(a),int(b)) for a,b in re.findall(r"_id=(\d+), rank=(\d+)",dock)),key=lambda row:row[1])
        for (row_id,_), app in zip(dock_rows,[apps[0],apps[1],apps[2],apps[15],apps[7]]):
            intent="#Intent;action=android.intent.action.MAIN;category=android.intent.category.LAUNCHER;launchFlags=0x10200000;component="+app["component"]+";end"
            shell("content","update","--uri",uri,"--where",f"_id={row_id}","--bind","itemType:i:0","--bind","title:s:"+app["label"],"--bind","intent:s:"+intent)
        all_rows = shell("content", "query", "--uri", uri, "--projection", "_id")
        next_shortcut_id = max(map(int, re.findall(r"_id=(\d+)", all_rows)), default=0) + 1

        def shortcuts(screen, comparison=False):
            nonlocal next_shortcut_id
            # Genuine launcher shortcuts establish the normal home-screen density.
            entries = [(i % 5, i // 5, app) for i, app in enumerate(apps[:8] + [apps[12], apps[9]])]
            if comparison:
                entries += [(i % 2, 3 + i // 2, apps[n]) for i, n in enumerate([0, 1, 4, 5])]
            for x, y, app in entries:
                where = f"container=-100 AND itemType=0 AND screen={screen} AND cellX={x} AND cellY={y}"
                existing = shell("content", "query", "--uri", uri, "--projection", "_id:intent", "--where", where)
                row_id = re.search(r"_id=(\d+)", existing)
                if row_id and "component=" + app["component"] + ";" in existing:
                    continue
                operation = ["content", "update" if row_id else "insert", "--uri", uri]
                if row_id:
                    operation += ["--where", "_id=" + row_id.group(1)]
                else:
                    operation += ["--bind", "_id:i:" + str(next_shortcut_id)]
                    next_shortcut_id += 1
                intent = ("#Intent;action=android.intent.action.MAIN;category=android.intent.category.LAUNCHER;"
                          "launchFlags=0x10200000;component=" + app["component"] + ";end")
                for key, value in [("title:s", app["label"]), ("intent:s", intent), ("itemType:i", "0"),
                                   ("container:i", "-100"), ("screen:i", str(screen)), ("cellX:i", str(x)),
                                   ("cellY:i", str(y)), ("spanX:i", "1"), ("spanY:i", "1"), ("profileId:l", "0")]:
                    operation += ["--bind", key + ":" + value]
                shell(*operation)
                actual = shell("content", "query", "--uri", uri, "--projection", "_id", "--where", where)
                if not re.search(r"_id=\d+", actual):
                    raise RuntimeError("Launcher comparison shortcut was not created: " + app["label"])

        rows = shell("content", "query", "--uri", "content://com.android.launcher3.settings/favorites",
                     "--projection", "_id:itemType:appWidgetId")
        widget_rows = {}
        for family in ["grid", "thumb"]:
            row = next((line for line in rows.splitlines()
                        if re.search(r"appWidgetId=" + str(ids[family]) + r"(?:,|$)", line)), None)
            if row is None:
                raise RuntimeError("Pinned widget has no Launcher3 placement: " + family)
            widget_rows[family] = re.search(r"_id=(\d+)", row).group(1)

        def position(family, screen, x, y, width, height):
            shell("content", "update", "--uri", "content://com.android.launcher3.settings/favorites",
                  "--where", "_id=" + widget_rows[family], "--bind", f"screen:i:{screen}",
                  "--bind", f"cellX:i:{x}", "--bind", f"cellY:i:{y}",
                  "--bind", f"spanX:i:{width}", "--bind", f"spanY:i:{height}")

        position("grid", 1, 3, 3, 2, 2)
        position("thumb", 2, 2, 3, 3, 2)
        shortcuts(1, comparison=True)
        shortcuts(2)
        shortcuts(3)
        page(1, "Grid")
        for name in ["grid9", "grid12"]:
            stage(name)
        display(3120)
        page(1, "Grid")
        stage("grid16")
        # An actual debug-only Android widget host permits low placement on a
        # plain black page without Launcher3 reserving a dock beneath it.
        stage("home-right")
        position("thumb", 2, 0, 3, 3, 2)
        page(2, "Thumb icon selector")
        stage("home-left")
        display(2560)
        position("grid", 3, 0, 4, 5, 1)
        page(3, "Grid")
        stage("strip")
        for name in ["visual", "layers"]:
            stage(name)
        (DEST / "phone").mkdir(parents=True, exist_ok=True)
        for name, _ in CAPTURES:
            adb("pull", REMOTE + "/" + name, str(DEST / "phone" / name))
        adb("pull", REMOTE + "/icon-provenance.json", str(DEST / "icon-provenance.json"))
        for old in (DEST / "phone").glob("*.png"):
            if old.name not in {name for name, _ in CAPTURES}:
                old.unlink()
    finally:
        demo("exit")
        # Leave the ordinary JUnit runner in place for subsequent test runs.
        run("./gradlew", ":app:assembleDebugAndroidTest", "--console=plain")
        adb("install", "-r", "app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk")


def package():
    provenance = json.loads((DEST / "icon-provenance.json").read_text())
    version = provenance["appVersion"]
    records = []
    cards = []
    hashes = set()
    for (name, title), description, alt_text in zip(CAPTURES, DESCRIPTIONS, ALT_TEXT, strict=True):
        path = DEST / "phone" / name
        with Image.open(path) as image:
            image.verify()
        with Image.open(path) as image:
            if image.size != CAPTURE_SIZES[name] or image.mode != "RGB":
                raise RuntimeError(f"Expected {CAPTURE_SIZES[name]} RGB PNG: {path}")
        checksum = hashlib.sha256(path.read_bytes()).hexdigest()
        if checksum in hashes:
            raise RuntimeError("Duplicate capture: " + name)
        hashes.add(checksum)
        records.append({"file": "phone/" + name, "title": title, "width": CAPTURE_SIZES[name][0],
                        "height": CAPTURE_SIZES[name][1], "format": "24-bit RGB PNG, no alpha",
                        "description": description, "altText": alt_text, "sha256": checksum})
        assert len(alt_text)<=140
        cards.append(f'<figure><a href="phone/{name}"><img src="phone/{name}" '
                     f'alt="{html.escape(alt_text)}" loading="lazy"></a><figcaption>'
                     f'<strong>{name[:2]}. {html.escape(title)}</strong><p>{html.escape(description)}</p></figcaption></figure>')
    (DEST / "manifest.json").write_text(json.dumps({"appVersion": version, "captureProfiles": provenance["captureProfiles"], "screenshots": records}, indent=2) + "\n")
    (DEST / "listing/en-US/screenshot-alt-text.txt").write_text("\n\n".join(record["file"]+"\n"+record["altText"] for record in records)+"\n")
    (DEST / "index.html").write_text('''<!doctype html>
<html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>App Grid &amp; Thumb Select Widget · Google Play materials</title>
<style>
body{margin:0;padding:32px;background:#101610;color:#eaf1e5;font:16px system-ui,sans-serif}
h1{font-size:32px;margin:0 0 12px}p{color:#b5c4af;line-height:1.6;max-width:760px}
a{color:#bef779}main{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:24px}
figure{margin:0}img{width:100%;height:auto;display:block;border-radius:10px;border:1px solid #344032}
figcaption{padding:12px 0;line-height:1.5}figcaption p{font-size:14px;margin:8px 0}header{margin-bottom:28px}
.brand{display:flex;align-items:center;gap:20px}.brand img{width:88px;border:0;border-radius:0}.feature{max-width:1024px;margin:24px 0}.feature img{border-radius:0}nav{display:flex;flex-wrap:wrap;gap:20px;margin:20px 0}h2{font-size:24px;margin:36px 0 20px}
</style><header><div class="brand"><img src="graphics/app-icon-512.png" alt="App Grid &amp; Thumb Select Widget app icon"><h1>App Grid &amp; Thumb Select Widget</h1></div>
<p>More apps in less space. Keep frequent apps close to your chosen corner.<br>
Compare 9, 12, and 16 apps in a compact Grid, or place a Thumb selector low on the screen.</p>
<p>Make it yours with optional labels, light or dark backgrounds, gradients, grayscale icons, borders, opacity, and independent layer sizes.</p>
<p>Eight real app and launcher captures · version VERSION_TOKEN · RGB PNG.<br>
Five 1440 × 2560 portraits and three tall 1440 × 3120 large-phone examples, with gesture navigation.<br>
Modern original Fossify color-variant icons, Android Settings, and our own app icon. No phone artwork or imported icon images.</p>
<nav><a href="graphics/app-icon-512.png">512px store icon</a><a href="graphics/feature-graphic-1024x500.png">Feature graphic</a><a href="RELEASE-CHECKLIST.md">Release checklist</a><a href="APP-CONTENT.md">App content answers</a></nav>
<figure class="feature"><img src="graphics/feature-graphic-1024x500.png" alt="More apps. Less reaching. Original thumb-layout illustration."><figcaption>1024 × 500 feature graphic · original vector artwork</figcaption></figure>
<h2>Phone screenshots</h2>
</header><main>'''.replace("VERSION_TOKEN", html.escape(version)) + "\n".join(cards) + "</main></html>\n")
    with zipfile.ZipFile(DEST / "icon-navigator-google-play-screenshots.zip", "w", zipfile.ZIP_DEFLATED) as archive:
        for path in [*(DEST / "phone" / name for name, _ in CAPTURES),
                     DEST / "README.md", DEST / "manifest.json", DEST / "icon-provenance.json",
                     DEST / "sample-apps.json", DEST / "index.html",
                     DEST / "RELEASE-CHECKLIST.md", DEST / "APP-CONTENT.md", DEST / "PUBLICATION.md",
                     *sorted((DEST / "graphics").glob("*"))]:
            archive.write(path, path.relative_to(DEST))
    print("Validated and packaged eight screenshots:", DEST)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", help="Dedicated rootable AOSP emulator serial; never a phone")
    parser.add_argument("--package-only", action="store_true", help="Validate existing captures and rebuild the gallery/ZIP")
    args = parser.parse_args()
    if not args.package_only:
        capture(args.serial)
    package()
