# Email Confirmation Setup Guide

This guide explains how to handle email confirmation for users who might click confirmation links from desktop or mobile devices.

## Current Setup

The app uses a custom deep link scheme: `com.carryzonemap.app://auth/callback`

**What works:**
- ✅ User clicks confirmation link on mobile device → App opens and logs them in automatically

**What doesn't work:**
- ❌ User clicks confirmation link on desktop → Browser can't open the custom scheme

## Solutions

### Option 1: Simple - Email Instructions ⭐ Easiest (Recommended for MVP)

**Pros:** Zero code changes, works immediately
**Cons:** User experience isn't ideal

#### Implementation Steps:

1. **Go to Supabase Dashboard**: https://supabase.com/dashboard/project/gqbxloaqamokbolcvesg

2. **Navigate to**: **Authentication** → **Email Templates** → **Confirm signup**

3. **Edit the email template** to include instructions:

```html
<h2>Confirm Your Email</h2>

<p>Thanks for signing up for CCW Map! Please confirm your email address to complete your account setup.</p>

<p><strong>📱 Important:</strong> Please open this email on your mobile device where you have the CCW Map app installed.</p>

<p>If you're viewing this on a computer, please:</p>
<ol>
  <li>Check this email on your phone</li>
  <li>Tap the button below to confirm your account</li>
  <li>The CCW Map app will open automatically</li>
</ol>

{{ .ConfirmationURL }}

<p>Or copy and paste this link into your mobile browser:</p>
<p style="word-break: break-all;">{{ .ConfirmationURL }}</p>

<p>If you didn't create an account, you can safely ignore this email.</p>
```

4. **Click "Save"**

---

### Option 2: Better - Add Fallback Web Page ⭐⭐ Recommended for Production

Create a web page that handles both desktop and mobile users gracefully.

#### What You Need:
- A hosting service (GitHub Pages, Vercel, Netlify, Firebase Hosting, etc.)
- A custom domain (optional but recommended)

#### Implementation Steps:

**Step 1: Create the fallback page**

Create `auth-callback.html`:

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Confirm Your CCW Map Account</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 20px;
        }

        .container {
            background: white;
            max-width: 500px;
            width: 100%;
            border-radius: 12px;
            box-shadow: 0 10px 40px rgba(0,0,0,0.2);
            padding: 40px;
            text-align: center;
        }

        .checkmark {
            width: 80px;
            height: 80px;
            border-radius: 50%;
            background: #4CAF50;
            display: flex;
            align-items: center;
            justify-content: center;
            margin: 0 auto 20px;
        }

        .checkmark::after {
            content: "✓";
            color: white;
            font-size: 50px;
            font-weight: bold;
        }

        h1 {
            color: #333;
            margin-bottom: 10px;
            font-size: 28px;
        }

        .subtitle {
            color: #666;
            margin-bottom: 30px;
            font-size: 16px;
        }

        .instructions {
            background: #f8f9fa;
            border-radius: 8px;
            padding: 24px;
            margin: 20px 0;
            text-align: left;
        }

        .instructions h3 {
            color: #333;
            margin-bottom: 16px;
            font-size: 18px;
        }

        .instruction-item {
            display: flex;
            align-items: flex-start;
            margin-bottom: 12px;
            padding: 12px;
            background: white;
            border-radius: 6px;
        }

        .instruction-icon {
            flex-shrink: 0;
            width: 32px;
            height: 32px;
            border-radius: 50%;
            background: #667eea;
            color: white;
            display: flex;
            align-items: center;
            justify-content: center;
            margin-right: 12px;
            font-weight: bold;
        }

        .instruction-text {
            flex: 1;
            color: #555;
            line-height: 1.5;
        }

        .instruction-text strong {
            color: #333;
            display: block;
            margin-bottom: 4px;
        }

        .button {
            background: #667eea;
            color: white;
            padding: 14px 32px;
            border: none;
            border-radius: 6px;
            font-size: 16px;
            font-weight: 600;
            cursor: pointer;
            text-decoration: none;
            display: inline-block;
            margin-top: 20px;
            transition: background 0.3s;
        }

        .button:hover {
            background: #5568d3;
        }

        .footer {
            margin-top: 24px;
            padding-top: 24px;
            border-top: 1px solid #eee;
            color: #999;
            font-size: 14px;
        }

        .loading {
            display: none;
            margin: 20px 0;
        }

        .spinner {
            border: 3px solid #f3f3f3;
            border-top: 3px solid #667eea;
            border-radius: 50%;
            width: 40px;
            height: 40px;
            animation: spin 1s linear infinite;
            margin: 0 auto;
        }

        @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="checkmark"></div>
        <h1>Email Confirmed!</h1>
        <p class="subtitle">Your CCW Map account has been verified.</p>

        <div class="instructions">
            <h3>📱 Next Steps</h3>

            <div class="instruction-item">
                <div class="instruction-icon">1</div>
                <div class="instruction-text">
                    <strong>On Mobile Device</strong>
                    The CCW Map app should open automatically. If not, tap the button below.
                </div>
            </div>

            <div class="instruction-item">
                <div class="instruction-icon">2</div>
                <div class="instruction-text">
                    <strong>On Desktop</strong>
                    Open the CCW Map app on your phone and sign in with your email and password.
                </div>
            </div>
        </div>

        <div class="loading">
            <div class="spinner"></div>
            <p style="margin-top: 10px; color: #666;">Opening app...</p>
        </div>

        <button class="button" onclick="tryOpenApp()">Open CCW Map App</button>

        <div class="footer">
            Need help? Contact support or check the app for more information.
        </div>
    </div>

    <script>
        function tryOpenApp() {
            // Show loading indicator
            document.querySelector('.button').style.display = 'none';
            document.querySelector('.loading').style.display = 'block';

            // Get the tokens from URL hash
            const hash = window.location.hash.substring(1);

            if (hash) {
                // Try to open the deep link with tokens
                const deepLink = `com.carryzonemap.app://auth/callback#${hash}`;
                window.location.href = deepLink;

                // After 3 seconds, restore button and show message
                setTimeout(() => {
                    document.querySelector('.loading').style.display = 'none';
                    document.querySelector('.button').style.display = 'inline-block';

                    // Check if we're on mobile
                    if (/Android|iPhone|iPad|iPod/i.test(navigator.userAgent)) {
                        alert('If the app didn\'t open, please install CCW Map from the Play Store first.');
                    } else {
                        alert('Please open CCW Map on your mobile device and sign in.');
                    }
                }, 3000);
            } else {
                alert('Invalid confirmation link. Please use the link from your email.');
                document.querySelector('.loading').style.display = 'none';
                document.querySelector('.button').style.display = 'inline-block';
            }
        }

        // Automatically try to open app on page load (mobile devices only)
        window.addEventListener('load', function() {
            if (/Android|iPhone|iPad|iPod/i.test(navigator.userAgent)) {
                // Give user time to see the confirmation message
                setTimeout(() => {
                    tryOpenApp();
                }, 1500);
            }
        });
    </script>
