-- Reset the sequences after loading test data with specific IDs.
-- Example data - implied sequences for JPA GenerationType.IDENTITY

alter sequence if exists contact_address_contact_address_id_seq restart with 18;
alter sequence if exists contact_email_contact_email_id_seq restart with 5;
alter sequence if exists contact_identity_contact_identity_id_seq restart with 5;
alter sequence if exists contact_phone_contact_phone_id_seq restart with 13;
alter sequence if exists prisoner_contact_prisoner_contact_id_seq restart with 31;
alter sequence if exists contact_restriction_contact_restriction_id_seq restart with 5;
alter sequence if exists contact_address_phone_contact_address_phone_id_seq restart with 2;
alter sequence if exists prisoner_contact_restriction_prisoner_contact_restriction_id_seq restart with 6;

-- End