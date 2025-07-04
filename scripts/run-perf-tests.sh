# Get the token
AUTH_HOST="https://sign-in-dev.hmpps.service.justice.gov.uk"

read -r user secret < <(echo $(kubectl -n hmpps-contacts-dev get secret hmpps-contacts-ui -o json | jq '.data[] |= @base64d' | jq -r '.data.SYSTEM_CLIENT_ID, .data.SYSTEM_CLIENT_SECRET'))

BASIC_AUTH="$(echo -n $user:$secret | base64)"
TOKEN_RESPONSE=$(curl -s -k -d "" -X POST "$AUTH_HOST/auth/oauth/token?grant_type=client_credentials&username=perf_test_user" -H "Authorization: Basic $BASIC_AUTH")
TOKEN=$(echo "$TOKEN_RESPONSE" | jq -er .access_token)
if [[ $? -ne 0 ]]; then
  echo "Failed to read token from credentials response"
  echo "$TOKEN_RESPONSE"
  exit 1
fi

# First, create a file to sore the prisoner numbers and add the header
mkdir -p ../src/gatling/resources/data && echo "prisonerNumber" > ../src/gatling/resources/data/prisoner-numbers-dev.csv

# Then append the prisoner numbers
curl --silent --location 'https://prisoner-search-dev.prison.service.justice.gov.uk/prisoner-search/prison/WMI?page=0&size=2000' \
--header 'accept: application/json' \
--header 'Content-Type: application/json' \
--header "Authorization: Bearer ${TOKEN}" \
| jq -r '.content[].prisonerNumber' >> ../src/gatling/resources/data/prisoner-numbers-dev.csv

# Check if the curl command was successful
if [ $? -eq 0 ]; then
    echo "Prisoner numbers have been successfully saved to prisonerNumbers.txt"
else
    echo "Error: Failed to fetch or process the data"
    exit 1
fi


# This script:
# 1. Retrieves client credentials from a Kubernetes secret
# 2. Run gatling tests,
# to run specific simulation file add the simulation file name
# --simulation uk.gov.justice.digital.hmpps.hmppscontactsapi.simulations.CreateOrUpdateDomesticStatusSimulation
# To run all gatling tests, use --all and remove --simulation simulation.filename


export BASE_URL="https://personal-relationships-api-dev.hmpps.service.justice.gov.uk"
export AUTH_TOKEN=${TOKEN}

echo "Running gatling tests"
# add specific simulation file here to run it
# Users: 60 concurrent users per second
# Duration: 300 seconds (5 minutes)
# Virtual users of 15,000 users
# Pause: 3-5 seconds
# Repeat: 1 time
# Environment: dev
# Response time percentile: 1000ms
# Successful requests percentage: 95%
cd ..
./gradlew gatlingRun --simulation uk.gov.justice.digital.hmpps.hmppscontactsapi.simulations.AddContactRelationshipSimulation \
-DuserCount=60 \
-DtestDuration=300 \
-DtestPauseRangeMin=${GATLING_PAUSE_MIN:-3} \
-DtestPauseRangeMax=${GATLING_PAUSE_MAX:-5} \
-DtestRepeat=${GATLING_TEST_REPEAT:-1} \
-Denvironment=${GATLING_ENVIRONMENT:-dev} \
-DresponseTimePercentile3=${GATLING_RESPONSE_TIME_PERCENTILE:-1000} \
-DsuccessfulRequestsPercentage=${GATLING_SUCCESS_PERCENTAGE:-95}

# Check if the gatling tests were successful
if [ $? -eq 0 ]; then
    echo "Gatling tests completed successfully"
    #  clear test data
    cd scripts
    chmod +x clear-test-data.sh
    ./clear-test-data.sh
else
    echo "Error: Gatling tests failed"
    exit 1
fi
# End
