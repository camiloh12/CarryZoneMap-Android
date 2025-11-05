# Email Confirmation & SMTP Setup Guide

## Overview

Supabase's built-in SMTP service has severe limitations:
- Only 2-4 emails per hour
- Only sends to authorized team members
- No SLA guarantee
- **Not suitable for production**

This guide shows how to enable email confirmation and set up a custom SMTP provider to overcome these limits.

---

## Enabling Email Confirmation in Supabase

1. Go to [Supabase Dashboard](https://supabase.com/dashboard) → Your Project
2. Navigate to **Authentication > Providers** (or Settings > Auth)
3. Ensure **Enable email confirmations** is turned ON
4. Users will now receive a confirmation email when signing up

---

## SMTP Provider Pricing Comparison (2025)

| Provider | **Free Tier** | Free Limit | Paid Plans Start At | Best For |
|----------|---------------|------------|---------------------|----------|
| **🏆 Brevo** | ✅ Forever | **300/day (~9,000/month)** | $9/mo (5K emails) | **BEST FREE TIER** |
| **Resend** | ✅ Forever | 3,000/month | $20/mo | Modern dev experience |
| **AWS SES** | ✅ 12 months (or forever from EC2) | 3,000/month (or 62,000/month from EC2) | $0.10 per 1K emails | Most cost-effective at scale |
| **ZeptoMail** | ✅ One-time | 10,000 emails (one-time) | $2.50 per 10K credits | Transactional only |
| **Mailgun** | ✅ Forever | 100/day (~3,000/month) | $15/mo (10K emails) | Developers |
| **Postmark** | ✅ Forever | 100/month | $15/mo (10K emails) | High deliverability |
| **SendGrid** | ❌ Discontinued | Previously 100/day | $19.95/mo (50K emails) | Enterprise (no free tier) |

---

## 🏆 Recommended: Brevo (Best Free Tier)

**Why Brevo:**
- **300 emails per day** (9,000/month) - 3x more than Resend
- **Forever free** (no expiration)
- **100,000 contacts** included
- Full SMTP + API access
- Unlimited log retention
- No credit card required

### Brevo SMTP Setup

1. **Sign up at [Brevo](https://www.brevo.com/)**

2. **Get your SMTP credentials:**
   - Go to Brevo Dashboard → Settings → SMTP & API
   - Generate an SMTP key

3. **Configure in Supabase:**
   - Go to [Authentication Settings](https://supabase.com/dashboard/project/_/settings/auth)
   - Enable **Custom SMTP**
   - Enter the following:
     ```
     SMTP Host: smtp-relay.brevo.com
     SMTP Port: 587 (or 465 for SSL)
     SMTP User: Your Brevo email
     SMTP Pass: Your SMTP key from Brevo dashboard
     Sender Email: noreply@yourdomain.com (or your verified email)
     Sender Name: CCW Map
     ```

4. **Adjust Rate Limits:**
   - Go to [Authentication > Rate Limits](https://supabase.com/dashboard/project/_/auth/rate-limits)
   - Default is 30 emails/hour after custom SMTP setup
   - Adjust higher based on your needs

---

## Alternative: Resend (Best Developer Experience)

**Why Resend:**
- Developer-friendly API
- [Official Supabase documentation](https://resend.com/docs/send-with-supabase-smtp)
- React Email template support
- 3,000 free emails/month

### Resend SMTP Setup

1. **Sign up at [Resend](https://resend.com/)**

2. **Get API Key:**
   - Dashboard → API Keys → Create API Key

3. **Configure in Supabase:**
   ```
   SMTP Host: smtp.resend.com
   SMTP Port: 465 (or 587)
   SMTP User: resend
   SMTP Pass: Your API key (starts with re_)
   Sender Email: onboarding@yourdomain.com
   Sender Name: CCW Map
   ```

---

## Alternative: AWS SES (Most Cost-Effective at Scale)

**Why AWS SES:**
- **$0.10 per 1,000 emails** after free tier
- If hosting on EC2: **62,000 FREE emails/month forever**
- Best for high-volume production apps
- Industry-standard reliability

### AWS SES SMTP Setup

1. **Sign up for [AWS](https://aws.amazon.com/)**

2. **Set up SES:**
   - Go to AWS Console → Simple Email Service
   - Verify your sending domain or email
   - Request production access (moves you out of sandbox)
   - Create SMTP credentials

3. **Configure in Supabase:**
   ```
   SMTP Host: email-smtp.[region].amazonaws.com
   SMTP Port: 587 (or 465)
   SMTP User: Your SES SMTP username
   SMTP Pass: Your SES SMTP password
   Sender Email: noreply@yourdomain.com
   Sender Name: CCW Map
   ```

**Note:** AWS SES requires domain verification for production use.

---

## Recommendations by Use Case

### For Early Development/Testing
→ **Brevo** (9,000 free emails/month)

### For Small Production Apps (<3K emails/month)
→ **Resend** (modern API, great docs)

### For High-Volume Apps (>50K emails/month)
→ **AWS SES** (cheapest at scale: $5 per 50K emails)

### For Best Deliverability
→ **Postmark** (but limited free tier)

---

## Migration Path Recommendation

1. **Start:** Use **Brevo** free tier (9,000/month)
2. **Growth:** Stay on Brevo or upgrade to paid ($9/mo for 5K emails)
3. **Scale:** Migrate to **AWS SES** when volume exceeds 50K/month

---

## Important Configuration After SMTP Setup

### 1. Adjust Supabase Rate Limits
- Default after custom SMTP: 30 emails/hour
- Go to: [Auth > Rate Limits](https://supabase.com/dashboard/project/_/auth/rate-limits)
- Increase based on your expected signup rate

### 2. Set Up Email Templates
- Go to: [Auth > Email Templates](https://supabase.com/dashboard/project/_/auth/templates)
- Customize confirmation email, password reset, etc.
- Use your app name and branding

### 3. Configure DNS Records (for better deliverability)
- **SPF**: Add SPF record for your SMTP provider
- **DKIM**: Set up DKIM with your provider
- **DMARC**: Configure DMARC policy
- This prevents emails from going to spam

### 4. Test Email Delivery
```bash
# Test signup flow
# 1. Clear any existing test users
# 2. Sign up with a real email address
# 3. Check inbox (and spam folder)
# 4. Verify confirmation link works
```

---

## Troubleshooting

### Emails Not Sending
1. Check SMTP credentials are correct
2. Verify port number (587 or 465)
3. Check Supabase logs for error messages
4. Ensure sender email is verified with your provider

### Emails Going to Spam
1. Set up SPF, DKIM, DMARC records
2. Use a custom domain (not gmail.com)
3. Verify domain with SMTP provider
4. Keep email content simple (avoid promotional language)

### Rate Limit Errors
1. Check Supabase Auth rate limits settings
2. Verify SMTP provider's sending limits
3. Consider implementing CAPTCHA to prevent abuse

---

## Resources

- [Supabase SMTP Guide](https://supabase.com/docs/guides/auth/auth-smtp)
- [Brevo SMTP Setup](https://help.brevo.com/hc/en-us/articles/7924908994450)
- [Resend + Supabase Integration](https://resend.com/docs/send-with-supabase-smtp)
- [AWS SES Documentation](https://docs.aws.amazon.com/ses/)
- [Supabase Production Checklist](https://supabase.com/docs/guides/deployment/going-into-prod)

---

## Current Status for CarryZoneMap

- [ ] Email confirmation currently disabled in Supabase
- [ ] Using default Supabase SMTP (2 emails/hour limit)
- [ ] No custom SMTP provider configured
- [ ] Need to set up before production launch

## Next Steps

1. **Choose provider:** Start with Brevo (best free tier)
2. **Sign up:** Create account at brevo.com
3. **Get credentials:** Generate SMTP key in Brevo dashboard
4. **Configure Supabase:** Add SMTP settings in Auth settings
5. **Enable email confirmation:** Turn on in Auth > Providers
6. **Test:** Sign up with real email and verify confirmation works
7. **Adjust rate limits:** Increase from default 30/hour if needed
