-- Create unique index to ensure only one active record exists per prisoner for number of children
-- This prevents duplicate active records for the same prisoner
CREATE UNIQUE INDEX one_active_record_per_prisoner_number_of_children
    ON prisoner_number_of_children (prisoner_number)
    WHERE active = true;

-- Create unique index to ensure only one active record exists per prisoner for domestic status
-- This prevents duplicate active records for the same prisoner
CREATE UNIQUE INDEX one_active_record_per_prisoner_domestic_status
    ON prisoner_domestic_status (prisoner_number)
    WHERE active = true;