</body>
</html>
```

**Step 2: Host the page**

Deploy to one of these services:

- **GitHub Pages** (Free):
  1. Create a repository (e.g., `ccw-map-web`)
  2. Add `auth-callback.html` as `index.html` in a `/auth/callback/` folder
  3. Enable GitHub Pages in repository settings
  4. URL: `https://yourusername.github.io/ccw-map-web/auth/callback/`

- **Vercel** (Free, easier):
  1. Create a Vercel account
  2. Upload your HTML file
  3. Deploy with one click
  4. URL: `https://your-project.vercel.app/auth/callback`

- **Netlify** (Free, drag & drop):
  1. Create a Netlify account
  2. Drag and drop your HTML file
  3. URL: `https://your-site.netlify.app/auth/callback`

**Step 3: Configure Supabase**

1. Go to **Supabase Dashboard** → **Authentication** → **URL Configuration**

2. **Add TWO redirect URLs**:
   ```
   com.carryzonemap.app://auth/callback
   https://your-site.vercel.app/auth/callback
   ```

3. In your email template, update the confirmation URL to use the HTTPS link:
   ```
   https://your-site.vercel.app/auth/callback
   ```

4. **Save changes**

**Step 4: Update MainActivity to handle both schemes**

The current code already handles `com.carryzonemap.app://auth/callback`. To also handle HTTPS links, update `AndroidManifest.xml`:

```xml
<intent-filter android:autoVerify="true">
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />

    <!-- HTTPS deep link for web fallback -->
    <data
        android:scheme="https"
        android:host="your-site.vercel.app"
        android:pathPrefix="/auth/callback" />
</intent-filter>
```

And update `MainActivity.handleDeepLink()`:

```kotlin
// Check if this is an auth callback deep link (custom scheme OR HTTPS)
val isAuthDeepLink = (data.scheme == "com.carryzonemap.app" && data.host == "auth") ||
                     (data.scheme == "https" && data.path?.startsWith("/auth/callback") == true)

if (isAuthDeepLink) {
    // ... existing code
}
```

---

### Option 3: Best - Android App Links (HTTPS Deep Links) ⭐⭐⭐ Production-Ready

Use **verified HTTPS URLs** that work as both web pages AND deep links.

**Requirements:**
- A custom domain you control (e.g., `carryzonemap.com`)
- Ability to host files at `/.well-known/` path

**How it works:**
- Link: `https://carryzonemap.com/auth/callback#...`
- On Android with app installed: Opens app directly (no browser dialog)
- On desktop or without app: Shows web page with instructions

#### Implementation Steps:

**Step 1: Get your app signing key SHA-256**

```bash
# For debug build (development)
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android

# For release build (production)
keytool -list -v -keystore /path/to/your/release.keystore -alias your_alias
```

Copy the **SHA256 fingerprint** (looks like: `AB:CD:EF:12:34:...`)

**Step 2: Create assetlinks.json**

Host this file at `https://carryzonemap.com/.well-known/assetlinks.json`:

```json
[{
  "relation": ["delegate_permission/common.handle_all_urls"],
  "target": {
    "namespace": "android_app",
    "package_name": "com.carryzonemap.app",
    "sha256_cert_fingerprints": [
      "AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12"
    ]
  }
}]
```

