#!/usr/bin/env bash
set -euo pipefail

# Use this script to run the backfill_contact_audit migration function
# set permission to execute: chmod +x scripts/run-audit-backfill.sh
# require kubectl, psql, jq installed and configured to access the cluster.

# Usage: ./scripts/run-audit-backfill.sh [1|2|3]
# 1 = dev, 2 = preprod, 3 = prod

# How to run the backfill_contact_audit migration function
#1. Confirm migration applied. In DB console:
#           select proname from pg_proc where proname = 'backfill_contact_audit';
#2. Estimate remaining work:
#           select count(*) from public.contact_audit_backfill_progress;
#           select count(*) from public.contact c inner join public.prisoner_contact pc on c.contact_id = pc.contact_id where pc.current_term = true;
#3. Choose batch size (e.g. 10000-20000). Larger batches create fewer rev_info rows but hold locks longer.
#4. Out of hours, open terminal on macOS and connect:
#   Open port-forward connection to database pod (see recreate-db-pod-connect.sh):
#     ./scripts/recreate-db-pod-connect.sh [1|2|3]
#   Run this script in a separate thread to start the backfill.

#   Other optional configurations
#5. (Optional) Set a statement timeout to avoid runaway session:
#   set statement_timeout = '15min';

# Monitoring steps:
#6. Monitor progress - use the check-backfill-progress.sh script or manual queries:
#     ./scripts/check-backfill-progress.sh [environment]
#   Or manually:
#     select count(*) processed from public.contact_audit_backfill_progress;
#     select count(*) total from public.contact c inner join public.prisoner_contact pc on c.contact_id = pc.contact_id where pc.current_term = true;
#7. If you want manual control instead of the script:
#            select backfill_contact_audit(10000);  -- repeat until result = 0
#8. Concurrency: You may run 2–3 sessions in parallel; the FOR UPDATE SKIP LOCKED prevents double work. Monitor load (CPU, I/O) before adding more.
#9. Validate completion: all contacts have audit entries:
#     select count(*) from public.contact c inner join public.prisoner_contact pc on c.contact_id = pc.contact_id
#       where pc.current_term = true and not exists (select 1 from public.contact_audit ca where ca.contact_id = c.contact_id and ca.rev_type = 0);
#10. Record revision count for audit:
#     select count(*) from public.contact_audit where rev_type = 0;

# Cleanup (after completion):
# 11. Run the cleanup migration V2025.11.24.47__cleanup_contact_audit_backfill.sql(Will be done as part of the next PR)
#     This will drop the tracking table, temporary function, and temporary index.
#12. Disconnect: \q


read -p "Select environment (1=dev, 2=preprod, 3=prod): " ENV_CHOICE
case "$ENV_CHOICE" in
  1) NAMESPACE="hmpps-personal-relationships-dev"; ENV_NAME="dev" ;;
  2) NAMESPACE="hmpps-personal-relationships-preprod"; ENV_NAME="preprod" ;;
  3) NAMESPACE="hmpps-personal-relationships-prod"; ENV_NAME="prod" ;;
  *) echo "Invalid selection. Usage: 1=dev, 2=preprod, 3=prod"; exit 1 ;;
esac

echo "Environment selected: $ENV_NAME ($NAMESPACE)"
read -p "Confirm by re-typing the environment name (dev/preprod/prod) to continue, or anything else to abort: " CONFIRM
if [[ "$CONFIRM" != "$ENV_NAME" ]]; then
  echo "Confirmation failed. Aborting."
  exit 1
fi

  # Get the database credentials and RDS instance address from the Kubernetes secret
  read -r database_name database_password database_username < <(echo $(kubectl -n $NAMESPACE get secrets rds-postgresql-instance-output -o json | jq '.data[] |= @base64d' | jq -r '.data.database_name, .data.database_password, .data.database_username'))


DB_URL="postgresql://${database_username}:${database_password}@localhost:5433/${database_name}?sslmode=require"
BATCH_SIZE=20000
MAX_BATCHES=50    # maximum number of batches to process in one run (0 = unlimited)
SLEEP_BETWEEN=0   # set to 1–2 if you want to reduce pressure

echo "Starting backfill: batch_size=$BATCH_SIZE max_batches=$MAX_BATCHES"
start_ts=$(date +%s)
total_processed=0
batch_num=0

while true; do
  processed=$(psql "$DB_URL" -At -c "SELECT backfill_contact_audit($BATCH_SIZE);")
  batch_num=$((batch_num + 1))
  if [[ "$processed" =~ ^[0-9]+$ ]]; then
    total_processed=$((total_processed + processed))
  fi
  now=$(date +%s)
  elapsed=$((now - start_ts))
  if [ "$processed" -eq 0 ]; then
    echo "No more rows. Finished after $batch_num batches. Total processed=$total_processed. Elapsed=${elapsed}s."
    break
  fi
  if [ "$MAX_BATCHES" -gt 0 ] && [ "$batch_num" -ge "$MAX_BATCHES" ]; then
    echo "Reached maximum batch limit ($MAX_BATCHES). Total processed=$total_processed. Elapsed=${elapsed}s."
    echo "Run the script again to continue processing remaining rows."
    break
  fi
  rate=$(awk -v p="$total_processed" -v e="$elapsed" 'BEGIN{ if(e>0) printf "%.1f", p/e; else print "0"; }')
  echo "$(date) batch=$batch_num processed=$processed cumulative=$total_processed elapsed=${elapsed}s avg_rows_per_sec=$rate"
  sleep "$SLEEP_BETWEEN"
done
