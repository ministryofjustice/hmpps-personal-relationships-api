-- ==========================================================
-- Example data
-- Not loaded into any real environments - DEV, PREPROD or PROD
-- Intended for integration tests and local-running only.
-- ===========================================================

insert into contact(contact_id, title, last_name, first_name, middle_names, date_of_birth, estimated_is_over_eighteen, gender, domestic_status, language_code, created_by, deceased_flag, deceased_date, interpreter_required, staff_flag)
values (1,  'MR',   'Last',   'Jack',       'Middle', '2000-11-21', null, 'M',   'M', 'ENG', 'TIM', false, null, false, true),
       (2,  'MISS', 'Last',   'Jacqueline', 'Middle', '2000-11-22', null, 'F', 'D', 'ENG', 'TIM', false, null, false, false),
       (3,  'MRS',  'Last',   'Jane',       'Middle', '2000-11-23', null, 'M',   'S', 'ENG', 'TIM', false, null, false, false),
       (4,  'MR',   'Four',   'John',       'Middle', '2000-05-18', null, 'M',   'S', 'ENG', 'TIM', false, null, false, false),
       (5,  'MR',   'Five',   'Jon',        'Middle', '2000-09-21', null, 'M',   'S', 'ENG', 'TIM', false, null, false, false),
       (6,  'MR',   'Six',    'Johnny',     'Middle', '2000-10-23', null, 'M',   'S', 'ENG', 'TIM', false, null, false, false),
       (7,  'MR',   'Seven',  'Pete',       'Middle', '2015-08-29', null, 'M',   'S', 'ENG', 'TIM', false, null, false, false),
       (8,  'MR',   'Eight',  'Harry',      'Middle', '2015-12-23', null, 'M',   'S', 'ENG', 'TIM', false, null, false, false),
       (9,  'MR',   'Nine',   'Donald',     'Middle', '2000-11-23', null, 'M',   'S', 'ENG', 'TIM', false, null, false, false),
       (10, 'MS',   'Ten',    'Freya',      'Middle', '2000-11-24', null, 'F', 'M', 'ENG', 'TIM', false, null, false, false),
       (11, 'MS',   'Eleven', 'Suki',       'Middle', '2000-11-25', null, 'F', 'D', 'ENG', 'TIM', false, null, false, false),
       (12, 'MRS',  'Twelve', 'Jane',       'Middle', '2000-11-26', null, 'F', 'S', 'ENG', 'TIM', false, null, false, false),
       (13, 'MRS',  'Thirteen', 'Mark',     'Middle', null, 'YES', 'F', 'S', 'ENG', 'TIM', false, null, false, false),
       (14, 'MRS',  'Fourteen', 'Phil',     'Middle', null, 'NO',  'F', 'S', 'ENG', 'TIM', false, null, false, false),
       (15, 'MRS',  'Fifteen', 'Carl',      'Middle', '2000-11-26', 'DO_NOT_KNOW', 'F', 'S', 'ENG', 'TIM', false, '2024-01-26', false, false),
       (16, 'MRS',  'NoAddress', 'Liam',    'Middle', null, 'YES', 'F', 'S', 'ENG', 'TIM', false, null, false, false),
       (17, 'MRS',  'NoAddress', 'Hannah',  'Middle', null, 'YES', 'F', 'S', 'ENG', 'TIM', false, null, false, false),
       (18, null,   'Address', 'Minimal',    null, null, null, null, null, null, 'TIM', false, null, false, false),
       (19, null,   'Dead', 'Currently',     null, '1980-01-01', null, null, null, null, 'TIM', true, '2000-01-01', false, false),
       (20, null,   'French', 'Only',     null, '1980-01-01', null, null, null, 'FRE-FRA', 'TIM', true, '2000-01-01', true, false),
       (21, 'MR',   'Language', 'Update',   'Middle', '2000-11-25', null, 'F', 'D', 'ENG', 'PB', false, null, false, false);

insert into contact_identity(contact_identity_id, contact_id, identity_type, identity_value, issuing_authority,created_by)
values (1, 1, 'DL', 'LAST-87736799M', 'DVLA', 'TIM'),
       (2, 2, 'PASS', 'PP87878787878', 'UKBORDER', 'TIM'),
       (3, 3, 'NINO', 'NI989989AA', 'HMRC', 'TIM'),
       (4, 4, 'NHS', 'NHS999', 'National Health Service', 'JAMES');

