# Map Tile Provider Options for CarryZoneMap

## Problem
Current MapTiler `streets-v2` style doesn't show enough business names and POI labels at high zoom levels.

## Best Options (Ranked)

### 1. MapTiler - Switch to More Detailed Style ⭐ (Easiest)
**Effort:** 1 minute - just change one line

Since you already have MapTiler configured, simply change the style URL in `MapScreen.kt` line 149:

```kotlin
// Current:
val styleUrl = "https://api.maptiler.com/maps/streets-v2/style.json?key=$apiKey"

// Better options for POI density:
val styleUrl = "https://api.maptiler.com/maps/basic-v2/style.json?key=$apiKey"  // More labels
// OR
val styleUrl = "https://api.maptiler.com/maps/bright-v2/style.json?key=$apiKey"  // High contrast + labels
// OR
val styleUrl = "https://api.maptiler.com/maps/topo-v2/style.json?key=$apiKey"  // Most detailed
```

**Pros:**
- Zero infrastructure change
- No new API keys needed
- Same billing structure

**Cons:**
- Still limited by MapTiler's free tier (100k requests/month)
- May not have as much POI data as commercial options

**Try it:** https://docs.maptiler.com/schema/

---

### 2. Mapbox ⭐⭐ (Best Quality)
**Effort:** 10-15 minutes

Industry standard with excellent POI density and customization.

**Setup:**
1. Sign up at https://account.mapbox.com/auth/signup/
2. Get your access token from the dashboard
3. Add to `local.properties`:
   ```properties
   MAPBOX_ACCESS_TOKEN=pk.your_token_here
   ```
4. Add to `app/build.gradle.kts` in the `buildConfigField` section:
   ```kotlin
   buildConfigField("String", "MAPBOX_ACCESS_TOKEN", "\"${localProperties["MAPBOX_ACCESS_TOKEN"]}\"")
   ```
5. Update `MapScreen.kt` line 148-149:
   ```kotlin
   val apiKey = com.carryzonemap.app.BuildConfig.MAPBOX_ACCESS_TOKEN
   val styleUrl = "https://api.mapbox.com/styles/v1/mapbox/streets-v12?access_token=$apiKey"
   ```

**Available Styles:**
- `mapbox/streets-v12` - Standard street map
- `mapbox/outdoors-v12` - Topographic details
- `mapbox/light-v11` - Minimalist
- `mapbox/dark-v11` - Dark mode
- `mapbox/satellite-streets-v12` - Satellite + labels

**Pros:**
- Best POI density in the industry
- Highly customizable (can create custom styles)
- Excellent documentation and tooling
- Very reliable infrastructure
- Real-time traffic data available

**Cons:**
- Commercial pricing after free tier
- Free tier: 100k map loads/month
- After free tier: $0.50 per 1,000 loads

**Best for:** Production apps where POI detail is critical

**Docs:** https://docs.mapbox.com/mapbox-gl-js/

---

### 3. Stadia Maps ⭐ (Good Free Tier)
**Effort:** 10-15 minutes

OSM-based with better POI labels than MapTiler.

**Setup:**
1. Sign up at https://client.stadiamaps.com/signup/
2. Get your API key from the dashboard
3. Add to `local.properties`:
   ```properties
   STADIA_API_KEY=your_key_here
   ```
4. Add to `app/build.gradle.kts`:
   ```kotlin
   buildConfigField("String", "STADIA_API_KEY", "\"${localProperties["STADIA_API_KEY"]}\"")
   ```
5. Update `MapScreen.kt`:
   ```kotlin
   val apiKey = com.carryzonemap.app.BuildConfig.STADIA_API_KEY
   val styleUrl = "https://tiles.stadiamaps.com/styles/osm_bright.json?api_key=$apiKey"
   ```

**Available Styles:**
- `osm_bright` - Classic OpenStreetMap style
- `alidade_smooth` - Clean, modern
- `alidade_smooth_dark` - Dark mode
- `outdoors` - Topographic details

**Pros:**
- Generous free tier: 200k requests/month
- Based on OpenStreetMap data
- Good label density
- Developer-friendly

**Cons:**
- Requires new account
- Less customization than Mapbox
- Smaller company (less infrastructure redundancy)

**Pricing:**
- Free: 200k requests/month
- After: $49/month for 1M requests

**Best for:** Apps with moderate traffic that need good POI coverage

**Docs:** https://docs.stadiamaps.com/

---

### 4. OpenStreetMap Direct (Free, No API Key)
**Effort:** 30-60 minutes (requires raster tile setup)

Completely free raster tiles from OpenStreetMap.

**Important:** This requires switching from vector tiles to raster tiles, which changes your MapLibre setup significantly.

**Tile URL:**
```
https://tile.openstreetmap.org/{z}/{x}/{y}.png
```

**Setup:**
You'll need to configure MapLibre to use raster tiles instead of vector tiles. This involves creating a custom style JSON.

**Pros:**
- Completely free
- No API key required
- Good POI detail from OpenStreetMap
- Community-driven data

