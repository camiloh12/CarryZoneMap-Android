# Play Store Submission Checklist

Use this checklist to ensure you have everything ready before submitting to Google Play.

## ✅ Pre-Submission Checklist

### 📝 Metadata Files (Text Content)

- [ ] **contact-email.txt** - Updated with your real email
- [ ] **contact-website.txt** - Updated with your website/GitHub URL
- [ ] **title.txt** - Reviewed (currently: "CCW Map")
- [ ] **short-description.txt** - Reviewed (currently 76 chars)
- [ ] **full-description.txt** - Reviewed and customized if needed
- [ ] **release-notes/en-US/default.txt** - Reviewed initial release notes

### 🎨 Graphics & Visual Assets

- [ ] **App Icon** (512x512 PNG)
  - Location: `listings/en-US/graphics/icon/icon.png`
  - Created and looks good

- [ ] **Feature Graphic** (1024x500 PNG)
  - Location: `listings/en-US/graphics/feature-graphic/feature-graphic.png`
  - Professional looking banner

- [ ] **Phone Screenshots** (minimum 2, recommended 4-6)
  - Location: `listings/en-US/graphics/phone-screenshots/`
  - screenshot-1.png (Main map view) ✓
  - screenshot-2.png (Login screen) ✓
  - screenshot-3.png (Pin creation dialog)
  - screenshot-4.png (Additional feature)
  - screenshot-5.png (Optional)
  - screenshot-6.png (Optional)

- [ ] **Tablet Screenshots** (optional but recommended)
  - Location: `listings/en-US/graphics/tablet-screenshots/`

### 🔒 Legal & Privacy

- [ ] **Privacy Policy**
  - Created from `PRIVACY_POLICY_TEMPLATE.md`
  - All `[INSERT ...]` placeholders replaced
  - Hosted online (URL ready)
  - URL tested and accessible

- [ ] **Terms of Service** (optional but recommended)
  - Created if needed
  - Hosted online

### 🔐 App Signing & Build

- [ ] **Release Keystore Created**
  ```bash
  keytool -genkey -v -keystore release-keystore.jks \
    -alias concealed-carry-map -keyalg RSA -keysize 2048 -validity 10000
  ```
  - Keystore file: `release-keystore.jks`
  - Alias: `concealed-carry-map`
  - Passwords stored securely (NOT in version control)

- [ ] **Keystore Backup**
  - Keystore file backed up securely
  - Passwords recorded in secure location
  - **CRITICAL**: If you lose the keystore, you can NEVER update the app!

- [ ] **Build Configuration**
  - `build.gradle.kts` configured with signing config
  - ProGuard/R8 enabled for release (optional but recommended)
  - Version code and version name set correctly

- [ ] **Release Build Generated**
  ```bash
  ./gradlew bundleRelease
  # Output: app/build/outputs/bundle/release/app-release.aab
  ```
  - AAB file generated successfully
  - File size reasonable (< 150 MB recommended)

### 🧪 Testing

- [ ] **Tested on Real Device**
  - Installed release build (not debug)
  - All features work correctly
  - No crashes or major bugs

- [ ] **Tested on Multiple Android Versions**
  - Minimum: Android 8.0 (API 26) ✓
  - Recommended: Android 13-14 ✓

- [ ] **Tested Core Functionality**
  - Authentication (login/signup) works
  - Map loads and displays correctly
  - Pin creation works
  - Pin editing/deletion works
  - Location services work
  - Offline mode works
  - Cloud sync works when online

- [ ] **Performance Check**
  - App launches quickly (< 3 seconds)
  - Map rendering is smooth
  - No memory leaks
  - Battery usage reasonable

### 📱 Play Console Setup

- [ ] **Developer Account**
  - Google Play Developer account created ($25 one-time fee)
  - Account verified

- [ ] **App Created in Console**
  - New app created in Play Console
  - App name: "CCW Map"
  - Default language: English (United States)

- [ ] **Store Listing**
  - App title set
  - Short description set
  - Full description set
  - App icon uploaded
  - Feature graphic uploaded
  - Screenshots uploaded (phone + tablet)
  - App category: **Maps & Navigation**
  - Tags/keywords added (optional)

- [ ] **Content Rating**
  - Questionnaire completed
  - Expected rating: Mature 17+ or Teen
  - Firearms/weapons content disclosed
  - User-generated content disclosed

- [ ] **Target Audience**
  - Age groups: 18+
  - Target countries selected

- [ ] **Data Safety**
  - Data collection practices declared:
    - ✓ Email address (Account info)
    - ✓ Approximate location (for map centering)
    - ✓ User-generated content (pins)
  - Data usage explained
  - Privacy policy URL added

- [ ] **App Access**
  - If app requires login for testing:
    - Demo credentials provided
    - Test account created
  - OR "All features available without credentials" selected

- [ ] **Ads Declaration**
  - "App contains ads" - NO (unless you add ads later)

