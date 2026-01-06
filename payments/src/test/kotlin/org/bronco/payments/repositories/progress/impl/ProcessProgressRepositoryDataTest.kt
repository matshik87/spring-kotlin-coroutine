package org.bronco.payments.repositories.progress.impl

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions
import org.bronco.payments.repositories.progress.ProcessProgressProperties
import org.bronco.payments.repositories.progress.ProgressKey
import org.bronco.payments.repositories.progress.ProgressRepository
import org.bronco.payments.schema.jooq.model.tables.references.PROCESS_PROGRESS
import org.bronco.payments.services.processes.model.ProcessType
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
    fun `initiateProgress - new progress should be persisted using properties and return progress details`() = runTest {
        val progressKeyId = UUID.randomUUID()
        val progressKey = ProgressKey(progressKeyId, ProcessType.CREATE_CUSTOMER)
        val entityId = UUID.randomUUID()
        val parentProcessId = UUID.randomUUID()
        val progressDetails = "just starting"
        val processProperties = ProcessProgressProperties(
            processType = ProcessType.CREATE_CUSTOMER,
            progressType = ProgressType.INITIALIZED,
            parentProcessId = parentProcessId,
            id = progressKeyId,
            entityId = entityId,
            progressDetails = progressDetails
        )

        val result = repository.initiateProgress(processProperties)

        Assertions.assertThat(result).isNotNull()
            .returns(progressKey.id) { it.id }
            .returns(progressKey.name.name) { it.type }
            .returns(ProgressType.INITIALIZED.name) { it.progress }
            .returns(entityId) { it.entityId }
            .returns(progressDetails) { it.details }
        val exists = dslContext.fetchExists(
            PROCESS_PROGRESS, PROCESS_PROGRESS.PROCESS_ID.eq(progressKey.id),
            PROCESS_PROGRESS.PROCESS_TYPE.eq(progressKey.name.name),
            PROCESS_PROGRESS.ENTITY_ID.eq(entityId),
            PROCESS_PROGRESS.DETAILS.eq(progressDetails),
            PROCESS_PROGRESS.PROCESS_PARENT_ID.eq(parentProcessId)
        )
        Assertions.assertThat(exists).isTrue
    }

    @Test
    fun `updateProgress - when process was updated using properties then its status is reflected on retrieve`() =
        runTest {
            val progressKeyId = UUID.randomUUID()
            val processType = ProcessType.CREATE_CUSTOMER
            val progressKey = ProgressKey(progressKeyId, processType)
            val entityId = UUID.randomUUID()
            val parentProcessId = UUID.randomUUID()
            val initialProperties = ProcessProgressProperties.ofInitial(
                processId = progressKeyId,
                processType = processType,
                parentProcessId = parentProcessId,
            )
            repository.initiateProgress(initialProperties)
            val progressType = ProgressType.ALREADY_PROCESSED
            val updatedProgressDetails = "already done"
            val processProperties = ProcessProgressProperties(
                processType = processType,
                progressType = progressType,
                parentProcessId = parentProcessId,
                id = progressKeyId,
                entityId = entityId,
                progressDetails = updatedProgressDetails
            )

            repository.updateProgress(processProperties)

            val retrievedProgress = repository.retrieveProcessDetailsByName(progressKeyId, processType)
            Assertions.assertThat(retrievedProgress).isNotNull()
                .returns(progressKey.id) { it?.id }
                .returns(progressKey.name.name) { it?.type }
                .returns(progressType.name) { it?.progress }
                .returns(entityId) { it?.entityId }
                .returns(updatedProgressDetails) { it?.details }
                .returns(parentProcessId) { it?.parentId }
        }

    @Test
    fun `updateProgress - non existing process progress cannot be updated, returned progress details is null`() =
        runTest {
            val progressKeyId = UUID.randomUUID()
            val processName = ProcessType.UPDATE_CUSTOMER
            val entityId = UUID.randomUUID()
            val progressType = ProgressType.ALREADY_PROCESSED
            val updatedProgressDetails = "already done"
            val processProperties = ProcessProgressProperties.of(
                processId = progressKeyId,
                processType = processName,
                progressType = progressType,
                parentProcessId = UUID.randomUUID(),
                entityId = entityId,
                processDetails = updatedProgressDetails
            )

            repository.updateProgress(processProperties)

            val retrievedProgress = repository.retrieveProcessDetailsByName(progressKeyId, processName)
            Assertions.assertThat(retrievedProgress).isNull()
        }

    @Test
    fun `findProcessDetailsForProcessNames - no matching process was found then empty response is returned`() =
        runTest {
            val progressKeyId = UUID.randomUUID()

            val result = repository.findProcessDetailsForProcessTypesByIds(
                listOf(ProcessType.UPDATE_CUSTOMER),
                processId = progressKeyId
            )
            Assertions.assertThat(result).isEmpty()
        }

    @Test
    fun `findProcessDetailsForProcessNames - matching process was found then entity is returned`() = runTest {
        val progressKeyId = UUID.randomUUID()
        val processType = ProcessType.CREATE_CUSTOMER
        val processProperties = ProcessProgressProperties.ofInitial(
            processId = progressKeyId,
            processType = processType,
            parentProcessId = UUID.randomUUID()
        )
        val expectedResponse = repository.initiateProgress(processProperties)

        val result = repository.findProcessDetailsForProcessTypesByIds(listOf(processType), processId = progressKeyId)
        Assertions.assertThat(result).singleElement()
            .usingRecursiveComparison().isEqualTo(expectedResponse)
    }

    @Test
    fun `findProcessDetailsForProcessNamesByIds - no matching process was found then empty response is returned`() =
        runTest {
            val result = repository.findProcessDetailsForProcessTypesByIds(
                listOf(ProcessType.UPDATE_CUSTOMER),
                parentProcessId = UUID.randomUUID()
            )
            Assertions.assertThat(result).isEmpty()
        }

    @Test
    fun `findProcessDetailsForProcessNamesByIds - matching process was found then entity is returned`() = runTest {
        val processId = UUID.randomUUID()
        val parentProcessId = UUID.randomUUID()
        val progressDetails = "just starting"
        val processType = ProcessType.CREATE_CUSTOMER
        val properties = ProcessProgressProperties.ofInitial(processId, processType, parentProcessId)
            .copy(progressDetails = progressDetails)
        val expectedResponse = repository.initiateProgress(properties)

        val result = repository.findProcessDetailsForProcessTypesByIds(
            listOf(processType),
            parentProcessId = parentProcessId
        )
        Assertions.assertThat(result).singleElement()
            .usingRecursiveComparison().isEqualTo(expectedResponse)
    }

    @Test
    fun `findProcessDetailsForProcessNamesByIds - for no id passed, an exception is thrown`() = runTest {
        Assertions.assertThatThrownBy {
            runBlocking {
                repository.findProcessDetailsForProcessTypesByIds(
                    listOf(ProcessType.CREATE_CUSTOMER)
                )
            }
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("any of main or parent process id is required")
    }
}