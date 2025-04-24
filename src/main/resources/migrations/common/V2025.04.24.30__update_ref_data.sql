-- Reference data style updates for PRC-586

--CONTACT_TYPE
UPDATE reference_codes SET description = 'Social ' WHERE group_code = 'CONTACT_TYPE' AND code = 'S';

--DOMESTIC_STS
UPDATE reference_codes SET description = 'Single (not married or in civil partnership)' WHERE group_code = 'DOMESTIC_STS' AND code = 'S';
UPDATE reference_codes SET description = 'Divorced or dissolved marriage' WHERE group_code = 'DOMESTIC_STS' AND code = 'D';
UPDATE reference_codes SET description = 'Separated (no longer living with legal partner)' WHERE group_code = 'DOMESTIC_STS' AND code = 'P';
UPDATE reference_codes SET description = 'Not known' WHERE group_code = 'DOMESTIC_STS' AND code = 'N';

--ID_TYPE
UPDATE reference_codes SET description = 'LIDS number' WHERE group_code = 'ID_TYPE' AND code = 'LIDS';
UPDATE reference_codes SET description = 'PNC number' WHERE group_code = 'ID_TYPE' AND code = 'PNC';
UPDATE reference_codes SET description = 'Shared alias warning' WHERE group_code = 'ID_TYPE' AND code = 'SHARED ALIAS';
UPDATE reference_codes SET description = 'CRO number' WHERE group_code = 'ID_TYPE' AND code = 'CRO';
UPDATE reference_codes SET description = 'Did not enter prison - tagged bail release' WHERE group_code = 'ID_TYPE' AND code = 'TBRI';
UPDATE reference_codes SET description = 'Prison legacy system number' WHERE group_code = 'ID_TYPE' AND code = 'HMPS';
UPDATE reference_codes SET description = 'Home Office reference number' WHERE group_code = 'ID_TYPE' AND code = 'HOREF';
UPDATE reference_codes SET description = 'Probation legacy system number' WHERE group_code = 'ID_TYPE' AND code = 'NPD';
UPDATE reference_codes SET description = 'Driving licence' WHERE group_code = 'ID_TYPE' AND code = 'DL';
UPDATE reference_codes SET description = 'National Insurance number' WHERE group_code = 'ID_TYPE' AND code = 'NINO';
UPDATE reference_codes SET description = 'Passport number' WHERE group_code = 'ID_TYPE' AND code = 'PASS';
UPDATE reference_codes SET description = 'Parkrun number' WHERE group_code = 'ID_TYPE' AND code = 'PARK';
UPDATE reference_codes SET description = 'Scottish PNC number' WHERE group_code = 'ID_TYPE' AND code = 'SPNC';
UPDATE reference_codes SET description = 'Staff pass or identity card' WHERE group_code = 'ID_TYPE' AND code = 'STAFF';
UPDATE reference_codes SET description = 'YJAF identifier' WHERE group_code = 'ID_TYPE' AND code = 'YJAF';
UPDATE reference_codes SET description = 'External relationship' WHERE group_code = 'ID_TYPE' AND code = 'EXTERNAL_REL';
UPDATE reference_codes SET description = 'Merged from legacy LIDS number' WHERE group_code = 'ID_TYPE' AND code = 'MERGE_HMPS';
UPDATE reference_codes SET description = 'Merged from NOMS number' WHERE group_code = 'ID_TYPE' AND code = 'MERGED';
UPDATE reference_codes SET description = 'NHS number' WHERE group_code = 'ID_TYPE' AND code = 'NHS';
UPDATE reference_codes SET description = 'Previous NOMS number' WHERE group_code = 'ID_TYPE' AND code = 'NOMS';
UPDATE reference_codes SET description = 'System identifier' WHERE group_code = 'ID_TYPE' AND code = 'SYSIDENT';

--PHONE_TYPE
UPDATE reference_codes SET description = 'Alternate business' WHERE group_code = 'PHONE_TYPE' AND code = 'ALTB';
UPDATE reference_codes SET description = 'Alternative home' WHERE group_code = 'PHONE_TYPE' AND code = 'ALTH';
UPDATE reference_codes SET description = 'Agency visit line' WHERE group_code = 'PHONE_TYPE' AND code = 'VISIT';

