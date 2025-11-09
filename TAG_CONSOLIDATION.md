# Carry Restriction Tag Consolidation

## Analysis Summary

**Original count:** 41 tags across 6 categories
**Recommended count:** 10 core tags
**Reduction:** 76% fewer options

## Recommended Tag Set (10 Tags)

### Federal Restrictions (2 tags)

1. **Federal Government Property**
   - **Consolidates:** Federal Facility, Federal Courthouse, U.S. Post Office (Building & Parking), VA Facility, Military Installation, National Park Buildings, Indian Reservation
   - **User-facing label:** "Federal Government Property"
   - **User-facing description:** "Federal building, post office, military base, VA facility, courthouse, or tribal land"
   - **Rationale:** All prohibited under federal law or separate federal jurisdiction (18 U.S.C. § 930, 38 CFR § 1.218, postal regs, tribal sovereignty). Tribal lands are technically separate nations but operate under federal-level jurisdiction in practice.

2. **Airport Secure Area**
   - **Consolidates:** Airport (Sterile Area)
   - **User-facing label:** "Airport Secure Area"
   - **User-facing description:** "Past TSA security checkpoint"
   - **Rationale:** Very common scenario, distinct enough from other federal restrictions to call out separately

### State/Local Government (1 tag)

3. **State/Local Government Property**
   - **Consolidates:** State Government Building, State Courthouse, Local Ordinance (Weak Preemption), Polling Place (State Law), Polling Place (Open Carry Prohibited)
   - **User-facing label:** "State/Local Government Property"
   - **User-facing description:** "State/local government building, courthouse, or polling place"
   - **Rationale:** All state/local-level prohibitions on government property. Polling places are typically in government buildings or schools, and temporary restriction doesn't warrant separate tag.

### Education (2 tags)

4. **School (K-12)**
   - **Consolidates:** School (K-12) Property, School Zone (1,000-ft / Non-Resident Permit)
   - **User-facing label:** "School (K-12)"
   - **User-facing description:** "Elementary, middle, or high school campus"
   - **Rationale:** Federal GFSZA + state laws. Merge property vs. zone since both are restricted and most users won't know the 1,000-ft boundary anyway.

5. **College/University**
   - **Consolidates:** College / University (Dorm/Class)
   - **User-facing label:** "College/University"
   - **User-facing description:** "College or university campus"
   - **Rationale:** Separate from K-12 since laws differ significantly by state. Don't distinguish dorm vs. classroom (too granular).

### State-Regulated Venues (4 tags)

6. **Bar/Alcohol Establishment**
   - **Consolidates:** Bar (>51% Alcohol Sales), Establishment Serving Alcohol (If Consuming)
   - **User-facing label:** "Bar/Alcohol Establishment"
   - **User-facing description:** "Bar, restaurant, or venue with alcohol restrictions"
   - **Rationale:** Merge 51% rule vs. consumption rule - too state-specific. Users just need to know "alcohol-related restriction."

7. **Healthcare Facility**
   - **Consolidates:** Hospital (State Law), Childcare Facility (State Law)
   - **User-facing label:** "Healthcare Facility"
   - **User-facing description:** "Hospital, medical clinic, or childcare facility"
   - **Rationale:** Both are healthcare/care facilities, often governed by similar state laws

8. **Place of Worship**
   - **Consolidates:** Place of Worship (Default Prohibited)
   - **User-facing label:** "Place of Worship"
   - **User-facing description:** "Church, mosque, temple, or religious facility"
   - **Rationale:** Keep separate - unique legal status where some states require permission from the institution

9. **Sports/Entertainment Venue**
   - **Consolidates:** Sports Arena / Stadium (State Law), Amusement Park
   - **User-facing label:** "Sports/Entertainment Venue"
   - **User-facing description:** "Sports stadium, arena, concert hall, or amusement park"
   - **Rationale:** Similar large-venue entertainment contexts, often state-regulated or private policy

### Private Property (1 tag)

10. **Private Property**
    - **Consolidates:** Private Property (Posted), Posted Sign (No Force of Law), Posted Sign (Force of Law), Posted Sign (Texas 30.05/30.06/30.07), "No Guns" Sign, Employer Policy (Inside Building), Employer Policy (Company Vehicle), Verbal Notification by Staff
    - **User-facing label:** "Private Property"
    - **User-facing description:** "Private business, workplace, or property restricting carry"
    - **Rationale:** Users don't need to distinguish between sign types (30.06 vs. 30.07, force of law vs. request), employer vs. general business, or signage vs. verbal notification. Just knowing "private restriction" is sufficient for mapping purposes.

