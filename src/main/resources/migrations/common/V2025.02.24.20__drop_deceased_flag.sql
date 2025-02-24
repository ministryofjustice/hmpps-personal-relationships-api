--
-- Remove column deceased_flag and rely on deceased date instead.
--

ALTER TABLE contact DROP COLUMN deceased_flag;

-- End