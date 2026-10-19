INSERT INTO contact (contact_id, first_name, last_name, middle_names, date_of_birth, created_by)
SELECT 9000000 + n,
       CASE WHEN n % 2 = 0 THEN 'Lex' ELSE 'Lexa' END,
       'Capsearch',
       CASE WHEN n % 3 = 0 THEN 'Middlex' ELSE 'Tess' END,
       DATE '1980-04-12',
       'SEARCH_CAP_TEST'
FROM generate_series(1, 2003) AS n;