### 🌍 Countries & Pricing

- [ ] **Countries Selected**
  - Recommended: Start with US only
  - Expand to other countries after successful launch

- [ ] **Pricing**
  - Free (recommended for initial launch)
  - OR Paid (set price if applicable)

### 📜 Additional Information

- [ ] **App Content**
  - Reviewed Google's prohibited content policies
  - Confirmed app doesn't violate policies
  - Firearms content is informational only

- [ ] **News Apps** (Not applicable)

- [ ] **COVID-19 Contact Tracing** (Not applicable)

- [ ] **Data Safety Form**
  - All questions answered honestly
  - Data collection practices disclosed

### 🚀 Release Track

- [ ] **Internal Testing** (Recommended First)
  - Create internal testing track
  - Upload AAB file
  - Add internal testers (email addresses)
  - Test for 1-2 weeks

- [ ] **Closed Testing** (Optional - Alpha/Beta)
  - Create closed testing track
  - Invite beta testers
  - Gather feedback

- [ ] **Production** (Public Release)
  - Ready to go public
  - All above items completed
  - Promotional content ready (optional)

### 📣 Marketing (Optional but Recommended)

- [ ] **App Short Description** (80 chars)
  - Compelling tagline for search results

- [ ] **Promotional Text** (170 chars)
  - Used for special promotions

- [ ] **Promo Video** (YouTube URL)
  - 30-120 second demo video
  - Shows key features

- [ ] **Social Media**
  - Twitter/X account for updates
  - Website/landing page
  - Reddit post in relevant communities

### 🔍 SEO & Discovery

- [ ] **App Title Optimized**
  - Contains main keyword ("CCW Map")
  - Under 50 characters

- [ ] **Short Description Optimized**
  - Contains relevant keywords
  - Compelling call-to-action

- [ ] **Keywords Research**
  - Identified relevant search terms
  - Incorporated in description naturally

### ⚠️ Legal Considerations

- [ ] **Firearms Content Compliance**
  - App is informational only (not facilitating illegal activity)
  - Disclaimers included in description
  - Not promoting violence

- [ ] **User-Generated Content**
  - Moderation plan in place (or acknowledged)
  - Reporting mechanism (planned for future)
  - Terms of Service prohibit illegal content

- [ ] **Location Data**
  - Privacy policy explains location usage
  - Location permission requested appropriately
  - User can deny and still use app (if applicable)

### 📊 Post-Launch

- [ ] **Analytics Setup** (Optional)
  - Firebase Analytics configured
  - Crashlytics configured
  - Key events tracked

- [ ] **Monitoring Plan**
  - Check reviews daily for first week
  - Respond to user feedback
  - Monitor crash reports

- [ ] **Update Plan**
  - Version 1.1 features planned
  - Bug fix process established

---

## 🎯 Quick Launch Path (Minimum Requirements)

If you want to launch quickly, focus on these essentials:

### Must Have:
1. ✅ Updated contact info (email, website)
2. ✅ App icon (512x512)
3. ✅ Feature graphic (1024x500)
4. ✅ 2 phone screenshots minimum
5. ✅ Privacy policy hosted online
6. ✅ Signed release AAB file
7. ✅ Content rating completed
8. ✅ Data safety form completed

### Should Have:
9. 4-6 phone screenshots (better first impression)
10. Tablet screenshots (if supporting tablets)
11. Tested on real devices
12. Internal testing track for 1 week

### Nice to Have:
13. Promo video
14. Tablet screenshots
15. Multiple language support
16. Promotional graphics

---

## 📞 Need Help?

### Common Issues:

**"Privacy policy URL is invalid"**
- Ensure URL is publicly accessible (not localhost)
- Must be HTTPS (not HTTP)
- Should be a direct link (not requiring login)

**"App icon doesn't meet requirements"**
- Must be exactly 512x512 pixels
- Must be PNG format
- Must have alpha channel (transparency support)

**"Feature graphic rejected"**
- Can't contain device frames
- Can't be low resolution
- Text must be readable
- Should be 1024x500 exactly

**"Data safety form incomplete"**
- Every section must be answered
- If you collect email, you must disclose it
- Location data must be explained

### Resources:

- [Play Console Help](https://support.google.com/googleplay/android-developer)
- [Launch Checklist](https://developer.android.com/distribute/best-practices/launch/launch-checklist)
- [Data Safety Form Guide](https://support.google.com/googleplay/android-developer/answer/10787469)

---

## ✅ Ready to Submit!

Once all items are checked:

1. Log into [Google Play Console](https://play.google.com/console)
2. Navigate to your app
3. Go to "Production" → "Release" → "Create new release"
4. Upload your AAB file
5. Fill in release notes
6. Review everything one final time
7. Click "Review release"
8. Click "Start rollout to Production"

**Approval typically takes 1-3 days.**

Good luck! 🚀
