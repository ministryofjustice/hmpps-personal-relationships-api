#
# This script is used to run the Book a video link API locally.
#
# It runs with a combination of properties from the default spring profile (in application.yaml) and supplemented
# with the -local profile (from application-local.yml). The latter overrides some of the defaults.
#
# The environment variables here will also override values supplied in spring profile properties, specifically
# around setting the DB properties, SERVER_PORT and client credentials to match those used in the docker-compose files.
#

# Provide the DB connection details to local container-hosted Postgresql DB already running
export DB_SERVER=localhost
export DB_NAME=personal-relationships-db
export DB_USER=contacts
export DB_PASS=contacts
export DB_SSL_MODE=prefer
export DPR_USER=dpr_user
export DPR_PASSWORD=dpr_password

export API_BASE_URL_HMPPS_AUTH=https://sign-in-dev.hmpps.service.justice.gov.uk/auth

export $(cat .env | xargs)  # If you want to set or update the current shell environment e.g. system client and secret.

# Run the application with stdout and local profiles active
SPRING_PROFILES_ACTIVE=stdout,local ./gradlew bootRun

# End

