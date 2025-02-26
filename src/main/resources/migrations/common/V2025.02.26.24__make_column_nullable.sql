--
-- Update column to allow null values
--
ALTER TABLE prisoner_domestic_status ALTER COLUMN domestic_status_code TYPE VARCHAR(12);
ALTER TABLE prisoner_domestic_status ALTER COLUMN domestic_status_code DROP NOT NULL;