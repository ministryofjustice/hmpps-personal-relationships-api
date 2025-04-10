--
-- Creates a view over the prisoner_contact table which counts the number of active social and and official contacts from the
-- prisoners current term.
-- Note: the view is only dropped if the checksum of this migration changes
-- Internal version to bump if you need to force recreation: 1
DROP VIEW IF EXISTS v_prisoner_contact_count;
CREATE VIEW v_prisoner_contact_count
AS
SELECT prisoner_number,
       count(*) FILTER (WHERE active AND relationship_type = 'S') AS social,
       count(*) FILTER (WHERE active AND relationship_type = 'O') AS official
FROM prisoner_contact
WHERE current_term = true
GROUP BY prisoner_number;

-- End