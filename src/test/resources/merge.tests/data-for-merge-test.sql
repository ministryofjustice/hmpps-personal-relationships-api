-- ==========================================================
-- Test data for prisoner contact merge integration test
-- ===========================================================

insert into contact(contact_id, title, last_name, first_name, middle_names, date_of_birth, gender, domestic_status, language_code, created_by, deceased_date, interpreter_required, staff_flag)
values (30001,  'MR', 'Merge-a', 'John', 'Mid-a', '2000-11-21', 'M', 'M', 'ENG', 'TIM', null, false, false),
       (30002,  'MR', 'Merge-b', 'Jack', 'Mid-b', '2000-11-22', 'M', 'D', 'ENG', 'TIM', null, false, false),
       (30003,  'MR', 'Merge-c', 'Joly', 'Mid-c', '2000-11-23', 'M', 'S', 'ENG', 'TIM', null, false, false),
       (30004,  'MR', 'Merge-d', 'Jam',  'Mid-d', '2000-11-24', 'M', 'S', 'ENG', 'TIM', null, false, false),
       (30005,  'MR', 'Merge-e', 'Jim',  'Mid-e', '2000-11-25', 'M', 'S', 'ENG', 'TIM', null, false, false);

insert into prisoner_contact (prisoner_contact_id, contact_id, prisoner_number, relationship_type, active, relationship_to_prisoner, comments, created_at_prison, created_by, created_time)
values (40001, 30001, 'A3333AA', 'S', true, 'BRO', 'Comment', 'MDI', 'TIM', current_timestamp),
       (40002, 30002, 'A3333AA', 'O', true, 'POL', 'Comment', 'MDI', 'TIM', current_timestamp),
       (40003, 30003, 'A4444AA', 'S', true, 'MOT', 'Comment', 'MDI', 'TIM', current_timestamp),
       (40004, 30004, 'A4444AA', 'S', true, 'SIS', 'Comment', 'MDI', 'TIM', current_timestamp),
       (40005, 30005, 'A4444AA', 'O', true, 'POL', 'Comment', 'MDI', 'TIM', current_timestamp);

insert into prisoner_contact_restriction (prisoner_contact_id, restriction_type, start_date, expiry_date, comments, created_by, created_time)
values (40001, 'BAN', '2025-01-01', '2025-05-02', 'Restriction A', 'TIM', current_timestamp),
       (40002, 'BAN', '2025-01-02', '2025-05-01', 'Restriction B', 'TIM', current_timestamp);
