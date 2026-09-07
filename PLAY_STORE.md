# Google Play Store

**Ready to launch?** See **[LAUNCH.md](LAUNCH.md)** for the complete step-by-step guide.

Also see:
- **[PLAY_STORE.md](PLAY_STORE.md)** — technical checklist
- **[DATA_SAFETY.md](DATA_SAFETY.md)** — Play Console data safety answers
- **[CONTENT_RATING.md](CONTENT_RATING.md)** — content rating questionnaire guide

## 1. Create a Play Console account

1. Go to https://play.google.com/console
2. Pay the one-time **$25** registration fee
3. Complete identity verification

## 2. Generate your upload keystore (one time)

```bash
./scripts/generate-keystore.sh
```

This creates:

- `release-keystore/colortap-release.keystore`
- `keystore.properties` (local only, never commit)

**Back up the keystore file and passwords.** You must use the same key for every update.

## 3. Add GitHub secrets for signed CI builds

In https://github.com/windedupbird-sys/droid-play/settings/secrets/actions add:

| Secret | Value |
|--------|-------|
| `KEYSTORE_BASE64` | Output from `base64 -w 0 release-keystore/colortap-release.keystore` |
| `KEYSTORE_PASSWORD` | From `keystore.properties` |
| `KEY_ALIAS` | `colortap` |
| `KEY_PASSWORD` | From `keystore.properties` |

## 4. Build the Play Store bundle locally

```bash
./gradlew bundleRelease
```

Upload this file to Play Console:

```
app/build/outputs/bundle/release/app-release.aab
```

Or create a GitHub release tag to build it in CI:

```bash
git tag v1.0.1
git push github v1.0.1
```

The workflow uploads `ColorTap.aab` to GitHub Releases.

## 5. Enable GitHub Pages for the privacy policy

1. Open **Settings → Pages** on GitHub
2. Source: **GitHub Actions**
3. After the next push, your privacy policy will be live at:

```
https://windedupbird-sys.github.io/droid-play/privacy-policy.html
```

Use that URL in Play Console.

## 6. Create the app in Play Console

1. **Create app**
2. App name: `Color Tap`
3. Default language: English
4. App or game: **Game**
5. Free or paid: **Free**

## 7. Store listing assets

Prepare these before submission:

| Asset | Requirement |
|-------|-------------|
| App name | Color Tap |
| Short description | Up to 80 characters |
| Full description | Up to 4000 characters |
| App icon | 512 x 512 PNG (see `store-assets/`) |
| Feature graphic | 1024 x 500 PNG |
| Phone screenshots | At least 2 (1080 x 1920 or similar) |
| Privacy policy URL | GitHub Pages URL above |

Suggested short description:

> Tap colorful circles before they fade. Fast reflexes win. How high can you score?

## 8. Content rating

Complete the IARC questionnaire in Play Console.

Suggested answers for Color Tap:

- No violence, gambling, user-generated content, or location sharing
- Suitable for all ages

## 9. Data safety form

Because Color Tap collects no data directly:

- **Does your app collect or share user data?** No (for your own app code)
- Declare **Google AdMob** as a third-party that may collect data for advertising
- Typical AdMob declarations: Device IDs, ad interactions, IP address (for ad delivery)
- **Ads:** Yes — the app contains ads

## 10. App content declarations

- **Ads:** Yes
- **In-app purchases:** No
- **Target audience:** All ages (or 13+ if you prefer a narrower audience)
- **News app / COVID / government:** No

## 11. Upload the release

1. Play Console → **Testing → Internal testing** (recommended first)
2. Create a new release
3. Upload `app-release.aab`
4. Add release notes
5. Review and roll out

After internal testing looks good, promote to **Production**.

## 12. Version updates

Before each new Play Store upload:

1. Increase `versionCode` in `app/build.gradle.kts`
2. Update `versionName` (for example `1.1.0`)
3. Build a new signed AAB
4. Upload to Play Console

## Store asset folder

See `store-assets/README.md` for icon and screenshot guidance.
