-- Tidy up data after merge tests.
-- Remove all prisoner_contact_restriction, prisoner_contact, and contact
-- rows created from the tests

delete from prisoner_contact_restriction where prisoner_contact_id in (
  select prisoner_contact_id
  from prisoner_contact pc
  where pc.contact_id in (30001, 30002, 30003, 30004, 30005)
);

delete from prisoner_contact where prisoner_contact_id in (
  select prisoner_contact_id
  from prisoner_contact pc
  where pc.contact_id in (30001, 30002, 30003, 30004, 30005)
);

delete from contact where contact_id in (30001, 30002, 30003, 30004, 30005);
