package uk.gov.justice.digital.hmpps.personalrelationships.config

import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.HikariPoolMXBean
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import java.util.*

@Component
class HikariPoolLogger(private val dataSource: HikariDataSource) {

  @PostConstruct
  fun startLogging() {
    val poolProxy: HikariPoolMXBean = dataSource.hikariPoolMXBean
    val timer = Timer(true)
    timer.scheduleAtFixedRate(
      object : TimerTask() {
        override fun run() {
          println(
            "HikariCP Pool Stats - Total: ${poolProxy.totalConnections}, " +
              "Active: ${poolProxy.activeConnections}, " +
              "Idle: ${poolProxy.idleConnections}, " +
              "Waiting: ${poolProxy.threadsAwaitingConnection}",
          )
        }
      },
      0,
      300000,
    ) // every 5 minutes
  }
}
