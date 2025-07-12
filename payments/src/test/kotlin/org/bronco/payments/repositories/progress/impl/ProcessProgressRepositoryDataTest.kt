package org.bronco.payments.repositories.progress.impl

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions
import org.bronco.payments.repositories.progress.ProgressKey
import org.bronco.payments.repositories.progress.ProgressRepository
import org.bronco.payments.schema.jooq.model.tables.references.PROCESS_PROGRESS
import org.bronco.payments.services.processes.model.ProcessName
import org.bronco.payments.services.processes.model.ProgressType
import org.jooq.DSLContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import java.util.*

@JooqTest(properties = ["payments.kafka.consumers.create-topics=false"])
@ComponentScan(basePackages = ["org.bronco.payments.config"])
@EnableAutoConfiguration(
    exclude = [KafkaAutoConfiguration::class]
)
@Import(ProcessProgressRepository::class)
open class ProcessProgressRepositoryDataTest {
    @Autowired
    private lateinit var repository: ProgressRepository

    @Autowired
    private lateinit var dslContext: DSLContext


    @AfterEach
    fun cleanUp(): Unit {
        dslContext.deleteFrom(PROCESS_PROGRESS)
    }

    @Test
    fun `initiateProgress - new progress should be persisted and return progress details`() = runTest {
        val progressKeyId = UUID.randomUUID()
        val progressKey = ProgressKey(progressKeyId, ProcessName.CREATE_CUSTOMER)
        val entityId = UUID.randomUUID()
        val progressDetails = "just starting"

        val result = repository.initiateProgress(progressKey, entityId, progressDetails)
        Assertions.assertThat(result).isNotNull()
            .returns(progressKey.id) { it.id }
            .returns(progressKey.name.name) { it.name }
            .returns(ProgressType.INITIALIZED.name) { it.progress }
            .returns(entityId) { it.entityId }
            .returns(progressDetails) { it.details }
        val exists = dslContext.fetchExists(
            PROCESS_PROGRESS, PROCESS_PROGRESS.PROCESS_ID.eq(progressKey.id),
            PROCESS_PROGRESS.PROCESS_NAME.eq(progressKey.name.name),
            PROCESS_PROGRESS.ENTITY_ID.eq(entityId),
            PROCESS_PROGRESS.DETAILS.eq(progressDetails)
        )
        Assertions.assertThat(exists).isTrue
    }

    @Test
    fun `updateProgress - when process was updated then its status is reflected on retrieve`() = runTest {
        val progressKeyId = UUID.randomUUID()
        val progressName = ProcessName.CREATE_CUSTOMER
        val progressKey = ProgressKey(progressKeyId, progressName)
        val entityId = UUID.randomUUID()
        val progressDetails = "just starting"
        repository.initiateProgress(progressKey, null, progressDetails)
        val progressType = ProgressType.ALREADY_PROCESSED
        val updatedProgressDetails = "already done"

        repository.updateProgress(progressKey, progressType, entityId, updatedProgressDetails)

        val retrievedProgress = repository.retrieveProcessDetails(progressKeyId, progressName)
        Assertions.assertThat(retrievedProgress).isNotNull()
            .returns(progressKey.id) { it?.id }
            .returns(progressKey.name.name) { it?.name }
            .returns(progressType.name) { it?.progress }
            .returns(entityId) { it?.entityId }
            .returns(updatedProgressDetails) { it?.details }
    }

    @Test
    fun `updateProgress - non existing process progress cannot be updated, returned progress details is null`() =
        runTest {
            val progressKeyId = UUID.randomUUID()
            val progressName = ProcessName.UPDATE_CUSTOMER
            val entityId = UUID.randomUUID()
            val progressType = ProgressType.ALREADY_PROCESSED
            val updatedProgressDetails = "already done"
            val newProgressKey = ProgressKey(progressKeyId, progressName)


            repository.updateProgress(newProgressKey, progressType, entityId, updatedProgressDetails)

            val retrievedProgress = repository.retrieveProcessDetails(progressKeyId, progressName)
            Assertions.assertThat(retrievedProgress).isNull()
        }
}