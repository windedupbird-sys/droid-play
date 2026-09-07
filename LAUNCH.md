# Color Tap — Play Store launch guide

Everything you need to submit **Color Tap v1.1.0** to Google Play.

## Status

| Item | Status |
|------|--------|
| Signed release AAB build | Ready |
| Privacy policy URL | https://windedupbird-sys.github.io/droid-play/privacy-policy.html |
| Store icon (512×512) | `store-assets/play-store-icon-512.png` |
| Feature graphic | `store-assets/feature-graphic-1024x500.png` |
| Screenshots (3) | `store-assets/screenshot-*.png` |
| Listing copy | `store-listing/` folder |
| High score persistence | Done |
| AdMob integration | Done (add your real IDs before production) |
| Target SDK 35 | Done |

## Before you submit (required)

### 1. Create AdMob ad units

1. Sign in at https://admob.google.com/ with your Google account
2. Add app **Color Tap** (Android)
3. Create an **Interstitial** ad unit
4. Copy your App ID and Interstitial Ad Unit ID
5. Create `admob.properties` from the example:

```bash
cp admob.properties.example admob.properties
```

Edit `admob.properties`:

```properties
admob.app_id=ca-app-pub-XXXXXXXX~XXXXXXXX
admob.interstitial_id=ca-app-pub-XXXXXXXX/XXXXXXXX
```

Or add GitHub Actions secrets: `ADMOB_APP_ID`, `ADMOB_INTERSTITIAL_ID`

### 2. Build the production AAB

```bash
./gradlew bundleRelease
```

Upload: `app/build/outputs/bundle/release/app-release.aab`

Or download from GitHub Releases after tagging `v1.1.0`.

### 3. Create Play Console account

https://play.google.com/console — $25 one-time fee.

---

## Play Console walkthrough

### Step 1: Create app

- **App name:** Color Tap
- **Default language:** English (United States)
- **App or game:** Game
- **Free or paid:** Free

### Step 2: Store listing

Copy text from `store-listing/`:

| Field | File |
|-------|------|
| App name | `title.txt` |
| Short description | `short-description.txt` |
| Full description | `full-description.txt` |

Upload graphics from `store-assets/`:

| Asset | File |
|-------|------|
| App icon | `play-store-icon-512.png` |
| Feature graphic | `feature-graphic-1024x500.png` |
| Phone screenshots | `screenshot-01-menu.png`, `screenshot-02-gameplay.png`, `screenshot-03-game-over.png` |

**Privacy policy URL:**
```
https://windedupbird-sys.github.io/droid-play/privacy-policy.html
```

**Category:** Games → Arcade

### Step 3: App content

| Declaration | Answer |
|-------------|--------|
| Privacy policy | URL above |
| Ads | Yes, contains ads |
| Content rating | Complete IARC questionnaire (see `CONTENT_RATING.md`) |
| Target audience | All ages (or 13+ recommended with ads) |
| Data safety | See `DATA_SAFETY.md` |
| News app | No |

### Step 4: Upload release

1. **Testing → Internal testing** → Create new release
2. Upload `app-release.aab`
3. Release notes: copy from `store-listing/release-notes.txt`
4. Save and review
5. Add testers (your email) and install from Play Store link
6. When satisfied, promote to **Production**

---

## Download latest AAB

https://github.com/windedupbird-sys/droid-play/releases

---

## After launch

- Back up `release-keystore/` and `keystore.properties` securely
- For updates: bump `versionCode` and `versionName` in `app/build.gradle.kts`, build new AAB, upload to Play Console
