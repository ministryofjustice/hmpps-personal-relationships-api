-- ==================================================================
-- Test data for prisoner reconciliation integration test
-- Requires the fixed IDs to tie the data together
-- (might be better to create these as part of the test using the API)
-- ==================================================================

insert into contact(contact_id, title, last_name, first_name, date_of_birth, gender, domestic_status, language_code, created_by, interpreter_required, staff_flag)
values (30001,  'MR', 'Ma', 'John', '2000-11-21', 'M', 'M', 'ENG', 'TIM', false, false),
       (30002,  'MR', 'Mb', 'Jack', '2000-11-22', 'M', 'D', 'ENG', 'TIM', false, false);

insert into prisoner_contact (prisoner_contact_id, contact_id, prisoner_number, relationship_type, active, relationship_to_prisoner, created_at_prison, created_by, created_time)
values (40001, 30001, 'A3333AA', 'S', true, 'BRO', 'MDI', 'TIM', current_timestamp),
       (40002, 30001, 'A3333AA', 'O', true, 'POL', 'MDI', 'TIM', current_timestamp),
       (40003, 30001, 'A4444AA', 'S', true, 'MOT', 'MDI', 'TIM', current_timestamp),
       (40004, 30001, 'A4444AA', 'S', true, 'SIS', 'MDI', 'TIM', current_timestamp),
       (40005, 30002, 'A4444AA', 'O', true, 'POL', 'MDI', 'TIM', current_timestamp);

insert into prisoner_contact_restriction (prisoner_contact_id, restriction_type, start_date, expiry_date, comments, created_by, created_time)
values (40001, 'BAN', '2025-01-01', '2025-05-02', 'Restriction A', 'TIM', current_timestamp),
       (40002, 'BAN', '2025-01-02', '2025-05-03', 'Restriction B', 'TIM', current_timestamp);

insert into contact_identity(contact_identity_id, contact_id, identity_type, identity_value, issuing_authority,created_by)
values (40001, 30001, 'DL', 'LAST-87736799M', 'DVLA', 'TIM'),
       (40002, 30001, 'PASS', 'PP87878787878', 'UKBORDER', 'TIM');

insert into contact_restriction(contact_id, restriction_type, start_date, expiry_date, comments, created_by)
values ( 30001, 'ACC', '2000-11-21','2000-11-21','comment', 'TIM'),
       ( 30001, 'BAN', '2000-11-21','2005-11-21','comment',  'TIM'),
       ( 30002, 'CCTV', '2000-11-21','2001-11-21','comment','TIM');

insert into contact_email(contact_id, email_address, created_by)
values (30001, 'mr.last@example.com', 'TIM'),
       (30001, 'miss.last@example.com',  'TIM'),
       (30002, 'mrs.last@example.com', 'TIM');

insert into contact_address(contact_address_id, contact_id, address_type, primary_address, flat, property, street, area, city_code, county_code, post_code, country_code, comments, created_by, verified, verified_by, verified_time, mail_flag, start_date, end_date, no_fixed_address)
values (40001,  30001,  'HOME', true,  null, '24','Acacia Avenue', 'Bunting', '25343', 'S.YORKSHIRE', 'S2 3LK', 'ENG', 'Some comments', 'TIM', false, null, null, false, null, null, false),
       (40002,  30001,  'WORK', false, 'Flat 1', '42','My Work Place', 'Bunting', '25343', 'S.YORKSHIRE', 'S2 3LK', 'ENG', 'Some comments', 'TIM', true, 'BOB', '2020-01-01 10:30:00', true, '2020-01-02', '2029-03-04', true),
       (40003,  30001,  'HOME', true,  null, '24','Acacia Avenue', 'Bunting', '25343', 'S.YORKSHIRE', 'S2 3LK', 'ENG', 'Some comments', 'TIM', false, null, null, false, null, null, false),
       (40004,  30002,  'HOME', true,  null, '24','Acacia Avenue', 'Bunting', '25343', 'S.YORKSHIRE', 'S2 3LK', 'ENG', 'Some comments', 'TIM', false, null, null, false, null, null, false);

insert into contact_phone(contact_phone_id, contact_id, phone_type, phone_number, ext_number, created_by, created_time)
values (40001, 30001, 'MOB', '01111 666666', null, 'TIM', '2024-10-01 12:00:00'),
       (40002, 30001, 'HOME', '01111 777777', '+0123', 'JAMES', '2024-10-01 13:00:00'),
       (40003, 30001, 'HOME', '01111 888888', '+0123', 'JAMES', '2024-10-01 13:00:00'),
       (40004, 30002, 'MOB', '07878 222222', null, 'TIM', '2024-10-01 12:00:00');

insert into contact_address_phone(contact_address_phone_id, contact_id, contact_address_id, contact_phone_id, created_by)
values (40001, 30001, 40003, 40003, 'TIM'),
       (40002, 30002, 40004, 40004, 'TIM');

insert into employment(contact_id, organisation_id, active, created_by, created_time)
values (30001, 57, true, 'TIM', '2024-10-01 12:00:00');
