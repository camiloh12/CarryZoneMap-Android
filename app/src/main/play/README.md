# Play Store Metadata

This directory contains all the metadata and assets needed for publishing **CCW Map** to the Google Play Store.

## 📁 Directory Structure

```
play/
├── contact-email.txt              # Developer contact email (REQUIRED)
├── contact-website.txt            # Developer website (REQUIRED)
├── default-language.txt           # Default language code (en-US)
├── listings/
│   └── en-US/                     # English (US) listing
│       ├── title.txt              # App title (max 50 chars)
│       ├── short-description.txt  # Short desc (max 80 chars)
│       ├── full-description.txt   # Full desc (max 4000 chars)
│       └── graphics/              # Visual assets
│           ├── icon/              # App icon (512x512 PNG)
│           ├── feature-graphic/   # Feature graphic (1024x500 PNG)
│           ├── phone-screenshots/ # Phone screenshots (2-8 images)
│           └── tablet-screenshots/# Tablet screenshots (optional)
└── release-notes/
    └── en-US/
        └── default.txt            # Release notes for updates
```

## ✅ Before Publishing - Checklist

### 1. Update Contact Information

Edit these files with your actual contact details:

- [ ] `contact-email.txt` - Replace with your support email
- [ ] `contact-website.txt` - Replace with your website or GitHub repo

### 2. Create Required Graphics

You need to create the following image assets:

#### App Icon (512x512 PNG)
- **Size**: 512x512 pixels
- **Format**: 32-bit PNG with alpha channel
- **Location**: `listings/en-US/graphics/icon/`
- **Filename**: `icon.png`
- **Requirements**:
  - Must be square
  - Must have transparent background or solid color
  - Will be used as the Play Store icon (different from launcher icon)

#### Feature Graphic (1024x500 PNG)
- **Size**: 1024x500 pixels
- **Format**: 24-bit PNG or JPEG
- **Location**: `listings/en-US/graphics/feature-graphic/`
- **Filename**: `feature-graphic.png`
- **Requirements**:
  - Must not include device frames
  - Used in Play Store listing header
  - Should showcase key app features visually

**Design Suggestions for Feature Graphic:**
```
┌────────────────────────────────────────────────┐
│  [App Icon]  CCW Map              │
│                                                │
│  🗺️ Community-Powered Zone Tracking            │
│  🟢🟡🔴 Color-Coded • Offline-First • Secure   │
└────────────────────────────────────────────────┘
```

#### Phone Screenshots (Minimum 2, Maximum 8)
- **Size**:
  - 16:9 aspect ratio recommended (e.g., 1080x1920, 1440x2560)
  - Min dimension: 320px
  - Max dimension: 3840px
- **Format**: 24-bit PNG or JPEG
- **Location**: `listings/en-US/graphics/phone-screenshots/`
- **Filenames**: `screenshot-1.png`, `screenshot-2.png`, etc.
- **Order matters**: Screenshots are displayed in alphabetical order

**Recommended Screenshots:**
1. `screenshot-1.png` - Map view with color-coded pins (green/yellow/red zones)
2. `screenshot-2.png` - Login/signup screen
3. `screenshot-3.png` - Pin creation dialog (showing POI name and status selection)
4. `screenshot-4.png` - Map with user location and nearby POIs
5. `screenshot-5.png` - Map showing offline indicator or sync status
6. `screenshot-6.png` - Pin editing dialog (showing edit/delete options)

**Screenshot Tips:**
- Use a real device or emulator with standard resolution
- Show the app in action with realistic data
- Add text overlays highlighting key features (optional but recommended)
- Ensure text is readable and UI elements are clear
- Consider adding mockup device frames for visual appeal

#### Tablet Screenshots (Optional but Recommended)
- **Size**: 16:9 or 16:10 aspect ratio (e.g., 2048x1536)
- **Location**: `listings/en-US/graphics/tablet-screenshots/`
- **Purpose**: Shows tablet-optimized layout (if applicable)

### 3. Review Text Content

Review and customize if needed:

- [ ] `listings/en-US/title.txt` - App title (currently: "CCW Map")
- [ ] `listings/en-US/short-description.txt` - 80-char tagline
- [ ] `listings/en-US/full-description.txt` - Full description with features
- [ ] `release-notes/en-US/default.txt` - Initial release notes

### 4. Additional Play Console Requirements

When publishing, you'll also need to provide in the Play Console:

- [ ] **App Category**: Maps & Navigation
- [ ] **Content Rating**: Complete questionnaire (likely Mature 17+ due to firearm content)
- [ ] **Privacy Policy URL**: Required for apps collecting user data
- [ ] **Target Audience**: Age groups (18+)
- [ ] **Data Safety**: Declare what data is collected and how it's used
- [ ] **App Access**: Provide test credentials if needed
- [ ] **APK or AAB**: Upload signed release build

## 🚀 Publishing Options

### Option 1: Manual Upload (Easiest for First Release)