--RELATIONSHIP
UPDATE reference_codes SET description = 'Common law husband' WHERE group_code = 'SOCIAL_RELATIONSHIP' AND code = 'CLH';
UPDATE reference_codes SET description = 'Common law wife' WHERE group_code = 'SOCIAL_RELATIONSHIP' AND code = 'CLW';
UPDATE reference_codes SET description = 'Foster parent' WHERE group_code = 'SOCIAL_RELATIONSHIP' AND code = 'FOP';
UPDATE reference_codes SET description = 'Half-brother' WHERE group_code = 'SOCIAL_RELATIONSHIP' AND code = 'HBRO';
UPDATE reference_codes SET description = 'Half-sister' WHERE group_code = 'SOCIAL_RELATIONSHIP' AND code = 'HSIS';
UPDATE reference_codes SET description = 'In loco parentis' WHERE group_code = 'SOCIAL_RELATIONSHIP' AND code = 'ILP';
UPDATE reference_codes SET description = 'No social relationship' WHERE group_code = 'SOCIAL_RELATIONSHIP' AND code = 'NONE';
UPDATE reference_codes SET description = 'Other social relationship' WHERE group_code = 'SOCIAL_RELATIONSHIP' AND code = 'OTHER';

--OFFICIAL_RELATIONSHIP
UPDATE reference_codes SET description = 'Drug and Alcohol Recovery Team member (DART)' WHERE group_code = 'OFFICIAL_RELATIONSHIP' AND code = 'DART';
UPDATE reference_codes SET description = 'Local authority contact' WHERE group_code = 'OFFICIAL_RELATIONSHIP' AND code = 'LAC';
UPDATE reference_codes SET description = 'Other official relationship' WHERE group_code = 'OFFICIAL_RELATIONSHIP' AND code = 'OTH';
UPDATE reference_codes SET description = 'Police officer' WHERE group_code = 'OFFICIAL_RELATIONSHIP' AND code = 'POL';
UPDATE reference_codes SET description = 'Public protection admin' WHERE group_code = 'OFFICIAL_RELATIONSHIP' AND code = 'PPA';
UPDATE reference_codes SET description = 'Priest or other clergy' WHERE group_code = 'OFFICIAL_RELATIONSHIP' AND code = 'PRE';
UPDATE reference_codes SET description = 'Probation officer' WHERE group_code = 'OFFICIAL_RELATIONSHIP' AND code = 'PROB';
UPDATE reference_codes SET description = 'Prison visitor' WHERE group_code = 'OFFICIAL_RELATIONSHIP' AND code = 'PV';
UPDATE reference_codes SET description = 'Responsible officer' WHERE group_code = 'OFFICIAL_RELATIONSHIP' AND code = 'RO';
UPDATE reference_codes SET description = 'Social worker' WHERE group_code = 'OFFICIAL_RELATIONSHIP' AND code = 'SWO';
UPDATE reference_codes SET description = 'Youth justice worker' WHERE group_code = 'OFFICIAL_RELATIONSHIP' AND code = 'YJW';
UPDATE reference_codes SET description = 'YOT Offender Supervisor or Manager' WHERE group_code = 'OFFICIAL_RELATIONSHIP' AND code = 'YOTWORKER';

--GENDER
UPDATE reference_codes SET description = 'Not known' WHERE group_code = 'GENDER' AND code = 'NK';

-- RESTRICTION
UPDATE reference_codes SET description = 'Access requirements' WHERE group_code = 'RESTRICTION' AND code = 'ACC';
UPDATE reference_codes SET description = 'Child visitors to be vetted' WHERE group_code = 'RESTRICTION' AND code = 'CHILD';
UPDATE reference_codes SET description = 'Disability health concerns' WHERE group_code = 'RESTRICTION' AND code = 'DIHCON';
UPDATE reference_codes SET description = 'Non-contact visit' WHERE group_code = 'RESTRICTION' AND code = 'NONCON';
UPDATE reference_codes SET description = 'Previous info' WHERE group_code = 'RESTRICTION' AND code = 'PREINF';
