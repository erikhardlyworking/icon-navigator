# First Google Play release

Prepared for **Hardly Working**, support **workinghardlyforyou@gmail.com**, on **15 November 2026**. Package **dev.iconnavigator**, version **0.6.5 (13)**, Android 12+ (min 31), target **36**. These are upload materials; no Play Console release has been submitted.

## Files to upload

| Play Console field | File or value |
| --- | --- |
| App name | `listing/en-US/title.txt` — App Grid & Thumb Select Widget |
| Short description | `listing/en-US/short-description.txt` |
| Full description | `listing/en-US/full-description.txt` |
| Release notes | `listing/en-US/release-notes.txt` |
| App icon | `graphics/app-icon-512.png` — 512 × 512 RGBA PNG |
| Feature graphic | `graphics/feature-graphic-1024x500.png` — 1024 × 500 RGB PNG |
| Phone screenshots | All eight `phone/*.png`, in numbered order — 1080 × 1920 RGB |
| App bundle | `release/icon-navigator-0.6.5.aab` — signed with the private upload key |
| Upload certificate, if requested | `release/upload-certificate.pem` — public certificate only |
| Category | App → Personalization |
| Developer | Hardly Working |
| More apps developer link | https://play.google.com/store/apps/dev?id=6256987868491351341 |
| Support email | workinghardlyforyou@gmail.com |
| Website | https://github.com/erikhardlyworking/icon-navigator |
| Privacy URL | https://github.com/erikhardlyworking/icon-navigator/blob/main/PRIVACY.md |

`store-listing.json` contains the same copy and contact information. `manifest.json` records screenshot checksums; `release/release-manifest.json` records the signed bundle checksum. `kit-manifest.json` covers the complete kit. Preview the images in `index.html`.

## Before submitting

1. The prepared public source and `PRIVACY.md` have been published; see `PUBLICATION.md`. Before submission, open the privacy URL while signed out and confirm it still renders as a readable webpage. The app also includes the full offline policy under Visual options → Privacy & support.
2. Create the app in Play Console with default language English (United States). Choose **free or paid**, distribution countries and intended age groups. These are publisher decisions and have not been guessed. Complete account identity, contact, trader and payment declarations as applicable.
3. Use Google Play App Signing. For this first release, choose Google's generated **app signing key**; the provided bundle is signed with a separate **upload key**. Supply its public certificate if the Console requests it. Never upload the private key or password to GitHub or attach them to the store listing.
4. Upload the AAB to **internal testing** first. Use the supplied descriptions and graphic files. Check the device catalog, automated pre-launch report and the real widget workflow on the Play-delivered build. The bundle targets API 36, meeting the current new-app target requirement. [Target API rules](https://support.google.com/googleplay/android-developer/answer/11926878).
5. Complete App content using `APP-CONTENT.md`. Submit the actual IARC questionnaire to obtain a rating; no rating is preassigned here. Preview the listing and confirm original sample app icons are appropriate for the listing.
6. Complete any testing gate shown for your account. Personal developer accounts created after 13 November 2023 generally require **12 testers opted into a closed test continuously for 14 days**, followed by an application for production access. This is separate from automated tests. [Google's account testing requirements](https://support.google.com/googleplay/android-developer/answer/14151465).
7. Review the release and submit it in Play Console when the account requirements, privacy URL, declarations and testing are complete.

The app icon is a full square without baked-in rounded corners or shadow. Phone screenshots show actual UI; the feature graphic is an original vector illustration. No tablet/TV/Wear assets are included or required for this phone listing. [Graphic asset specifications](https://support.google.com/googleplay/android-developer/answer/9866151), [icon design specifications](https://developer.android.com/distribute/google-play/resources/icon-design-specifications), [store listing copy limits](https://support.google.com/googleplay/android-developer/answer/9859152).

## Keep the upload key

The key and its password are in the ignored local directory **`.private/play-signing/`**. Securely back up that directory separately. It is excluded from the kit and public source. The alias is `upload`, the key is RSA 4096-bit PKCS#12, and the public certificate is included in `release/`. Future bundles should use the same upload key unless it is reset through Play Console. [Play App Signing](https://support.google.com/googleplay/android-developer/answer/9842756).

The phone currently uses a debug-signed build. Google's Play app signing certificate will differ, so the first Play-delivered installation cannot update that debug build in place. Keep using compatible debug updates on the phone until you choose a migration; this preparation has not removed its data. Local widget settings currently have no export/import feature.

## Rebuild

Use JDK 17 and Android SDK Platform 36 with `JAVA_HOME` and `ANDROID_HOME` set. Run the required checks from `AGENTS.md`, then:

```sh
python3 tools/prepare-play-assets.py
python3 tools/build-play-release.py
python3 tools/package-play-kit.py
```

The graphics script needs Pillow, ImageMagick, librsvg and Cairo. Capture fresh screenshots on the isolated emulator as documented in `README.md`. `build-play-release.py --create-upload-key` is only for creating a first key when no original exists; do not replace the existing key. `package-play-kit.py` exports an explicit allowlist of public source, checks for private files, and packages the store materials. It never reads the signing password or private key.
