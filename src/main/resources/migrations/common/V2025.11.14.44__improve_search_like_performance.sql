create index if not exists idx_soundex_lastname on contact(soundex(last_name));
create index if not exists idx_soundex_firstname on contact(soundex(first_name));
create index if not exists idx_soundex_middlenames on contact(soundex(middle_names));
create index if not exists idx_contact_address_end_date on contact_address(end_date);
create index if not exists idx_contact_address_start_date on contact_address(start_date);
-- improve the performance of joins on contact_id
create index idx_contact_id_as_text on contact(cast(contact_id as text));
