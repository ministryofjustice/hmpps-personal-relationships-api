------------------------------------------------------------
-- Contact audit initial backfill migration
-- Enhancements:
--  * Idempotent batch processing (skips contacts already audited with rev_type=0)
--  * Concurrency-safe selection using FOR UPDATE SKIP LOCKED
--  * Partial indexes for performance
--  * Returns number of rows processed; emits NOTICEs
--  * Avoids creating empty revisions
--  * Unique partial index prevents duplicate initial audit rows
--  * Uses separate tracking table (can be safely dropped after completion)
------------------------------------------------------------
-- 1. Tracking table to monitor backfill progress
--    This table can be safely dropped after backfill completion
------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.contact_audit_backfill_progress (
    contact_id BIGINT PRIMARY KEY,
    processed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    rev_id BIGINT NOT NULL
);

------------------------------------------------------------
-- 2. Indexes
--    Optimized partial index for finding unprocessed contacts
------------------------------------------------------------
-- Index on contact_audit for fast lookup of existing initial audit entries
CREATE INDEX IF NOT EXISTS idx_contact_audit_rev_type_0
    ON public.contact_audit(contact_id)
    WHERE rev_type = 0;

-- Unique partial index to ensure only one initial (rev_type=0) audit row per contact
-- Note: PostgreSQL doesn't support partial unique constraints directly via ALTER TABLE,
-- so we create a unique partial index which serves the same purpose
CREATE UNIQUE INDEX IF NOT EXISTS ux_contact_audit_initial
    ON public.contact_audit(contact_id)
    WHERE rev_type = 0;

------------------------------------------------------------
-- 3. Backfill function (batch-oriented)
--    Single snapshot of batch locked; avoids re-evaluating selection.
--    Skips contacts already having an initial audit entry (rev_type=0).
--    Uses tracking table instead of modifying contact table.
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
    actual_inserted INT := 0;
BEGIN
    -- Lock and capture batch in one atomic operation
    -- This prevents race conditions between threads
    WITH batch AS (
        SELECT c.contact_id
        FROM public.contact c
        INNER JOIN public.prisoner_contact pc
            ON c.contact_id = pc.contact_id
        WHERE pc.current_term = true
          AND NOT EXISTS (
            SELECT 1 FROM public.contact_audit_backfill_progress p
            WHERE p.contact_id = c.contact_id
          )
        ORDER BY c.contact_id
        LIMIT batch_size
        FOR UPDATE OF c SKIP LOCKED
    )
    SELECT array_agg(contact_id), COUNT(*) INTO batch_ids, rows_processed FROM batch;

    IF rows_processed IS NULL OR rows_processed = 0 THEN
        RAISE NOTICE 'No rows to backfill.';
        RETURN 0;
    END IF;

    -- Immediately mark these contacts as being processed to prevent other threads from picking them up
    INSERT INTO public.contact_audit_backfill_progress (contact_id, rev_id)
    SELECT unnest(batch_ids), 0  -- Use 0 as placeholder; will update after creating revision
    ON CONFLICT (contact_id) DO NOTHING;

    -- Create revision
    INSERT INTO public.rev_info (timestamp, username)
    VALUES (NOW(), 'audit_backfill')
    RETURNING id INTO new_rev_id;

    -- Insert audit rows for the captured batch
    INSERT INTO public.contact_audit (
        contact_id, rev_id, rev_type,
        title, last_name, first_name, middle_names,
        date_of_birth, deceased_date, staff_flag, remitter_flag,
        gender, domestic_status, language_code, interpreter_required,
        created_by, created_time, updated_by, updated_time
    )
    SELECT
        c.contact_id, new_rev_id, 0,
        c.title, c.last_name, c.first_name, c.middle_names,
        c.date_of_birth, c.deceased_date, c.staff_flag, c.remitter_flag,
        c.gender, c.domestic_status, c.language_code, c.interpreter_required,
        c.created_by, c.created_time, c.updated_by, c.updated_time
    FROM public.contact c
    WHERE c.contact_id = ANY(batch_ids)
    ON CONFLICT DO NOTHING;

    GET DIAGNOSTICS actual_inserted = ROW_COUNT;

    -- Update tracking table with actual revision ID
    UPDATE public.contact_audit_backfill_progress
    SET rev_id = new_rev_id, processed_at = NOW()
    WHERE contact_id = ANY(batch_ids);

    RAISE NOTICE 'Backfill batch complete. revision %, rows processed %, audit rows inserted %',
                  new_rev_id, rows_processed, actual_inserted;
    RETURN rows_processed;
END;
$$;
