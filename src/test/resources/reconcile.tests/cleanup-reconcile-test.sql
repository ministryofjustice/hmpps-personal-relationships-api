-- Tidy up data after merge tests.
-- Remove all prisoner_contact_restriction, prisoner_contact, and contact
-- rows created from the tests

delete from prisoner_contact_restriction where prisoner_contact_id in (
    select prisoner_contact_id
    from prisoner_contact pc
    where pc.contact_id in (30001, 30002)
);

delete from prisoner_contact where prisoner_contact_id in (
    select prisoner_contact_id
    from prisoner_contact pc
    where pc.contact_id in (30001, 30002)
);

delete from contact_email where contact_id in (30001, 30002);
delete from contact_identity where contact_id in (30001, 30002);
delete from contact_restriction where contact_id in (30001, 30002);
delete from employment where contact_id in (30001, 30002);
delete from contact_address_phone where contact_id in (30001, 30002);
delete from contact_address where contact_id in (30001, 30002);
delete from contact_phone where contact_id in (30001, 30002);
delete from contact where contact_id in (30001, 30002);