---

## Enforcement Details (Optional Secondary System)

**Recommendation:** Make these *optional checkboxes* rather than primary tags, since they describe "how" enforcement works, not "why" carry is prohibited.

### Optional Enforcement Indicators (Checkboxes)

- **☐ Active security screening** - Metal detectors, bag checks, or body scanners in use
- **☐ Posted signage visible** - "No guns" or similar signs displayed

**Alternative:** Drop enforcement indicators entirely and keep the UI ultra-simple. Users can add this info in a notes field if needed.

---

## Dropped Distinctions (Intentionally Simplified)

These nuances were removed to create a manageable tag set:

- ❌ Federal agency type (VA vs. Post Office vs. Military vs. Tribal)
- ❌ State vs. county vs. city ordinances
- ❌ Polling place as separate from government property
- ❌ School property vs. 1,000-ft zone
- ❌ College dorm vs. classroom
- ❌ Bar 51% rule vs. consumption prohibition
- ❌ Hospital vs. childcare (merged to healthcare)
- ❌ Stadium vs. amusement park
- ❌ Texas sign code variations (30.05/06/07)
- ❌ Sign force-of-law vs. request-only
- ❌ Open carry vs. concealed carry distinctions
- ❌ Employer policy vs. general private property
- ❌ Posted signage vs. verbal notification
- ❌ Specific security screening methods
- ❌ Post Office building vs. parking lot

**Rationale:** These distinctions are important for legal analysis but overwhelming for average users creating pins on a map. The 10 consolidated tags communicate the essential "what type of place" users need to know.

---

## Implementation in App

### UI Design (Recommended)

```
Create Pin: [POI Name]

Location status:
○ Green (Carry allowed/friendly)
○ Yellow (Uncertain)
● Red (No carry/prohibited)

Why is carry restricted? (Required for Red pins)

[Dropdown menu]
- Federal Government Property
- Airport Secure Area
- State/Local Government Property
- School (K-12)
- College/University
- Bar/Alcohol Establishment
- Healthcare Facility
- Place of Worship
- Sports/Entertainment Venue
- Private Property

Optional details:
☐ Active security screening
☐ Posted signage visible

[Create] [Cancel]
```

### Database Schema

```kotlin
// Domain model
data class Pin(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val status: PinStatus,
    val restrictionTag: RestrictionTag? = null,  // Required if status == RED
    val hasSecurityScreening: Boolean = false,   // Optional
    val hasPostedSignage: Boolean = false,       // Optional
    val createdBy: String,
    val createdAt: Instant,
    val lastModified: Instant
)

enum class PinStatus {
    GREEN,  // Carry allowed/friendly
    YELLOW, // Uncertain
    RED     // No carry/prohibited
}

enum class RestrictionTag {
    FEDERAL_PROPERTY,
    AIRPORT_SECURE,
    STATE_LOCAL_GOVT,
    SCHOOL_K12,
    COLLEGE_UNIVERSITY,
    BAR_ALCOHOL,
    HEALTHCARE,
    PLACE_OF_WORSHIP,
    SPORTS_ENTERTAINMENT,
    PRIVATE_PROPERTY
}
```

### Room Entity (Local Database)

```kotlin
@Entity(tableName = "pins")
data class PinEntity(
    @PrimaryKey val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val status: String,  // "GREEN", "YELLOW", "RED"
    val restrictionTag: String? = null,  // Enum name or null
    val hasSecurityScreening: Boolean = false,
    val hasPostedSignage: Boolean = false,
    val createdBy: String,
    val createdAt: Long,
    val lastModified: Long
)
```

### Supabase Schema (Cloud Database)

```sql
-- Add columns to existing pins table
ALTER TABLE pins
ADD COLUMN restriction_tag TEXT,
ADD COLUMN has_security_screening BOOLEAN DEFAULT FALSE,
ADD COLUMN has_posted_signage BOOLEAN DEFAULT FALSE;

-- Optional: Add constraint to ensure RED pins have a tag
ALTER TABLE pins
ADD CONSTRAINT check_red_pin_has_tag
CHECK (
    status != 'RED' OR restriction_tag IS NOT NULL
);

-- Optional: Create enum type for better validation
CREATE TYPE restriction_tag_type AS ENUM (
    'FEDERAL_PROPERTY',
    'AIRPORT_SECURE',
    'STATE_LOCAL_GOVT',
    'SCHOOL_K12',
    'COLLEGE_UNIVERSITY',
    'BAR_ALCOHOL',
    'HEALTHCARE',
    'PLACE_OF_WORSHIP',
    'SPORTS_ENTERTAINMENT',
    'PRIVATE_PROPERTY'
);

ALTER TABLE pins
ALTER COLUMN restriction_tag TYPE restriction_tag_type
USING restriction_tag::restriction_tag_type;
```

