package uk.gov.justice.digital.hmpps.personalrelationships.integration.container

import io.github.cdimascio.dotenv.dotenv
import org.slf4j.LoggerFactory
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import java.io.IOException
import java.net.ServerSocket

object PostgresContainer {
  // Override the external container port if the environment variable exists - otherwise default to 5432/tcp
  private val dotenv = dotenv {
    ignoreIfMissing = true // don't crash if .env is absent
  }
  val hostPort: Int =
    dotenv["POSTGRES_TEST_DB_PORT"]?.toInt()
      ?: System.getenv("POSTGRES_TEST_DB_PORT")?.toInt()
      ?: 5432

  // Binding externalPort:internalContainer port
  private val bindings = listOf("$hostPort:5432")

  private val log = LoggerFactory.getLogger(this::class.java)

  val instance: PostgreSQLContainer<Nothing>? by lazy { startPostgresqlIfNotRunning() }

  // This only starts a TestContainer if postgresql is not already running
  private fun startPostgresqlIfNotRunning(): PostgreSQLContainer<Nothing>? {
    if (isPostgresRunning()) {
      return null
    }

    val logConsumer = Slf4jLogConsumer(log).withPrefix("postgresql")

    return PostgreSQLContainer<Nothing>("postgres:16").apply {
      withEnv("HOSTNAME_EXTERNAL", "localhost")
      portBindings = bindings
      withDatabaseName("contacts")
      withUsername("contacts")
      withPassword("contacts")
      withReuse(false)
      setWaitStrategy(Wait.forListeningPort())
      start()
      followOutput(logConsumer)
    }
  }

  // This checks whether there is a running instance on the default 5432/tcp port - for example - when
  // running integration tests within a CircleCI pipelines (here it pre-starts a postgres docker container)
  private fun isPostgresRunning(): Boolean = try {
    val serverSocket = ServerSocket(hostPort)
    serverSocket.localPort == 0
  } catch (e: IOException) {
    true
  }
}
