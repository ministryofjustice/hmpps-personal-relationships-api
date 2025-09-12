-- Rename columns on prisoner_restrictions
    ALTER TABLE prisoner_restrictions
        RENAME COLUMN effective_date TO start_date;

    ALTER TABLE prisoner_restrictions
        RENAME COLUMN comment_text TO comments;