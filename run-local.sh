#
# This script is used to run the Book a video link API locally.
#
# It runs with a combination of properties from the default spring profile (in application.yaml) and supplemented
# with the -local profile (from application-local.yml). The latter overrides some of the defaults.
#
# The environment variables here will also override values supplied in spring profile properties, specifically
# around setting the DB properties, SERVER_PORT and client credentials to match those used in the docker-compose files.
#
# The default port for the local postgresq docker container is the standard 5432/tcp, but can be overridden here by
# setting the LOCAL_DB_PORT environment variable. e.g. `export LOCAL_DB_PORT=5555`.

# The default port for the integration test postgres container is the standard 5432/tcp, but you can also override this
# by setting the POSTGRES_TEST_DB_PORT environment variable. e.g. `export POSTGRES_TEST_DB_PORT=9559`. You may need to
# do this if you have an existing running database on the default port, on your machine.

# Provide the DB connection details to local docker Postgresql DB already running
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