1. Go to [Google Play Console](https://play.google.com/console)
2. Create a new app
3. Copy/paste content from the `.txt` files into the appropriate fields
4. Upload graphics from the `graphics/` folders
5. Upload your signed APK/AAB

### Option 2: Gradle Play Publisher Plugin (Automated)

For automated deployments, you can use the [Gradle Play Publisher plugin](https://github.com/Triple-T/gradle-play-publisher).

**Setup:**

1. Add to `build.gradle.kts` (app module):

```kotlin
plugins {
    id("com.github.triplet.play") version "3.9.0"
}

play {
    serviceAccountCredentials.set(file("../play-service-account.json"))
    track.set("internal") // internal, alpha, beta, or production
    defaultToAppBundles.set(true)
}
```

2. Create a Google Cloud service account and download JSON key
3. Save as `play-service-account.json` (add to `.gitignore`!)
4. Run: `./gradlew publishBundle` to publish automatically

### Option 3: Fastlane (Cross-Platform)

If you also develop for iOS, consider [Fastlane Supply](https://docs.fastlane.tools/actions/supply/).

## 📝 Data Safety Declaration

For the Play Console Data Safety section, declare:

### Data Collected:
- **Account Info**: Email address (for authentication)
- **Location**: Approximate location (for map centering, not stored on server)
- **User-Generated Content**: Pin locations and status (stored on Supabase)

### Data Usage:
- **Account Info**: Used for authentication and accountability
- **Location**: Used only for map positioning, not transmitted to server
- **User-Generated Content**: Shared publicly with all users

### Data Sharing:
- Pin data is shared publicly with all app users
- Email addresses are not shared publicly

### Security:
- Data encrypted in transit (HTTPS/TLS)
- Authentication via Supabase Auth
- No data sold to third parties

## 🔒 Content Rating Guidance

When filling out the content rating questionnaire, be aware:

- **Violence**: The app is about firearms → may trigger higher rating
- **User-Generated Content**: Community can post data → requires disclosure
- **Location**: Uses device location → requires disclosure

Expected rating: **Mature 17+** or **Teen** depending on responses

## 📱 Privacy Policy

You MUST provide a Privacy Policy URL. Create a privacy policy covering:

1. What data is collected (email, location, pins)
2. How data is used (authentication, mapping, community sharing)
3. Data retention (how long data is kept)
4. User rights (account deletion, data export)
5. Third-party services (Supabase, MapTiler, Google Play Services)
6. Contact information

**Quick Privacy Policy Generators:**
- [TermsFeed](https://www.termsfeed.com/privacy-policy-generator/)
- [FreePrivacyPolicy.com](https://www.freeprivacypolicy.com/)
- [PrivacyPolicies.com](https://www.privacypolicies.com/)

Host the privacy policy on your website or GitHub Pages.

## 🎨 Design Asset Creation Tools

### Free Tools:
- **Figma** - Professional design tool (free tier available)
- **Canva** - Easy templates for graphics (free tier available)
- **GIMP** - Open-source Photoshop alternative
- **Inkscape** - Vector graphics editor

### Screenshot Tools:
- **Android Emulator** - Built into Android Studio
- **Screener** - Add device frames: https://play.google.com/store/apps/details?id=de.toastcode.screener
- **Fastlane Frameit** - Automated device frames

### Mockup Generators:
- **Mockuphone** - https://mockuphone.com/
- **Smartmockups** - https://smartmockups.com/
- **Previewed** - https://previewed.app/

## 📊 App Signing

Don't forget to sign your release build:

```bash
# Generate keystore (only once)
keytool -genkey -v -keystore release-keystore.jks -alias concealed-carry-map -keyalg RSA -keysize 2048 -validity 10000

# Sign APK/AAB in build.gradle.kts
android {
    signingConfigs {
        create("release") {
            storeFile = file("../release-keystore.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = "concealed-carry-map"
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }
}
```

**Important**: Keep your keystore file secure and NEVER commit it to Git!

## 🔄 Updating Release Notes

For future releases, update `release-notes/en-US/default.txt` with what's new:

```
Version 1.1.0 - New Features

✨ What's New:
• Added search functionality for locations
• Improved offline sync reliability
• New dark mode theme

🐛 Bug Fixes:
• Fixed crash on Android 14
• Improved map rendering performance

📝 Notes:
• Please update to get the latest features
```

## 📞 Support

After publishing, users can contact you via:
- Email: (from `contact-email.txt`)
- Website: (from `contact-website.txt`)
- Play Store reviews (respond to feedback!)

## ✅ Final Pre-Launch Checklist

- [ ] All contact info updated
- [ ] App icon (512x512) created and added
- [ ] Feature graphic (1024x500) created and added
- [ ] At least 2 phone screenshots added
- [ ] Descriptions reviewed and accurate
- [ ] Privacy Policy created and hosted
- [ ] Release build signed with release keystore
- [ ] Tested on multiple devices/Android versions
- [ ] Content rating questionnaire completed
- [ ] Data safety declaration completed
- [ ] Age rating appropriate for content
- [ ] App tested in release mode (not debug)

## 🎉 Ready to Publish!

Once everything is ready:

1. Build signed release: `./gradlew bundleRelease`
2. Upload to Play Console (Internal Testing first recommended)
3. Complete all required sections
4. Submit for review
5. Wait for approval (usually 1-3 days)

Good luck with your launch! 🚀
