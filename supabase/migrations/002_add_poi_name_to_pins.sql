-- Add POI name column to pins table
-- This migration adds the 'name' field to store the POI (Point of Interest) name
-- that each pin is associated with

-- Add name column to pins table
ALTER TABLE pins
ADD COLUMN name TEXT NOT NULL DEFAULT '';

-- Update the default constraint to remove default value for future inserts
-- Existing rows will keep the empty string, but new inserts must provide a name
ALTER TABLE pins
ALTER COLUMN name DROP DEFAULT;

-- Add index on name for searching pins by POI name
CREATE INDEX idx_pins_name ON pins(name);

-- Example queries after migration:

-- 1. Get all pins with their POI names
-- SELECT id, name, longitude, latitude, status FROM pins;

-- 2. Find pins at a specific POI
-- SELECT * FROM pins WHERE name = 'Starbucks Coffee';

-- 3. Search for pins by partial POI name
-- SELECT * FROM pins WHERE name ILIKE '%starbucks%';
