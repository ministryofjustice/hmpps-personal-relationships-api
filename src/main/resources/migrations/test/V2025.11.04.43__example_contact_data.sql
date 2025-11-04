-- ==========================================================
-- Example data
-- Not loaded into any real environments - DEV, PREPROD or PROD
-- Intended for integration tests and local-running only.
-- ===========================================================

INSERT INTO public.contact (contact_id, title, last_name, first_name, middle_names, date_of_birth, staff_flag, remitter_flag, deceased_date, gender, domestic_status, language_code, interpreter_required, created_by, created_time, updated_by, updated_time) VALUES
(11041, 'MRS', 'NELSINWOOD', 'AJNCEOT', null, '1932-01-14', false, false, null, 'F', null, null, false, 'ZQA16H', '2007-04-17 13:47:19.955077', 'OMS_OWNER', '2017-05-22 16:06:20.049325'),
(11042, 'MRS', 'NELSINWOOD', 'AJNCEOT', null, '1932-01-14', false, false, null, 'F', null, null, false, 'ZQA16H', '2007-04-17 13:47:19.955077', 'OMS_OWNER', '2017-05-22 16:06:20.049325'),
(11043, 'MR', 'NELSINWOOD', 'IJAHINA', null, '1952-01-23', false, false, null, 'M', null, null, false, 'MQB87I', '2007-06-04 14:34:40.775857', 'OMS_OWNER', '2017-05-22 16:06:20.068387');
