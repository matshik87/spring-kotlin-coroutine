package org.bronco.payments.repositories.progress.impl

import org.assertj.core.api.Assertions
import org.bronco.payments.repositories.progress.ProgressRepository
import org.jooq.DSLContext
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration

@JooqTest(properties = ["payments.kafka.consumers.create-topics=false"])
@ComponentScan(basePackages = ["org.bronco.payments.config"])
@EnableAutoConfiguration(
    exclude = [KafkaAutoConfiguration::class]
)
@Import(ProcessesProgressRepository::class)
@ContextConfiguration(initializers = [PostgreSqlInitializer::class])
open class ProcessesProgressRepositoryDataTest {
    @Autowired
    private lateinit var repository: ProgressRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @Test
    fun name() {
        Assertions.assertThat(repository).isNotNull()
        Assertions.assertThat(dslContext).isNotNull()
    }
}