insert into contact_restriction(contact_id, restriction_type, start_date, expiry_date, comments, created_by)
values (1, 'ACC', '2000-11-21','2000-11-21','N/A', 'JBAKER_GEN'),
       (2, 'BAN', '2000-11-21','2005-11-21','N/A',  'JBAKER_GEN'),
       (3, 'CCTV', '2000-11-21','2001-11-21','N/A','JBAKER_GEN'),
       (3, 'BAN', null, null, null, 'FOO_USER');

insert into contact_address(contact_address_id, contact_id, address_type, primary_address, flat, property, street, area, city_code, county_code, post_code, country_code, comments, created_by, verified, verified_by, verified_time, mail_flag, start_date, end_date, no_fixed_address)
values (1,  1,  'HOME', true,  null, '24','Acacia Avenue', 'Bunting', '25343', 'S.YORKSHIRE', 'S2 3LK', 'ENG', 'Some comments', 'TIM', false, null, null, false, null, null, false),
       (2,  1,  'WORK', false, 'Flat 1', '42','My Work Place', 'Bunting', '25343', 'S.YORKSHIRE', 'S2 3LK', 'ENG', 'Some comments', 'TIM', true, 'BOB', '2020-01-01 10:30:00', true, '2020-01-02', '2029-03-04', true),
       (3,  2,  'HOME', true,  null, '24','Acacia Avenue', 'Bunting', '25343', 'S.YORKSHIRE', 'S2 3LK', 'ENG', 'Some comments', 'TIM', false, null, null, false, null, null, false),
       (4,  3,  'HOME', true,  null, '24','Acacia Avenue', 'Bunting', '25343', 'S.YORKSHIRE', 'S2 3LK', 'ENG', 'Some comments', 'TIM', false, null, null, false, null, null, false),
       (5,  4,  'HOME', true,  null, '26','Acacia Avenue', 'Bunting', '25343', 'S.YORKSHIRE', 'S2 3LK', 'ENG', 'Some comments', 'TIM', false, null, null, false, null, null, false),
       (6,  5,  'HOME', true,  null, '28','Acacia Avenue', 'Bunting', '25343', 'S.YORKSHIRE', 'S2 3LK', 'ENG', 'Some comments', 'TIM', false, null, null, false, null, null, false),
       (7,  6,  'HOME', true,  null, '30','Acacia Avenue', 'Bunting', '25343', 'S.YORKSHIRE', 'S2 3LK', 'ENG', 'Some comments', 'TIM', false, null, null, false, null, null, false),
       (8,  7,  'HOME', true,  null, '32','Acacia Avenue', 'Bunting', '25343', 'S.YORKSHIRE', 'S2 3LK', 'ENG', 'Some comments', 'TIM', false, null, null, false, null, null, false),
       (9,  8,  'HOME', true,  null, '34','Acacia Avenue', 'Bunting', '25343', 'S.YORKSHIRE', 'S2 3LK', 'ENG', 'Some comments', 'TIM', false, null, null, false, null, null, false),
       (10,  9, 'HOME', true,  null, '36','Acacia Avenue', 'Bunting', '25343', 'S.YORKSHIRE', 'S2 3LK', 'ENG', 'Some comments', 'TIM', false, null, null, false, null, null, false),
       (11, 10, 'HOME', true,  null, '38','Acacia Avenue', 'Bunting', '25343', 'S.YORKSHIRE', 'S2 3LK', 'ENG', 'Some comments', 'TIM', false, null, null, false, null, null, false),
       (12, 11, 'HOME', true,  null, '40','Acacia Avenue', 'Bunting', '25343', 'S.YORKSHIRE', 'S2 3LK', 'ENG', 'Some comments', 'TIM', false, null, null, false, null, null, false),
       (13, 12, 'HOME', true, 'Flat 3b', '42','Acacia Avenue', 'Bunting', '25343', 'S.YORKSHIRE', 'S2 3LK', 'ENG', 'Some comments', 'TIM', false, null, null, false, null, null, false),
       (14, 13, 'HOME', true, 'Flat 35b', '42','Acacia Avenue', 'Bunting', '25343', 'S.YORKSHIRE', 'S2 3LK', 'ENG', 'Some comments', 'TIM', false, null, null, false, null, null, false),
       (15, 14, 'HOME', true, 'Flat 3', '42','Acacia Avenue', 'Bunting', '25343', 'S.YORKSHIRE', 'S2 3LK', 'ENG', 'Some comments', 'TIM', false, null, null, false, null, null, false),
       (16, 15, 'HOME', true, 'Flat 32', '42','Acacia Avenue', 'Bunting', '25343', 'S.YORKSHIRE', 'S2 3LK', 'ENG', 'Some comments', 'TIM', false, null, null, false, null, null, false),
       (17, 18, 'HOME', true, null, null,null, null, null, null, null, null, null, 'TIM', false, null, null, false, null, null, true);

