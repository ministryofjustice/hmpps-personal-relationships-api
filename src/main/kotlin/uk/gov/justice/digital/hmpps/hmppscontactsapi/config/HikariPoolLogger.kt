package uk.gov.justice.digital.hmpps.hmppscontactsapi.config

import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.HikariPoolMXBean
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.*

@Component
@Profile("dev", "preprod") // Only active in dev or preprod
class HikariPoolLogger(private val dataSource: HikariDataSource) {

    @PostConstruct
    fun startLogging() {
        val poolProxy: HikariPoolMXBean = dataSource.hikariPoolMXBean
        val timer = Timer(true)
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                println("HikariCP Pool Stats - Total: ${poolProxy.totalConnections}, " +
                        "Active: ${poolProxy.activeConnections}, " +
                        "Idle: ${poolProxy.idleConnections}, " +
                        "Waiting: ${poolProxy.threadsAwaitingConnection}")
            }
        }, 0, 5000) // every 5 seconds
    }
}
