#!/bin/bash

# Script to obtain an OAuth2 authentication token for API testing
#
# This script:
# 1. Retrieves client credentials from a Kubernetes secret
# 2. Makes a POST request to get an OAuth2 token from the auth service
# 3. Updates a Kubernetes secret with the new JWT token
# 4. validate if the token value updated successfully
#
# Example:
#
# $ ./update-jwt-token-perf-tests.sh
#
AUTH_HOST="https://sign-in-dev.hmpps.service.justice.gov.uk"

read -r user secret < <(echo $(kubectl -n hmpps-contacts-dev get secret hmpps-contacts-ui -o json | jq '.data[] |= @base64d' | jq -r '.data.SYSTEM_CLIENT_ID, .data.SYSTEM_CLIENT_SECRET'))

BASIC_AUTH="$(echo -n $user:$secret | base64)"
TOKEN_RESPONSE=$(curl -s -k -d "" -X POST "$AUTH_HOST/auth/oauth/token?grant_type=client_credentials&username=prabash_gen" -H "Authorization: Basic $BASIC_AUTH")
TOKEN=$(echo "$TOKEN_RESPONSE" | jq -er .access_token)

kubectl -n hmpps-personal-relationships-dev patch secret hmpps-personal-relationships-perf-test-creds --type='json' -p='[{"op": "replace", "path": "/data/JWT", "value":"'$(echo -n "$TOKEN" | base64)'"}]'

read -r user secret jwt < <(echo $(kubectl -n hmpps-personal-relationships-dev get secret hmpps-personal-relationships-perf-test-creds -o json  -o json | jq '.data[] |= @base64d' | jq -r '.data.SYSTEM_CLIENT_ID, .data.SYSTEM_CLIENT_SECRET, .data.JWT'))
echo
# Verify JWT matches TOKEN
if [ "$jwt" = "$TOKEN" ]; then
    echo "Success: JWT TOKEN added successfully"
else
    echo "Error: JWT does not match TOKEN"
    exit 1
fi
echo

# End
