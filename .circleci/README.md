# CircleCI Configuration

This directory contains the CircleCI configuration for the HMPPS Personal Relationships API.

## Workflows

The following workflows are configured:

- **build-test-and-deploy**: Main workflow that runs on every commit. Includes validation, Gatling performance tests, Docker build, and deployment.
- **security**: Runs security scans on a daily schedule.
- **security-weekly**: Runs weekly security policy scans.
- **performance**: Runs Gatling performance tests on a weekly schedule.

## Jobs

### Gatling Performance Tests

The `gatling_performance_tests` job runs Gatling performance tests against the API. It can be configured using the following environment variables:

| Environment Variable | Description | Default Value |
|---------------------|-------------|---------------|
| `GATLING_BASE_URL` | The base URL of the API to test | `http://localhost:8080` |
| `GATLING_USER_COUNT` | The number of virtual users to simulate | `10` |
| `GATLING_TEST_DURATION` | The duration of the test in seconds | `60` |
| `GATLING_PAUSE_MIN` | Minimum pause duration (in seconds) between simulated user actions | `3` |
| `GATLING_PAUSE_MAX` | Maximum pause duration (in seconds) between simulated user actions | `5` |
| `GATLING_TEST_REPEAT` | Number of iterations to repeat the test scenario | `5` |
| `GATLING_ENVIRONMENT` | Target environment for the load test | `dev` |
| `GATLING_RESPONSE_TIME_PERCENTILE` | Response time threshold (in milliseconds) that 99.9% of requests should be under | `1000` |
| `GATLING_SUCCESS_PERCENTAGE` | Minimum percentage of requests that should be successful for the test to pass | `95` |

### Authentication for Gatling Tests

For the Gatling tests to authenticate with the API, you need to set the following environment variables in the CircleCI project settings:

- `AUTH_URL`: The URL of the authentication service
- `CLIENT_ID`: The client ID for authentication
- `CLIENT_SECRET`: The client secret for authentication

Alternatively, you can set `AUTH_TOKEN` directly with a valid JWT token.

## Running Gatling Tests Locally

To run the Gatling tests locally, use the following command:

```bash
./gradlew runGatling \
  -DbaseUrl=http://localhost:8080 \
  -DuserCount=10 \
  -DtestDuration=60 \
  -DtestPauseRangeMin=3 \
  -DtestPauseRangeMax=5 \
  -DtestRepeat=5 \
  -Denvironment=dev \
  -DresponseTimePercentile3=1000 \
  -DsuccessfulRequestsPercentage=95
```

## Viewing Gatling Test Results

After running the tests, the results will be available in the CircleCI artifacts tab. Look for the `gatling-reports` directory.