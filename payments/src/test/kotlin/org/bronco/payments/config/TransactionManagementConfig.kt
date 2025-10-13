package org.bronco.payments.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import javax.sql.DataSource


@TestConfiguration
open class TransactionManagementConfig {
    @Bean
    open fun transactionManager(
        dataSource: DataSource
    ): DataSourceTransactionManager {
        return DataSourceTransactionManager(dataSource)
    }
}