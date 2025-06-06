#!/bin/bash
# Use this script after running the gatling tests, to clean up the test data created by perf_test_user

# Get the database credentials and RDS instance address from the Kubernetes secret
    read -r database_name database_password database_username rds_instance_address < <(echo $(kubectl -n hmpps-personal-relationships-dev get secrets rds-postgresql-instance-output -o json | jq '.data[] |= @base64d' | jq -r '.data.database_name, .data.database_password, .data.database_username, .data.rds_instance_address'))

# Check if the port-forward pod exists
  if kubectl -n hmpps-personal-relationships-dev get pod port-forward-pod &>/dev/null; then
    echo "port-forward-pod already exists"
  else
    # Create the port-forward pod
    kubectl -n hmpps-personal-relationships-dev run port-forward-pod --image=ministryofjustice/port-forward --env="REMOTE_HOST=$rds_instance_address" --env="REMOTE_PORT=5432" --env="LOCAL_PORT=5432"
    # Wait for the pod to be ready
    kubectl -n hmpps-personal-relationships-dev wait --for=condition=Ready pod/port-forward-pod --timeout=60s
        echo "port-forward-pod created"
  fi


  # Kill any existing processes using port 5433
  if lsof -ti :5433 >/dev/null; then
    lsof -ti :5433 | xargs -r kill
  fi

  # Start port-forwarding
  kubectl -n hmpps-personal-relationships-dev port-forward port-forward-pod 5433:5432 &

# Connect to the database and delete test data created by perf_test_user
  DB_HOST="localhost"
  DB_PORT="5433"
  DB_NAME=$database_name
  DB_USER=$database_username
  DB_PASSWORD=$database_password


# Wait for the port to be open
  while ! nc -z $DB_HOST $DB_PORT; do
    sleep 1
  done

#  Run the SQL commands to delete test data
export PGPASSWORD=$DB_PASSWORD
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME <<EOF
delete from prisoner_number_of_children where created_by ='perf_test_user' returning *;
delete from prisoner_domestic_status where created_by ='perf_test_user' returning *;
delete from prisoner_contact where created_by ='perf_test_user' returning *;
delete from contact where created_by ='perf_test_user' returning *;
EOF

unset PGPASSWORD
echo "completed deleting test data created by perf_test_user"
# Check if the port-forward pod exists
if lsof -ti :5433 >/dev/null; then
  lsof -ti :5433 | xargs -r kill
  echo "Killed port-forward process on port 5433"
fi