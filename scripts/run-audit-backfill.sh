#!/usr/bin/env bash
set -euo pipefail

# Use this script to run the backfill_contact_audit migration function
# set permission to execute: chmod +x scripts/run-audit-backfill.sh

# Usage: ./recreate-db-pod-connect.sh [1|2|3]
# 1 = dev, 2 = preprod, 3 = prod

# How to run the backfill_contact_audit migration function
#1. Confirm migration applied. In DB console:
#           select proname from pg_proc where proname = 'backfill_contact_audit';
#2. Estimate remaining work:
#           select count(*) from public.contact where initial_audit_done = false;
#3. Choose batch size (e.g. 10000). Larger batches create fewer rev\_info rows but hold locks longer.
#4. Out of hours, open terminal on macOS and connect:
#   Open port-forward connection to database pod (see recreate-db-pod-connect.sh):
#     ./scripts/recreate-db-pod-connect.sh [1|2|3]
#   Run this script in a separate thread to start the backfill:  Run looping block until function returns 0:

#   Other optional configurations
#5. (Optional) Set a statement timeout to avoid runaway session:
#   set statement_timeout = '15min';

# Monitoring steps:
#6. Monitor progress between loops (in a separate session if desired):
#            select count(*) remaining from public.contact where initial_audit_done = false;
#7. If you want manual control instead of DO loop:
#            select backfill_contact_audit(10000);  -- repeat until result = 0
#8. Concurrency: You may run 2–3 sessions in parallel; the FOR UPDATE SKIP LOCKED prevents double work. Monitor load (CPU, I/O) before adding more.
#9. Validate completion: remaining count = 0 and no contacts lacking an initial audit row:
#             select count(*) from public.contact c where not exists (select 1 from public.contact_audit ca where ca.contact_id = c.contact_id and ca.rev_type = 0);
#10. Record revision count for audit:
#             select count(*) from public.contact_audit where rev_type = 0;

# Cleanup (optional):
# 11. remove helper functions below
#  drop COLUMN initial_audit_done
#  drop INDEX idx_contact_initial_audit_pending
#12. Disconnect: \q


  read -p "Select environment (1=dev, 2=preprod, 3=prod): " ENV_CHOICE
  if [[ "$ENV_CHOICE" == "1" ]]; then
    NAMESPACE="hmpps-personal-relationships-dev"
  elif [[ "$ENV_CHOICE" == "2" ]]; then
    NAMESPACE="hmpps-personal-relationships-preprod"
  elif [[ "$ENV_CHOICE" == "3" ]]; then
    NAMESPACE="hmpps-personal-relationships-prod"
  else
    echo "Invalid selection. Usage: 1=dev, 2=preprod, 3=prod"
    exit 1
  fi


  # Use this script after running the gatling tests, to clean up the test data created by perf_test_user

  # Get the database credentials and RDS instance address from the Kubernetes secret
  read -r database_name database_password database_username rds_instance_address < <(echo $(kubectl -n $NAMESPACE get secrets rds-postgresql-instance-output -o json | jq '.data[] |= @base64d' | jq -r '.data.database_name, .data.database_password, .data.database_username, .data.rds_instance_address'))


DB_URL="postgresql://${database_username}:${database_password}@localhost:5433/${database_name}?sslmode=require"
BATCH_SIZE=20000
SLEEP_BETWEEN=0   # set to 1–2 if you want to reduce pressure

echo "Starting backfill: batch_size=$BATCH_SIZE"
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
  rate=$(awk -v p="$total_processed" -v e="$elapsed" 'BEGIN{ if(e>0) printf "%.1f", p/e; else print "0"; }')
  echo "$(date) batch=$batch_num processed=$processed cumulative=$total_processed elapsed=${elapsed}s avg_rows_per_sec=$rate"
  sleep "$SLEEP_BETWEEN"
done