---

## User Stories Validation

**Story 1:** "I'm at a Starbucks with a 'No Guns' sign"
- Status: Red
- Tag: **Private Property** ✓
- Optional: Check "Posted signage visible"

**Story 2:** "I'm at the post office"
- Status: Red
- Tag: **Federal Government Property** ✓

**Story 3:** "I'm at a high school football game on campus"
- Status: Red
- Tag: **School (K-12)** ✓

**Story 4:** "I'm at a VA hospital"
- Status: Red
- Tag: **Federal Government Property** ✓ (or **Healthcare Facility** - user chooses based on context)

**Story 5:** "I'm at a bar with metal detectors"
- Status: Red
- Tag: **Bar/Alcohol Establishment** ✓
- Optional: Check "Active security screening"

**Story 6:** "I'm at a county courthouse"
- Status: Red
- Tag: **State/Local Government Property** ✓

**Story 7:** "I'm voting at a polling place"
- Status: Red
- Tag: **State/Local Government Property** ✓

**Story 8:** "My employer doesn't allow guns in the office"
- Status: Red
- Tag: **Private Property** ✓

**Story 9:** "This gun shop is CCW-friendly"
- Status: Green
- Tag: (none - not required for green pins)

**Story 10:** "I'm not sure if this mall allows carry"
- Status: Yellow
- Tag: (none - optional for yellow pins)

All common scenarios covered with 10 clear, intuitive options.

---

## UX Flow Recommendations

### For Green Pins
- Don't require a restriction tag
- Keep creation fast and simple
- Focus on "carry allowed/friendly" use case

### For Yellow Pins
- Make restriction tag optional
- This represents uncertainty, so forcing a reason doesn't make sense
- Users might add notes explaining the uncertainty

### For Red Pins
- **Require** restriction tag selection
- This provides the most value to other users
- Validates the prohibition claim
- Optional enforcement checkboxes provide additional context

### Tag Display on Map
When users tap a red pin, show:
```
[POI Name]
Status: No Carry
Reason: Federal Government Property
⚠ Active security screening
📋 Posted signage visible
Created by: [User] on [Date]
```

This gives users actionable information at a glance.

---

## Migration Strategy

### For Existing Pins Without Tags

If you already have pins in production without restriction tags:

1. **Backfill options:**
   - Set all existing RED pins to `restrictionTag = null` (allow null temporarily)
   - Add UI banner: "Help improve this pin - add restriction details"
   - Allow users to edit and add tags retroactively

2. **AI/ML inference (future):**
   - Use POI type + name to guess likely tag
   - "Courthouse" → STATE_LOCAL_GOVT
   - "Elementary School" → SCHOOL_K12
   - "Church" → PLACE_OF_WORSHIP
   - Present as suggestion, require user confirmation

3. **Gradual enforcement:**
   - Phase 1: Make tags optional for all pins (encourage adoption)
   - Phase 2: Require tags for new RED pins only
   - Phase 3: Backfill campaign or automated inference for old pins

---

## Analytics & Insights

With this tag system, you can generate valuable insights:

### For Users
- "10 federal properties within 5 miles"
- "Nearest CCW-friendly restaurant: 0.3 miles"
- "Schools in this area: 3 (K-12), 1 (University)"

### For Developers
- Most common restriction type by region
- Which POI types correlate with which tags
- Data quality: % of RED pins with tags filled in

### For Community
- Heat maps by restriction type
- "Most restrictive city" analysis
- Trend tracking (are businesses adding more restrictions?)

---

## Localization Considerations

If expanding internationally:

- Tag names/descriptions can be translated
- Enum values stay English (code-level)
- Some tags may not apply in other countries (e.g., GFSZA is US-only)
- May need country-specific tag sets

For now, this 10-tag system is US-centric, which aligns with CCW being a US legal framework.

---

## Summary

**Final recommendation: 10 core restriction tags** covering all major prohibition categories, with optional enforcement detail checkboxes.

This balances:
- ✅ **Simplicity** - Small enough to not overwhelm users
- ✅ **Coverage** - Handles all common real-world scenarios
- ✅ **Legal accuracy** - Maps to actual restriction frameworks
- ✅ **UX** - Quick pin creation, valuable information display
- ✅ **Analytics** - Enables insights and filtering

The aggressive consolidation (76% reduction) makes the app approachable for casual users while preserving enough detail for the data to be actionable.
