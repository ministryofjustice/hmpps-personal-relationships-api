#!/bin/bash

#!/bin/bash
# Use this script to recreate the port-forward pod for connecting to the RDS database
# and start port-forwarding to your local machine on port 5433.
# Make sure you have kubectl and jq installed and configured to access the cluster.

# Usage: ./recreate-db-pod-connect.sh [1|2|3]
# 1 = dev, 2 = preprod, 3 = prod

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

  # Get the database credentials and RDS instance address from the Kubernetes secret
  read -r database_name database_password database_username rds_instance_address < <(echo $(kubectl -n $NAMESPACE get secrets rds-postgresql-instance-output -o json | jq '.data[] |= @base64d' | jq -r '.data.database_name, .data.database_password, .data.database_username, .data.rds_instance_address'))
  # Always delete the port-forward pod if it exists
  echo $database_name
  echo $database_username
  echo $database_password
  echo $rds_instance_address