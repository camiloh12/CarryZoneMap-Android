# Workload Identity Federation Setup Guide

This guide shows how to set up **Workload Identity Federation (WIF)** for GitHub Actions to deploy to Google Play Store **without service account keys**.

## Why Workload Identity Federation?

✅ **Advantages:**
- No service account keys to manage or rotate
- More secure (no long-lived credentials)
- Bypasses organization policies that block key creation
- Google's recommended approach
- Uses GitHub's OIDC tokens for authentication

## Prerequisites

1. **Google Play Console account** with an existing app
2. **Google Cloud project** (can be new or existing)
3. **GitHub repository** with Actions enabled
4. **gcloud CLI** installed (for setup commands)

## Setup Steps

### Part 1: Google Cloud Setup

#### Step 1: Enable Required APIs

```bash
# Set your project ID
export PROJECT_ID="your-gcp-project-id"
gcloud config set project $PROJECT_ID

# Enable required APIs
gcloud services enable iamcredentials.googleapis.com
gcloud services enable androidpublisher.googleapis.com
gcloud services enable sts.googleapis.com
```

#### Step 2: Create Service Account

```bash
# Create the service account
gcloud iam service-accounts create github-actions-deploy \
    --display-name="GitHub Actions Deploy" \
    --description="Service account for GitHub Actions to deploy to Play Store"

# Get the service account email
export SA_EMAIL="github-actions-deploy@${PROJECT_ID}.iam.gserviceaccount.com"
echo "Service Account Email: $SA_EMAIL"
```

#### Step 3: Create Workload Identity Pool

```bash
# Create the workload identity pool
gcloud iam workload-identity-pools create "github-pool" \
    --location="global" \
    --display-name="GitHub Actions Pool" \
    --description="Workload Identity Pool for GitHub Actions"

# Get the full pool ID
export WORKLOAD_IDENTITY_POOL_ID=$(gcloud iam workload-identity-pools describe "github-pool" \
    --location="global" \
    --format="value(name)")

echo "Workload Identity Pool ID: $WORKLOAD_IDENTITY_POOL_ID"
```

#### Step 4: Create Workload Identity Provider

```bash
# Replace YOUR_GITHUB_ORG and YOUR_GITHUB_REPO with your values
export GITHUB_REPO="YOUR_GITHUB_ORG/YOUR_GITHUB_REPO"  # e.g., "camiloh12/CarryZoneMap-Android"

# Create the OIDC provider for GitHub
gcloud iam workload-identity-pools providers create-oidc "github-provider" \
    --location="global" \
    --workload-identity-pool="github-pool" \
    --display-name="GitHub Provider" \
    --attribute-mapping="google.subject=assertion.sub,attribute.actor=assertion.actor,attribute.repository=assertion.repository,attribute.repository_owner=assertion.repository_owner" \
    --attribute-condition="assertion.repository_owner == '$(echo $GITHUB_REPO | cut -d'/' -f1)'" \
    --issuer-uri="https://token.actions.githubusercontent.com"

# Get the full provider ID
export WORKLOAD_IDENTITY_PROVIDER=$(gcloud iam workload-identity-pools providers describe "github-provider" \
    --location="global" \
    --workload-identity-pool="github-pool" \
    --format="value(name)")

echo "Workload Identity Provider: $WORKLOAD_IDENTITY_PROVIDER"
```

#### Step 5: Grant Service Account Access to Workload Identity

```bash
# Allow GitHub Actions to impersonate the service account
gcloud iam service-accounts add-iam-policy-binding $SA_EMAIL \
    --role="roles/iam.workloadIdentityUser" \
    --member="principalSet://iam.googleapis.com/${WORKLOAD_IDENTITY_POOL_ID}/attribute.repository/${GITHUB_REPO}"
```

#### Step 6: Grant Play Store Permissions

The service account needs access to your Play Console app:

