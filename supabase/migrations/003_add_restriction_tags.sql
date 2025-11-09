-- Migration: Add restriction tags and enforcement details to pins table
-- Version: 003
-- Date: 2025-11-08
-- Description: Adds restriction_tag, has_security_screening, and has_posted_signage columns to pins table

-- Add restriction_tag column (nullable, required if status is NO_GUN/2)
ALTER TABLE pins
ADD COLUMN restriction_tag TEXT;

-- Add enforcement detail columns with default values
ALTER TABLE pins
ADD COLUMN has_security_screening BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE pins
ADD COLUMN has_posted_signage BOOLEAN NOT NULL DEFAULT FALSE;

-- Optional: Create enum type for better validation
-- Note: This is optional but recommended for data integrity
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

-- Alter the column to use the enum type
-- This provides validation at the database level
ALTER TABLE pins
ALTER COLUMN restriction_tag TYPE restriction_tag_type
USING restriction_tag::restriction_tag_type;

-- Optional: Add constraint to ensure RED pins (status=2) have a restriction tag
-- Comment this out if you want to allow gradual migration
ALTER TABLE pins
ADD CONSTRAINT check_red_pin_has_tag
CHECK (
    status != 2 OR restriction_tag IS NOT NULL
);

-- Add index on restriction_tag for faster filtering queries
CREATE INDEX idx_pins_restriction_tag ON pins(restriction_tag);

-- Add comment to document the constraint
COMMENT ON CONSTRAINT check_red_pin_has_tag ON pins IS
'Ensures that pins with status NO_GUN (2) must have a restriction tag specified';
