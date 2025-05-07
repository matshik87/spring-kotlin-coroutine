package org.bronco.payments.config

import com.zaxxer.hikari.HikariDataSource
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy
import javax.sql.DataSource

@Configuration
open class DataSourceConfigurationConfiguration {
    @Bean
    open fun dataSource(properties: PaymentsDataSourceProperties): DataSource =
        HikariDataSource(properties.toHikariConfig())

    @Bean
    open fun transactionDateSourceProxy(dataSource: DataSource): TransactionAwareDataSourceProxy =
        TransactionAwareDataSourceProxy(dataSource)

    @Bean
    open fun dslContext(dataSource: DataSource, properties: PaymentsDataSourceProperties): DSLContext =
        DSL.using(dataSource, properties.dialect)
}