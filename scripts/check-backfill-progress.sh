#!/usr/bin/env bash
#
# Check progress of contact audit backfill
# Usage: ./check-backfill-progress.sh [environment]
#
# Example: ./check-backfill-progress.sh dev
#

set -euo pipefail

# Check if environment is provided, otherwise default to 'dev'
read -p "Select environment (1=dev, 2=preprod, 3=prod): " ENV_CHOICE
case "$ENV_CHOICE" in
  1) NAMESPACE="hmpps-personal-relationships-dev"; ENV="dev" ;;
  2) NAMESPACE="hmpps-personal-relationships-preprod"; ENV="preprod" ;;
  3) NAMESPACE="hmpps-personal-relationships-prod"; ENV="prod" ;;
  *) echo "Invalid selection. Usage: 1=dev, 2=preprod, 3=prod"; exit 1 ;;
esac

echo "Environment selected: $ENV ($NAMESPACE)"
read -p "Confirm by re-typing the environment name (dev/preprod/prod) to continue, or anything else to abort: " CONFIRM
if [[ "$CONFIRM" != "$ENV" ]]; then
  echo "Confirmation failed. Aborting."
  exit 1
fi

  # Get the database credentials and RDS instance address from the Kubernetes secret
  read -r database_name database_password database_username < <(echo $(kubectl -n $NAMESPACE get secrets rds-postgresql-instance-output -o json | jq '.data[] |= @base64d' | jq -r '.data.database_name, .data.database_password, .data.database_username'))

# Build the connection string
DB_URL="postgresql://${database_username}:${database_password}@localhost:5433/${database_name}?sslmode=require"

echo "=========================================="
echo "Contact Audit Backfill Progress Report"
echo "Environment: $ENV"
echo "=========================================="
echo ""

# Total contacts that need backfilling
echo "Total contacts with current_term=true:"
psql "$DB_URL" -At -c "
SELECT COUNT(DISTINCT c.contact_id)
FROM contact c
INNER JOIN prisoner_contact pc ON c.contact_id = pc.contact_id
WHERE pc.current_term = true;"

echo ""
echo "Contacts already processed (in tracking table):"
psql "$DB_URL" -At -c "
SELECT COUNT(*) FROM contact_audit_backfill_progress;"

echo ""
echo "Contacts with initial audit entry (rev_type=0):"
psql "$DB_URL" -At -c "
SELECT COUNT(DISTINCT contact_id)
FROM contact_audit
WHERE rev_type = 0;"

echo ""
echo "Remaining contacts to process:"
psql "$DB_URL" -At -c "
SELECT COUNT(DISTINCT c.contact_id)
FROM contact c
INNER JOIN prisoner_contact pc ON c.contact_id = pc.contact_id
WHERE pc.current_term = true
  AND NOT EXISTS (
    SELECT 1 FROM contact_audit_backfill_progress p
    WHERE p.contact_id = c.contact_id
  )
  AND NOT EXISTS (
    SELECT 1 FROM contact_audit ca
    WHERE ca.contact_id = c.contact_id AND ca.rev_type = 0
  );"

echo ""
echo "Progress by hour (last 24 hours):"
psql "$DB_URL" -c "
SELECT
    DATE_TRUNC('hour', processed_at) AS hour,
    COUNT(*) AS contacts_processed,
    MIN(rev_id) AS min_rev_id,
    MAX(rev_id) AS max_rev_id
FROM contact_audit_backfill_progress
WHERE processed_at >= NOW() - INTERVAL '24 hours'
GROUP BY DATE_TRUNC('hour', processed_at)
ORDER BY hour DESC
LIMIT 24;"

echo ""
echo "=========================================="