insert into contact_phone(contact_phone_id, contact_id, phone_type, phone_number, ext_number, created_by, created_time)
values (1, 1, 'MOB', '07878 111111', null, 'TIM', '2024-10-01 12:00:00'),
       (2, 1, 'HOME', '01111 777777', '+0123', 'JAMES', '2024-10-01 13:00:00'), --most recent
       (3, 2, 'MOB', '07878 222222', null, 'TIM', '2024-10-01 12:00:00'),
       (4, 3, 'MOB', '07878 222222', null, 'TIM', '2024-10-01 12:00:00'),
       (5, 4, 'MOB', '07878 222222', null, 'TIM', '2024-10-01 12:00:00'),
       (6, 5, 'MOB', '07878 222222', null, 'TIM', '2024-10-01 12:00:00'),
       (7, 6, 'HOME', '07878 222222', null, 'TIM', '2024-10-01 12:00:00'),
       (8, 7, 'HOME', '07878 222222', null, 'TIM', '2024-10-01 12:00:00'),
       (9, 8, 'MOB', '07878 222222', null, 'TIM', '2024-10-01 12:00:00'),
       (10, 9, 'WORK', '07878 222222', null, 'TIM', '2024-10-01 12:00:00'),
       (11, 10, 'MOB', '07878 222222', null, 'TIM', '2024-10-01 12:00:00'),
       (12, 11, 'MOB', '07878 222222', null, 'TIM', '2024-10-01 12:00:00');

insert into contact_address_phone(contact_address_phone_id, contact_id, contact_address_id, contact_phone_id, created_by)
values (1, 1, 1, 2, 'JAMES');

insert into contact_email(contact_email_id, contact_id, email_address, created_by)
values (1, 1, 'mr.last@example.com', 'TIM'),
       (2, 2, 'miss.last@example.com',  'TIM'),
       (3, 3, 'mrs.last@example.com', 'TIM'),
       (4, 3, 'work@example.com', 'JAMES');