**Important:** Replace the SHA256 fingerprint with your actual value (remove colons, keep uppercase).

**Step 3: Update AndroidManifest.xml**

Replace the custom scheme intent filter with:

```xml
<!-- Android App Links (verified HTTPS deep links) -->
<intent-filter android:autoVerify="true">
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />

    <data
        android:scheme="https"
        android:host="carryzonemap.com"
        android:pathPrefix="/auth/callback" />
</intent-filter>
```

**Step 4: Update MainActivity.kt**

```kotlin
private fun handleDeepLink(intent: Intent?) {
    val data = intent?.data ?: return

    // Check if this is an App Links callback
    if (data.scheme == "https" &&
        data.host == "carryzonemap.com" &&
        data.path?.startsWith("/auth/callback") == true) {

        Timber.d("Received App Links auth callback")

        lifecycleScope.launch {
            try {
                val fragment = data.fragment
                if (!fragment.isNullOrEmpty()) {
                    val params = fragment.split("&").associate {
                        val parts = it.split("=", limit = 2)
                        if (parts.size == 2) parts[0] to parts[1] else parts[0] to ""
                    }

                    val accessToken = params["access_token"]
                    val refreshToken = params["refresh_token"]

                    if (!accessToken.isNullOrEmpty() && !refreshToken.isNullOrEmpty()) {
                        Timber.d("Importing session from email confirmation")
                        auth.importAuthToken(accessToken, refreshToken)
                        Timber.d("Email confirmation successful - user is now authenticated")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to handle App Links: ${e.message}")
            }
        }
    }
}
```

**Step 5: Update SupabaseModule.kt**

```kotlin
install(Auth) {
    alwaysAutoRefresh = true
    autoLoadFromStorage = true
    // Use your domain for App Links
    scheme = "https"
    host = "carryzonemap.com"
}
```

**Step 6: Configure Supabase**

1. **Authentication** → **URL Configuration** → **Redirect URLs**
   ```
   https://carryzonemap.com/auth/callback
   ```

2. **Save changes**

**Step 7: Host the callback page**

At `https://carryzonemap.com/auth/callback/index.html`, use the HTML from Option 2.

**Step 8: Test App Links verification**

```bash
# Install the app
./gradlew installDebug

# Check if App Links are verified
adb shell pm get-app-links com.carryzonemap.app

# Should show: "carryzonemap.com" with verification status "verified"
```

If not verified, check:
- `assetlinks.json` is accessible at `https://carryzonemap.com/.well-known/assetlinks.json`
- JSON is valid (no syntax errors)
- SHA256 fingerprint matches your signing key
- Domain uses HTTPS (not HTTP)

---

## Testing Each Option

### Option 1 (Email Instructions Only):
1. Sign up with a new email
2. Check email on desktop → Should see instructions to open on phone
3. Open email on phone → Click link → App opens ✅

### Option 2 (Web Fallback):
1. Sign up with a new email
2. Click link on desktop → Web page opens with instructions ✅
3. Click link on phone → Web page opens briefly → App opens ✅

### Option 3 (App Links):
1. Sign up with a new email
2. Click link on desktop → Web page opens with instructions ✅
3. Click link on phone → App opens directly (no browser) ✅✅✅

---

## Current Supabase Configuration

**Required Settings:**

1. **Go to**: https://supabase.com/dashboard/project/gqbxloaqamokbolcvesg

2. **Navigate to**: **Authentication** → **URL Configuration**

3. **Add Redirect URL(s)**:

   For Option 1 (current setup):
   ```
   com.carryzonemap.app://auth/callback
   ```

   For Option 2 (with fallback):
   ```
   com.carryzonemap.app://auth/callback
   https://your-site.vercel.app/auth/callback
   ```

   For Option 3 (App Links):
   ```
   https://carryzonemap.com/auth/callback
   ```

4. **Site URL**: Set to your domain or leave as default

5. **Save changes**

---

## Recommendation

**For MVP/Beta:** Use **Option 1** (email instructions) - quickest to implement

**For Production:** Use **Option 3** (App Links) - best user experience and industry standard

**Middle Ground:** Use **Option 2** (web fallback) - good UX without needing a custom domain (can use Vercel/Netlify subdomain)

---

## Troubleshooting

### Link opens browser instead of app
- Check that redirect URL is configured in Supabase
- Verify AndroidManifest intent filter is correct
- For App Links: Ensure `assetlinks.json` is accessible and valid

### App opens but doesn't log in
- Check logcat for errors: `adb logcat -s MainActivity SupabaseAuthRepository`
- Verify tokens are in the URL fragment (after `#`)
- Test with a fresh signup to get a new confirmation link

### Desktop users see error page
- Implement Option 2 or Option 3 with a fallback web page
- Update email template with clear instructions

---

## Additional Resources

- [Android App Links Guide](https://developer.android.com/training/app-links)
- [Android Deep Linking Best Practices](https://developer.android.com/training/app-links/deep-linking)
- [Supabase Auth Deep Linking](https://supabase.com/docs/guides/auth/redirect-urls)
