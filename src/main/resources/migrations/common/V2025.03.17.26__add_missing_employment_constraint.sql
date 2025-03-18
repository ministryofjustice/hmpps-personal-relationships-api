--
-- Add constraint on employments in contact
--

ALTER TABLE employment ADD CONSTRAINT employment_contact_id_fkey FOREIGN KEY (contact_id) REFERENCES contact (contact_id);

-- End