insert into contact(contact_id, title, last_name, first_name, date_of_birth, gender, domestic_status, created_by, interpreter_required, staff_flag)
values (30001,  'MR', 'One',  'Pom', '2000-11-21', 'M', 'M', 'TIM', false, false),
       (30002,  'MR', 'Two',  'Com', '2000-11-22', 'M', 'M', 'TIM', false, false),
       (30003,  'MR', 'Three','Ppa', '2000-11-23', 'M', 'M', 'TIM', false, false),
       (30004,  'MR', 'Four', 'Prob','2000-11-24', 'M', 'M', 'TIM', false, false),
       (30005,  'MR', 'Five', 'Ro',  '2000-11-25', 'M', 'M', 'TIM', false, false),
       (30006,  'MR', 'Six',  'Ca',  null,         'M', 'M', 'TIM', false, false);

insert into prisoner_contact (prisoner_contact_id, contact_id, prisoner_number, relationship_type, active, current_term, relationship_to_prisoner, created_at_prison, created_by, created_time)
values (40001, 30001, 'A3333AA', 'O', false, false,'POM', 'MDI', 'TIM', current_timestamp),
       (40002, 30002, 'A3333AA', 'O', true,  true, 'COM', 'MDI', 'TIM', current_timestamp),
       (40003, 30003, 'A3333AA', 'O', false, false,'PPA', 'MDI', 'TIM', current_timestamp),
       (40004, 30004, 'A3333AA', 'O', true,  true, 'PROB','MDI', 'TIM', current_timestamp),
       (40005, 30004, 'A4444AA', 'O', true,  true, 'RO',  'MDI', 'TIM', current_timestamp),
       (40006, 30005, 'A3333AA', 'O', true,  true, 'RO',  'MDI', 'TIM', current_timestamp),
       (40007, 30005, 'A4444AA', 'S', true,  true, 'FRI', 'MDI', 'TIM', current_timestamp),
       (40008, 30006, 'A3333AA', 'O', false, false,'CA',  'MDI', 'TIM', current_timestamp);