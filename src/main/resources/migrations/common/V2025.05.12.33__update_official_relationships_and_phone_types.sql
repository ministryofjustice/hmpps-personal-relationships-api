-- Further reference data style updates for PRC-586 and missing disabled OFFICIAL_RELATIONSHIP

--OFFICIAL_RELATIONSHIP updates
UPDATE reference_codes SET description = 'Case administrator' WHERE group_code = 'OFFICIAL_RELATIONSHIP' AND code = 'CA';
UPDATE reference_codes SET description = 'Community offender manager' WHERE group_code = 'OFFICIAL_RELATIONSHIP' AND code = 'COM';
UPDATE reference_codes SET description = 'CuSP officer' WHERE group_code = 'OFFICIAL_RELATIONSHIP' AND code = 'CUSPO';
UPDATE reference_codes SET description = 'CuSP officer (backup)' WHERE group_code = 'OFFICIAL_RELATIONSHIP' AND code = 'CUSPO2';
UPDATE reference_codes SET description = 'Drug and alcohol recovery team member (DART)' WHERE group_code = 'OFFICIAL_RELATIONSHIP' AND code = 'DART';
UPDATE reference_codes SET description = 'Family liaison officer' WHERE group_code = 'OFFICIAL_RELATIONSHIP' AND code = 'FLO';
UPDATE reference_codes SET description = 'Offender supervisor' WHERE group_code = 'OFFICIAL_RELATIONSHIP' AND code = 'OFS';
UPDATE reference_codes SET description = 'Prison offender manager' WHERE group_code = 'OFFICIAL_RELATIONSHIP' AND code = 'POM';
UPDATE reference_codes SET description = 'Resettlement practitioner' WHERE group_code = 'OFFICIAL_RELATIONSHIP' AND code = 'RESPRA';
UPDATE reference_codes SET description = 'Resettlement worker' WHERE group_code = 'OFFICIAL_RELATIONSHIP' AND code = 'RW';
UPDATE reference_codes SET description = 'Youth justice service case manager' WHERE group_code = 'OFFICIAL_RELATIONSHIP' AND code = 'YJSCM';
UPDATE reference_codes SET description = 'Youth justice service' WHERE group_code = 'OFFICIAL_RELATIONSHIP' AND code = 'Youth Justice Service';
UPDATE reference_codes SET description = 'YOT offender supervisor or manager' WHERE group_code = 'OFFICIAL_RELATIONSHIP' AND code = 'YOTWORKER';

-- OFFICIAL_RELATIONSHIP missing but should be disabled
INSERT INTO reference_codes (group_code, code, description, display_order, is_active, created_by)
VALUES ('OFFICIAL_RELATIONSHIP', 'LANDLORD', 'Landlord', 99, false, 'JAMES'),
       ('OFFICIAL_RELATIONSHIP', 'LIFERBACK', 'Lifer back up officer', 99, false, 'JAMES');

--PHONE_TYPE
UPDATE reference_codes SET description = 'Alternate home' WHERE group_code = 'PHONE_TYPE' AND code = 'ALTH';
