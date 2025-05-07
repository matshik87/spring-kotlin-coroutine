package org.bronco.payments.config

import com.zaxxer.hikari.HikariConfig
import org.jooq.SQLDialect
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Component

@EnableConfigurationProperties
@Component
@ConfigurationProperties(prefix = "payments.data-source")
open class PaymentsDataSourceProperties {
    var schema: String? = null
    var autoCommit: Boolean = false
    var jdbcUrl: String? = null
    var username: String? = null
    var password: String? = null
    var connectionTimeout: Long = 30000
    var idleTimeout: Long = 600000
    var keepAliveTime: Long = 120000
    var minPoolSize: Int? = null
    var maxPoolSize: Int = 10
    var poolName: String? = null
    var dialect: SQLDialect? = null

    fun toHikariConfig(): HikariConfig {
        return HikariConfig().apply {
            schema = this@PaymentsDataSourceProperties.schema
            autoCommit = this@PaymentsDataSourceProperties.autoCommit
            jdbcUrl = this@PaymentsDataSourceProperties.jdbcUrl
            username = this@PaymentsDataSourceProperties.username
            password = this@PaymentsDataSourceProperties.password
            connectionTimeout = this@PaymentsDataSourceProperties.connectionTimeout
            idleTimeout = this@PaymentsDataSourceProperties.idleTimeout
            keepAliveTime = this@PaymentsDataSourceProperties.keepAliveTime
            minPoolSize = this@PaymentsDataSourceProperties.minPoolSize
            maxPoolSize = this@PaymentsDataSourceProperties.maxPoolSize
            poolName = this@PaymentsDataSourceProperties.poolName
        }
    }
}