**Cons:**
- Raster tiles (can't be styled like vector tiles)
- Rate limits (must respect tile usage policy)
- Less flexible than vector tiles
- Requires more complex MapLibre setup
- Must follow OSM usage policy (https://operations.osmfoundation.org/policies/tiles/)

**Best for:** Open source projects, prototypes, non-commercial use

**Rate limits:**
- Max 2 connections per client
- Clearly identifiable User-Agent
- Must not exceed "heavy use" (undefined, but be reasonable)

**Alternative OSM Tile Providers:**
- Thunderforest (requires API key)
- HOT (Humanitarian OpenStreetMap Team)
- CyclOSM (for bike routes)

---

### 5. Custom MapLibre Style with Multiple Sources
**Effort:** 2-4 hours

Advanced option: Combine multiple tile sources in a custom style.

**Concept:**
```json
{
  "version": 8,
  "sources": {
    "base-map": {
      "type": "vector",
      "url": "https://api.maptiler.com/maps/basic-v2/style.json?key={key}"
    },
    "poi-overlay": {
      "type": "vector",
      "url": "https://example.com/poi-tiles/{z}/{x}/{y}.pbf"
    }
  },
  "layers": [
    // Base map layers
    // Custom POI layers with your own filtering
  ]
}
```

**Pros:**
- Maximum control over what POIs to show
- Can combine multiple data sources
- Optimize for your specific use case (carry zones)

**Cons:**
- Complex setup
- Requires deep MapLibre knowledge
- Must maintain custom style JSON
- Harder to debug

**Best for:** Apps with very specific POI requirements

---

## Comparison Table

| Provider | Free Tier | POI Density | Effort | Vector Tiles | Best For |
|----------|-----------|-------------|--------|--------------|----------|
| **MapTiler (new style)** | 100k/mo | Medium-High | ⭐ Easy | ✅ Yes | Quick fix |
| **Mapbox** | 100k/mo | ⭐⭐ Excellent | Medium | ✅ Yes | Production apps |
| **Stadia Maps** | 200k/mo | High | Medium | ✅ Yes | Moderate traffic |
| **OpenStreetMap** | Unlimited* | Medium-High | Hard | ❌ Raster | Open source |
| **Custom Style** | Varies | ⭐⭐ Maximum | Hard | ✅ Yes | Specific needs |

*With rate limits and usage policy restrictions

---

## Recommendation for CarryZoneMap

### Immediate Action (Today):
**Try MapTiler `basic-v2` or `bright-v2`** - Change line 149 in `MapScreen.kt`:
```kotlin
val styleUrl = "https://api.maptiler.com/maps/basic-v2/style.json?key=$apiKey"
```

Test at various zoom levels around businesses. If this shows enough detail, you're done!

### If More Detail Needed (This Week):
**Switch to Mapbox** - Best POI density for location-based apps like yours. The free tier (100k loads/month) is likely sufficient during development and early adoption. For a carry zone mapping app where users need to identify specific businesses, Mapbox's superior POI coverage is worth the potential cost.

### Budget-Conscious Alternative:
**Stadia Maps** - Double the free tier of Mapbox (200k vs 100k) with good OSM-based POI data.

---

## Implementation Checklist

### Option 1: MapTiler Style Change (5 minutes)
- [ ] Open `app/src/main/java/com/carryzonemap/app/ui/MapScreen.kt`
- [ ] Change line 149 to use `basic-v2`, `bright-v2`, or `topo-v2`
- [ ] Run app and test at zoom levels 15-18 around businesses
- [ ] Evaluate if sufficient

### Option 2: Mapbox (15 minutes)
- [ ] Sign up at https://account.mapbox.com/auth/signup/
- [ ] Copy access token from dashboard
- [ ] Add `MAPBOX_ACCESS_TOKEN` to `local.properties`
- [ ] Add `buildConfigField` to `app/build.gradle.kts`
- [ ] Sync Gradle
- [ ] Update `MapScreen.kt` lines 148-149
- [ ] Run app and test
- [ ] Check billing dashboard to monitor usage

### Option 3: Stadia Maps (15 minutes)
- [ ] Sign up at https://client.stadiamaps.com/signup/
- [ ] Copy API key from dashboard
- [ ] Add `STADIA_API_KEY` to `local.properties`
- [ ] Add `buildConfigField` to `app/build.gradle.kts`
- [ ] Sync Gradle
- [ ] Update `MapScreen.kt` lines 148-149
- [ ] Run app and test

---

## Additional Considerations

### POI Filtering
If you get too many POIs, you can filter them in a custom style:
- Show only specific categories (restaurants, bars, stores)
- Hide residential/irrelevant POIs
- Adjust label density per zoom level

### Offline Support
For offline functionality:
- Mapbox: Use Mapbox Maps SDK with offline region downloads
- MapLibre: Download and bundle MBTiles
- OSM: Use OSM2VectorTiles or similar tools

### Cost Estimation
For 1,000 active users viewing the map 10 times/month:
- MapTiler: 10k loads = Free tier
- Mapbox: 10k loads = Free tier
- Stadia: 10k loads = Free tier

For 10,000 active users viewing 10 times/month:
- MapTiler: 100k loads = Free tier limit
- Mapbox: 100k loads = Free tier limit
- Stadia: 100k loads = Free tier (still under 200k limit)

---

## Resources

- **MapTiler Styles:** https://docs.maptiler.com/schema/
- **Mapbox Styles:** https://docs.mapbox.com/api/maps/styles/
- **Stadia Maps:** https://docs.stadiamaps.com/
- **MapLibre GL JS:** https://maplibre.org/maplibre-gl-js/docs/
- **OpenStreetMap Tiles:** https://wiki.openstreetmap.org/wiki/Tiles
