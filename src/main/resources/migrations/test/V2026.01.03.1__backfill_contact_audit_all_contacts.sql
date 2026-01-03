
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
    WITH batch AS (SELECT c.contact_id
               FROM public.contact c
               WHERE NOT EXISTS (SELECT 1
                                 FROM public.contact_audit_backfill_progress p
                                 WHERE p.contact_id = c.contact_id)
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