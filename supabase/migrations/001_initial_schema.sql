-- CarryZoneMap Initial Database Schema
-- This migration creates the pins table with geographic support and Row Level Security

-- Enable PostGIS extension for geographic queries
CREATE EXTENSION IF NOT EXISTS postgis;

-- Create pins table
CREATE TABLE pins (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  longitude DOUBLE PRECISION NOT NULL,
  latitude DOUBLE PRECISION NOT NULL,

  -- Generated geography column for efficient spatial queries
  -- This is automatically computed from longitude/latitude
  location GEOGRAPHY(POINT, 4326) GENERATED ALWAYS AS (
    ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)
  ) STORED,

  -- Pin status: 0=ALLOWED, 1=UNCERTAIN, 2=NO_GUN
  status INTEGER NOT NULL CHECK (status IN (0, 1, 2)),

  -- Optional metadata
  photo_uri TEXT,
  notes TEXT,
  votes INTEGER DEFAULT 0,

  -- User tracking
  created_by UUID REFERENCES auth.users(id) ON DELETE SET NULL,

  -- Timestamps
  created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
  last_modified TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

-- Create indexes for common queries
CREATE INDEX idx_pins_status ON pins(status);
CREATE INDEX idx_pins_created_by ON pins(created_by);
CREATE INDEX idx_pins_created_at ON pins(created_at DESC);
CREATE INDEX idx_pins_last_modified ON pins(last_modified DESC);

-- Create spatial index for geographic queries (bounding box, radius search)
CREATE INDEX idx_pins_location ON pins USING GIST (location);

-- Enable Row Level Security
ALTER TABLE pins ENABLE ROW LEVEL SECURITY;

-- RLS Policy: Anyone can read pins
-- This allows unauthenticated users to view the map
-- TODO: Consider restricting by geographic region in the future
CREATE POLICY "Pins are viewable by everyone"
  ON pins FOR SELECT
  USING (true);

-- RLS Policy: Authenticated users can insert pins
-- The created_by field must match the authenticated user
CREATE POLICY "Authenticated users can insert pins"
  ON pins FOR INSERT
  WITH CHECK (auth.uid() = created_by);

-- RLS Policy: Users can update any pin
-- TODO: Consider restricting to own pins only, or implement voting system
CREATE POLICY "Users can update any pin"
  ON pins FOR UPDATE
  USING (auth.role() = 'authenticated')
  WITH CHECK (auth.role() = 'authenticated');

-- RLS Policy: Users can delete their own pins
CREATE POLICY "Users can delete own pins"
  ON pins FOR DELETE
  USING (auth.uid() = created_by);

-- Function to automatically update last_modified timestamp
CREATE OR REPLACE FUNCTION update_last_modified()
RETURNS TRIGGER AS $$
BEGIN
  NEW.last_modified = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to auto-update last_modified on UPDATE
CREATE TRIGGER set_last_modified
  BEFORE UPDATE ON pins
  FOR EACH ROW
  EXECUTE FUNCTION update_last_modified();

-- Example queries for testing after migration:

-- 1. Get all pins
-- SELECT id, longitude, latitude, status, created_at FROM pins;

-- 2. Get pins within a bounding box (San Francisco area example)
-- SELECT id, longitude, latitude, status
-- FROM pins
-- WHERE longitude BETWEEN -122.5 AND -122.3
--   AND latitude BETWEEN 37.7 AND 37.8;

-- 3. Get pins within 1000 meters of a point (using PostGIS)
-- SELECT id, longitude, latitude, status,
--        ST_Distance(location, ST_MakePoint(-122.4194, 37.7749)::geography) as distance_meters
-- FROM pins
-- WHERE ST_DWithin(
--   location,
--   ST_MakePoint(-122.4194, 37.7749)::geography,
--   1000
-- )
-- ORDER BY distance_meters;

-- 4. Count pins by status
-- SELECT status, COUNT(*) as count FROM pins GROUP BY status;
