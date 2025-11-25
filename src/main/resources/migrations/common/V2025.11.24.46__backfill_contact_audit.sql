------------------------------------------------------------
-- Contact audit initial backfill migration
-- Enhancements:
--  * Idempotent batch processing (skips contacts already audited with rev_type=0)
--  * Concurrency-safe selection using FOR UPDATE SKIP LOCKED
--  * Partial indexes for performance
--  * Returns number of rows processed; emits NOTICEs
--  * Avoids creating empty revisions
--  * Unique partial index prevents duplicate initial audit rows
------------------------------------------------------------
-- 1. Helper column to track processed rows (retained for operational visibility)
------------------------------------------------------------
ALTER TABLE public.contact
    ADD COLUMN IF NOT EXISTS initial_audit_done BOOLEAN DEFAULT FALSE;

------------------------------------------------------------
-- 2. Indexes
--    Drop any previous non-partial index and create optimized partial index
------------------------------------------------------------
DROP INDEX IF EXISTS idx_contact_initial_audit_done; -- old design (non-partial)
CREATE INDEX IF NOT EXISTS idx_contact_initial_audit_pending
    ON public.contact(contact_id)
    WHERE initial_audit_done = FALSE;

-- Unique partial index to ensure only one initial (rev_type=0) audit row per contact
CREATE UNIQUE INDEX IF NOT EXISTS ux_contact_audit_initial
    ON public.contact_audit(contact_id)
    WHERE rev_type = 0;

------------------------------------------------------------
-- 3. Backfill function (batch-oriented)
--    Single snapshot of batch locked; avoids re-evaluating selection.
--    Skips contacts already having an initial audit entry (rev_type=0).
--    Returns number of rows processed. Emits NOTICE messages for observability.
------------------------------------------------------------
CREATE OR REPLACE FUNCTION backfill_contact_audit(batch_size INT DEFAULT 10000)
    RETURNS INTEGER
    LANGUAGE plpgsql
AS $$
DECLARE
    new_rev_id BIGINT;
    rows_processed INT;
    batch_ids BIGINT[];
BEGIN
    -- Lock and capture batch once
    WITH batch AS (
        SELECT c.contact_id
        FROM public.contact c
        inner join prisoner_contact pc
        on c.contact_id = pc.contact_id
        WHERE pc.current_term = true and c.initial_audit_done = FALSE
          AND NOT EXISTS (
            SELECT 1 FROM public.contact_audit ca
            WHERE ca.contact_id = c.contact_id AND ca.rev_type = 0
        )
        ORDER BY c.contact_id
            LIMIT batch_size
            FOR UPDATE SKIP LOCKED
    )
    SELECT array_agg(contact_id), COUNT(*) INTO batch_ids, rows_processed FROM batch;

    IF rows_processed IS NULL OR rows_processed = 0 THEN
        RAISE NOTICE 'No rows to backfill.';
        RETURN 0;
    END IF;

    -- Create revision only if we have rows
    INSERT INTO public.rev_info (timestamp, username)
    VALUES (NOW(), 'audit_backfill')
    RETURNING id INTO new_rev_id;

    -- Insert audit rows for the captured batch
    INSERT INTO public.contact_audit (
        contact_id,
        rev_id,
        rev_type,
        title,
        last_name,
        first_name,
        middle_names,
        date_of_birth,
        deceased_date,
        staff_flag,
        remitter_flag,
        gender,
        domestic_status,
        language_code,
        interpreter_required,
        created_by,
        created_time,
        updated_by,
        updated_time
    )
    SELECT
        c.contact_id,
        new_rev_id,
        0,
        c.title,
        c.last_name,
        c.first_name,
        c.middle_names,
        c.date_of_birth,
        c.deceased_date,
        c.staff_flag,
        c.remitter_flag,
        c.gender,
        c.domestic_status,
        c.language_code,
        c.interpreter_required,
        c.created_by,
        c.created_time,
        c.updated_by,
        c.updated_time
    FROM public.contact c
    WHERE c.contact_id = ANY(batch_ids);

    -- Mark processed contacts
    UPDATE public.contact c
    SET initial_audit_done = TRUE
    WHERE c.contact_id = ANY(batch_ids);

    RAISE NOTICE 'Backfill batch complete. revision %, rows %', new_rev_id, rows_processed;
    RETURN rows_processed;
EXCEPTION WHEN unique_violation THEN
    -- In rare race conditions duplicate insert could occur; skip and continue
    RAISE WARNING 'Unique violation encountered - possible concurrent batch overlap.';
    RETURN 0;
END;
$$;
