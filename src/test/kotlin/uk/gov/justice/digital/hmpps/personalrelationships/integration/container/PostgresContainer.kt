package uk.gov.justice.digital.hmpps.personalrelationships.integration.container

import io.github.cdimascio.dotenv.dotenv
import org.slf4j.LoggerFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.io.IOException
import java.net.ServerSocket

object PostgresContainer {

  private const val DBNAME = "contacts"
  private const val USERNAME = "contacts"
  private const val PASSWORD = "contacts"

  val jdbcUrl: String get() = "jdbc:postgresql://localhost:$hostPort/$DBNAME"
  val dbUsername: String get() = USERNAME
  val dbPassword: String get() = PASSWORD

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

  val instance: GenericContainer<*>? by lazy { startPostgresqlIfNotRunning() }

  // This only starts a TestContainer if postgresql is not already running
  private fun startPostgresqlIfNotRunning(): GenericContainer<*>? {
    if (isPostgresRunning()) {
      return null
    }

    val logConsumer = Slf4jLogConsumer(log).withPrefix("postgresql")

    return GenericContainer(DockerImageName.parse("postgres:17")).apply {
      withEnv("HOSTNAME_EXTERNAL", "localhost")
      portBindings = bindings
      withEnv("POSTGRES_DB", DBNAME)
      withEnv("POSTGRES_USER", USERNAME)
      withEnv("POSTGRES_PASSWORD", PASSWORD)
      withReuse(false)
      waitingFor(Wait.forListeningPort())
      start()
      followOutput(logConsumer)
    }
  }

  // This checks whether there is a running instance on the POSTGRES_TEST_DB_PORT/tcp port - for example. If it's
  // not running, this comes back false, allowing the container to be started, ready for integration tests. If you don't
  // set the variable POSTGRES_TEST_DB_PORT, it defaults to 5432.
  private fun isPostgresRunning(): Boolean = try {
      ServerSocket(hostPort).use { false }
    } catch (_: IOException) {
      true
    }
}
