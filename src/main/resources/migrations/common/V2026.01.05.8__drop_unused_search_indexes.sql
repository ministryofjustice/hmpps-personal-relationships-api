-- Remove unused indexes from contact - more effective indexes are created below
DROP index IF EXISTS idx_soundex_lastname;
DROP index IF EXISTS idx_soundex_firstname;
DROP index IF EXISTS idx_soundex_middlenames;