insert into prisoner_contact (prisoner_contact_id, contact_id, prisoner_number, contact_type, active, relationship_type, comments, created_at_prison, created_by, created_time)
values (1, 1, 'A1234BB', 'S', true, 'FA', 'Comment', 'MDI', 'TIM', current_timestamp),
       -- Jason Mitchell
       (2,  1,  'A8185DY', 'S', true, 'FA', 'Comment', 'MDI', 'TIM', current_timestamp),
       (3,  6,  'A8185DY', 'S', true, 'FRI', 'Comment', 'MDI', 'TIM', current_timestamp),
       (4,  7,  'A8185DY', 'S', true, 'FRI', 'Comment', 'MDI', 'TIM', current_timestamp),
       (5,  8,  'A8185DY', 'S', true, 'FRI', 'Comment', 'MDI', 'TIM', current_timestamp),
       (6,  10, 'A8185DY', 'S', true, 'FRI', 'Comment', 'MDI', 'TIM', current_timestamp),
       (7,  11, 'A8185DY', 'S', true, 'FRI', 'Comment', 'MDI', 'TIM', current_timestamp),
       -- Tim Harrison
       (8,  1,  'G4793VF', 'S', true,  'FA', 'Comment', 'MDI', 'TIM', current_timestamp),
       (9,  2,  'G4793VF', 'S', true,  'MOT', 'Comment', 'MDI', 'TIM', current_timestamp),
       (10, 3,  'G4793VF', 'O', true,  'POL', 'Comment', 'MDI', 'TIM', current_timestamp),
       (11, 4,  'G4793VF', 'S', true,  'BRO', 'Comment', 'MDI', 'TIM', current_timestamp),
       (12, 5,  'G4793VF', 'S', true,  'FRI', 'Comment', 'MDI', 'TIM', current_timestamp),
       (13, 6,  'G4793VF', 'S', true,  'FRI', 'Comment', 'MDI', 'TIM', current_timestamp),
       (14, 7,  'G4793VF', 'S', true,  'FRI', 'Comment', 'MDI', 'TIM', current_timestamp),
       (15, 8,  'G4793VF', 'S', true,  'UN', 'Comment', 'MDI', 'TIM', current_timestamp),
       (16, 9,  'G4793VF', 'S', true,  'UN', 'Comment', 'MDI', 'TIM', current_timestamp),
       (17, 10, 'G4793VF', 'S', true, 'GIF', 'Comment', 'MDI', 'TIM', current_timestamp),
       (18, 11, 'G4793VF', 'S', false, 'GIF', 'Comment', 'MDI', 'TIM', current_timestamp),
       (19, 12, 'G4793VF', 'S', false, 'GIF', 'Comment', 'MDI', 'TIM', current_timestamp),
       -- Tim Cooks
       (20, 1, 'A4162DZ', 'S', true, 'FA', 'Comment', 'MDI', 'TIM', current_timestamp),
       (21, 2, 'A4162DZ', 'S', true, 'FRI', 'Comment', 'MDI', 'TIM', current_timestamp),
       (22, 3, 'A4162DZ', 'O', true, 'POL', 'Comment', 'MDI', 'TIM', current_timestamp),
       (23, 4, 'A4162DZ', 'S', true, 'FRI', 'Comment', 'MDI', 'TIM', current_timestamp),
       (24, 5, 'A4162DZ', 'S', true, 'FRI', 'Comment', 'MDI', 'TIM', current_timestamp),
       (25, 10, 'A4162DZ', 'S', true, 'MOT', 'Comment', 'MDI', 'TIM', current_timestamp),
       -- Justin Timberlake
       (26, 1, 'A5166DY', 'S', true, 'FA', 'Comment', 'MDI', 'TIM', current_timestamp),
       -- Mike Toby
       (27, 1, 'A4385DZ', 'S', true, 'FA', 'Comment', 'MDI', 'TIM', current_timestamp),
       (28, 10, 'A4385DZ', 'S', true, 'MOT', 'Comment', 'MDI', 'TIM', current_timestamp),
       (29, 18, 'A4385DZ', 'S', true, 'FRI', null, 'MDI', 'TIM', current_timestamp),
       -- Stubbed Prisoner With Dead Contact
       (30, 19, 'E4567FG', 'S', true, 'FRI', 'Dead Contact', 'MDI', 'TIM', current_timestamp);

insert into prisoner_contact_restriction (prisoner_contact_id, restriction_type, start_date, expiry_date, comments, created_by, created_time, updated_by, updated_time )
values
    (12, 'PREINF', '2024-01-01', '2024-12-31', 'Restriction due to ongoing investigation', 'admin', '2024-10-01 12:00:00', 'editor', '2024-10-02 15:30:00'),
    (12, 'CHILD', '2023-06-01', '2024-01-01', 'Limited contact allowed', 'supervisor', '2023-05-31 09:00:00', 'manager', '2023-07-01 10:00:00'),
    (12, 'BAN', '2022-08-15', '2023-08-15', 'No contact allowed due to past incidents', 'officer', '2022-08-14 11:00:00', 'reviewer', '2022-09-15 16:00:00'),
    (10, 'PREINF', '2024-01-01', '2024-12-31', 'Restriction due to ongoing investigation', 'admin', '2024-10-01 12:00:00', 'editor', '2024-10-02 15:30:00'),
    (10, 'BAN', null, null, null, 'officer', '2022-08-14 11:00:00', null, null);
