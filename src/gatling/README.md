# Gatling Performance Tests for HMPPS Personal Relationships API

This directory contains Gatling performance tests for the HMPPS Personal Relationships API.

## Prerequisites

- JDK 21 or higher
- Gradle 8.x or higher
- Running instance of the HMPPS Personal Relationships API

## Running the Tests

You can run the Gatling tests using the following Gradle command:

```bash
./gradlew gatlingRun
```
Please refer to run-perf-tests.sh for easy run of the gatling tests

Alternatively use this script to run gatling tests [run-perf-tests.sh](../../scripts/run-perf-tests.sh).

### Configuration Options

You can configure the tests using the following system properties:

- `baseUrl`: The base URL of the API (default: `http://localhost:8080`)
- `userCount`: The number of virtual users to simulate (default: `10`)
- `testDuration`: The duration of the test in seconds (default: `60`)
- `testPauseRangeMin`: Attach a pause computed randomly between 2 values – the pause maximum (default: `3`)
- `testPauseRangeMax`: Attach a pause computed randomly between 2 values – the pause maximum (default: `5`)
- `testRepeat`: Number of times to repeat the test (default: `5`)
- `environment`: The envirnoment (default: `dev`)
- `responseTimePercentile3`: The Response time threshold (in milliseconds) that 99.9% of requests should be under (default: `1000`)
- `successfulRequestsPercentage`: The Minimum percentage of requests that should be successful for the test to pass (default: `95`)

Example:

```bash
./gradlew  gatlingRun
   -DAUTH_URL=https://sign-in-dev.hmpps.service.justice.gov.uk/auth/oauth/token \
   -DCLIENT_ID='**REDACTED**' \
   -DCLIENT_SECRET='**REDACTED**' \
   -DbaseUrl=http://localhost:8080 \
   -DuserCount=30 \
   -DtestDuration=30 \
   -DtestPauseRangeMin=3 \
   -DtestPauseRangeMax=5 \
   -DtestRepeat=5 \
   -Denvironment=dev \
   -responseTimePercentile3=1000 \
   -successfulRequestsPercentage=99 \
```

## Test Scenarios

The following test scenarios are included:

- **Get prisoner contacts**: Tests the `/prisoner/#{personIdentifier}/contact` endpoint
- **Get prisoner's domestic status**: Tests the `/prisoner/#{personIdentifier}/domestic-status` endpoint
- **Get prisoner's number of children**: Tests the `/prisoner/#{personIdentifier}/number-of-children` endpoint

## Test Data

The tests use sample data from the `src/gatling/resources/data` directory. You may need to update this data to match your environment.

## Viewing the Results

After running the tests, the results will be available in the `build/reports/gatling` directory. Open the `index.html` file in a web browser to view the results.

## Adding New Tests

To add new tests:

1. Create a new simulation class in the `src/gatling/kotlin/uk/gov/justice/digital/hmpps/hmppscontactsapi/simulations` directory
2. Extend the `BaseSimulation` class or implement the `Simulation` interface
3. Define your scenarios and assertions
4. Add any required test data to the `src/gatling/resources/data` directory

## Performance Criteria

The tests include the following performance criteria:

- 95% of requests should be successful
- 95% of requests should complete in less than 1000ms

You can adjust these criteria in the simulation classes as needed.