-- Tidy up data after merge tests
delete from prisoner_contact_restriction where prisoner_contact_id in (40001, 40002, 40003, 40004, 40005);
delete from prisoner_contact where prisoner_contact_id in (40001, 40002, 40003, 40004, 40005);
delete from contact where contact_id in (30001, 30002, 30003, 30004, 30005);
