# Play Console declarations for 0.6.6

These answers follow the current source and release configuration. Review the exact questions presented in your Console; account-specific business declarations and the intended audience remain the publisher's choices.

## Data safety

**Does the app collect or share required user data types? No.** No personal data is transmitted off the device by App Grid & Thumb Select Widget. No accounts, ads, analytics, crash-upload service or network permission exist. No production SDKs are included. Installed launcher app identifiers, names and icons are used locally to configure shortcuts. Selected images are copied from the user-selected document URI into private app storage. Preferences and imported artwork remain local; cloud and device-transfer backup are disabled.

Android's widget host receives the user-configured widget for display, and tapping a shortcut launches the selected activity. These are the app's user-requested/platform operations. They are not a transfer of an installed-app inventory to the publisher. Google excludes qualifying user-initiated transfers and service-provider processing from its sharing declaration; on-device processing is outside its collection definition. This **No/No** assessment is an inference from those definitions and the audited behavior, not an automated Play certification. [Data safety definitions and exceptions](https://support.google.com/googleplay/android-developer/answer/10787469).

If the form skips encryption/deletion questions after No, leave them skipped. Do not claim independent security certification or an encryption-in-transit feature: the app transmits no data. There is no account creation, so the account-deletion URL requirement for apps with account creation does not apply. Local settings/artwork can be deleted through Android Settings → Apps → App Grid & Thumb Select Widget → Storage → Clear storage, or by uninstalling. Removing a widget deletes its configuration; shared imported files may remain until app storage is cleared.

**More apps by this developer** opens Hardly Working's verified Google Play developer page, preferring the Play Store and falling back to a browser. There is no in-app email button. Information voluntarily sent to the support email listed on Google Play or in the policy is handled by the publisher and email providers; it is not automatically read or uploaded by the app. The policy link opens a browser. Reassess the form and policy before adding any network service or SDK.

## Other App content answers

| Item | Answer or evidence |
| --- | --- |
| Privacy policy | Use the public rendered `PRIVACY.md` URL in RELEASE-CHECKLIST.md; verify it is live and works without login before submission |
| Ads | No ads |
| App access | All functionality is available without special access; no login, subscription, credentials or region restriction |
| Account creation | None |
| Sensitive permissions | None requested; launcher discovery uses a scoped MAIN/LAUNCHER query, not QUERY_ALL_PACKAGES |
| Health / financial features | None |
| Government app | No |
| News / dating / social features | None |
| Advertising ID | Not used; no AD_ID permission |
| Purchases | No in-app purchases or billing integration; store price is a separate publisher choice |
| Target audience | Choose the actual intended age groups in Console; this preparation does not assert children-directed design |
| Content rating | Complete IARC for a personalization utility. No bundled violence, sexual content, gambling, user messaging or public user-generated content. The user's own local artwork is not shared by the app. Use the final rating assigned by IARC |

All apps require an in-app and publicly accessible privacy policy, even when they collect no data. [Google's review preparation requirements](https://support.google.com/googleplay/android-developer/answer/9859455).

## Reviewer instructions, if requested

No login or test account is needed. Long-press the Android home screen, choose Widgets → App Grid & Thumb Select Widget, and add Grid or Thumb icon selector. Select one or more installed apps, adjust Basic settings or Visual options, and save. Alternatively open App Grid & Thumb Select Widget, configure a draft, save and choose Add widget. The launcher controls placement and resizing. Tap a widget icon to launch its assigned activity directly. Blank widget space and empty-widget hints have no tap action; long-press the widget and select its settings/configuration action to edit it. Thumb handedness and inner/outer reach are in Basic settings; layer icon sizes are in Visual options. The live preview remains visible throughout. Settings show overflow warnings; widgets have no pagination. Privacy & support is at the bottom of Visual options.

## Code evidence

- `app/src/main/AndroidManifest.xml`: no uses-permission entries, no production services, scoped launcher queries, allowBackup=false.
- `app/src/main/res/xml/data_extraction_rules.xml`: excludes all private-data domains from cloud/device transfer.
- `ConfigStore.java`: local SharedPreferences; `EditorActivity.java`: user-selected document imports copied locally.
- `PrivacyActivity.java` and `res/raw/privacy_policy.txt`: offline policy and explicit external links.
- `app/build.gradle`: test-only dependencies; no production dependencies or native binaries.
- Release bundle validation excludes capture host/instrumentation classes. Tests inspect privacy permissions and backup/export settings.