1. **Go to [Google Play Console](https://play.google.com/console/)**
2. Select your app
3. Go to **Setup** → **API access**
4. Under **Service accounts**, find `github-actions-deploy@...`
5. Click **Manage Play Console permissions**
6. Grant permissions:
   - ✅ **Releases** → Manage testing track releases
   - ✅ **App access** → View app information
7. Click **Invite user** → **Send invite**

#### Step 7: Get Values for GitHub Secrets

```bash
# Print all values you need for GitHub
echo "=== Copy these values to GitHub Secrets ==="
echo ""
echo "WORKLOAD_IDENTITY_PROVIDER:"
echo "$WORKLOAD_IDENTITY_PROVIDER"
echo ""
echo "SERVICE_ACCOUNT_EMAIL:"
echo "$SA_EMAIL"
echo ""
echo "GCP_PROJECT_ID:"
echo "$PROJECT_ID"
```

### Part 2: GitHub Setup

#### Step 1: Add GitHub Secrets

Go to your GitHub repository → **Settings** → **Secrets and variables** → **Actions**

Add these secrets:

| Secret Name | Value | Example |
|-------------|-------|---------|
| `WORKLOAD_IDENTITY_PROVIDER` | From Step 7 above | `projects/123.../locations/global/workloadIdentityPools/github-pool/providers/github-provider` |
| `SERVICE_ACCOUNT_EMAIL` | From Step 7 above | `github-actions-deploy@your-project.iam.gserviceaccount.com` |
| `GCP_PROJECT_ID` | Your GCP project ID | `your-gcp-project-id` |

**Keep these existing secrets** (still needed):
- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`
- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`
- `MAPTILER_API_KEY`

**Remove this secret** (no longer needed):
- ~~`GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`~~ - Can be deleted

#### Step 2: Update Workflow

The workflow has been updated to use Workload Identity Federation. See `.github/workflows/deploy.yml`.

Key changes:
- Uses `google-github-actions/auth@v2` action
- Authenticates with OIDC tokens instead of service account keys
- No more service account JSON key needed

### Part 3: Verification

#### Test the Setup

1. **Trigger the workflow manually:**
   - Go to **Actions** tab
   - Select **Deploy to Google Play (Closed Testing - Alpha)**
   - Click **Run workflow**
   - Monitor the execution

2. **Check authentication step:**
   - Should see: "Authenticating with Workload Identity Federation"
   - Should complete without errors

3. **Check deployment:**
   - Build and upload should succeed
   - Check Google Play Console for new release

## Troubleshooting

### "Failed to generate Google Cloud access token"

**Cause:** Workload Identity Provider not configured correctly

**Solution:**
1. Verify the provider exists:
   ```bash
   gcloud iam workload-identity-pools providers describe "github-provider" \
       --location="global" \
       --workload-identity-pool="github-pool"
   ```
2. Check attribute condition matches your repository owner
3. Ensure `WORKLOAD_IDENTITY_PROVIDER` secret is correct (full path)

### "Permission denied" on Play Store upload

**Cause:** Service account lacks Play Console permissions

**Solution:**
1. Go to Play Console → **Setup** → **API access**
2. Find your service account
3. Verify it has "Manage testing track releases" permission
4. Wait 5-10 minutes for permissions to propagate

### "Workload Identity Pool not found"

**Cause:** Pool created in wrong project or not yet created

**Solution:**
1. Verify current project:
   ```bash
   gcloud config get-value project
   ```
2. List all pools:
   ```bash
   gcloud iam workload-identity-pools list --location=global
   ```
3. Recreate if necessary

### "Repository not authorized"

**Cause:** Attribute condition doesn't match your repository

**Solution:**
1. Check the attribute condition:
   ```bash
   gcloud iam workload-identity-pools providers describe "github-provider" \
       --location="global" \
       --workload-identity-pool="github-pool" \
       --format="value(attributeCondition)"
   ```
2. Should match: `assertion.repository_owner == 'your-github-username'`
3. Update if needed:
   ```bash
   gcloud iam workload-identity-pools providers update-oidc "github-provider" \
       --location="global" \
       --workload-identity-pool="github-pool" \
       --attribute-condition="assertion.repository_owner == 'YOUR_USERNAME'"
   ```

## Security Best Practices

1. ✅ **Limit repository access** - The attribute condition ensures only your specific repository can authenticate
2. ✅ **Use branch restrictions** - Consider adding branch conditions (e.g., only `master`)
3. ✅ **Audit regularly** - Monitor service account usage in Cloud Console
4. ✅ **Follow least privilege** - Only grant necessary Play Console permissions
5. ✅ **Enable Cloud Audit Logs** - Track authentication events

## Advanced Configuration

### Restrict to Specific Branch

Update the attribute condition to only allow deployments from `master`:

```bash
gcloud iam workload-identity-pools providers update-oidc "github-provider" \
    --location="global" \
    --workload-identity-pool="github-pool" \
    --attribute-condition="assertion.repository_owner == 'YOUR_USERNAME' && assertion.ref == 'refs/heads/master'"
```

### Multiple Repositories

To allow multiple repositories, use OR conditions:

```bash
--attribute-condition="assertion.repository_owner == 'YOUR_USERNAME' && (assertion.repository == 'ORG/REPO1' || assertion.repository == 'ORG/REPO2')"
```

### Add Custom Attributes

Map additional GitHub token claims:

```bash
--attribute-mapping="google.subject=assertion.sub,attribute.actor=assertion.actor,attribute.repository=assertion.repository,attribute.ref=assertion.ref,attribute.sha=assertion.sha"
```

## Migration from Service Account Keys

If you previously used service account keys:

1. ✅ Update workflow to use WIF (done)
2. ✅ Add new GitHub secrets (WIF-related)
3. ✅ Test deployment with WIF
4. ✅ Remove old `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` secret
5. ✅ Delete any downloaded service account key files
6. ✅ (Optional) Disable service account keys in Google Cloud:
   ```bash
   gcloud iam service-accounts keys list --iam-account=$SA_EMAIL
   # Delete any existing keys
   gcloud iam service-accounts keys delete KEY_ID --iam-account=$SA_EMAIL
   ```

## Cost

✅ **Workload Identity Federation is FREE**
- No charges for authentication
- No charges for token exchanges
- Same free Play Store API usage as before

## Resources

- [Workload Identity Federation Documentation](https://cloud.google.com/iam/docs/workload-identity-federation)
- [GitHub OIDC Documentation](https://docs.github.com/en/actions/deployment/security-hardening-your-deployments/about-security-hardening-with-openid-connect)
- [google-github-actions/auth](https://github.com/google-github-actions/auth)

---

**Setup complete!** Your CD pipeline now uses secure, keyless authentication. 🎉
