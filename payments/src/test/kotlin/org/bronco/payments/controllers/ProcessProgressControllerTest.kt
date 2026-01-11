package org.bronco.payments.controllers

import com.fasterxml.jackson.databind.ObjectWriter
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.bronco.payments.config.ObjectMapperConfig
import org.bronco.payments.controllers.api.ErrorDetail
import org.bronco.payments.controllers.api.ErrorTypes
import org.bronco.payments.controllers.api.PaymentsErrorResponse
import org.bronco.payments.repositories.progress.impl.ProcessProgressRepository
import org.bronco.payments.services.processes.model.ProcessProgressDetails
import org.bronco.payments.services.processes.model.ProcessType
import org.bronco.payments.services.processes.model.ProgressType
import org.bronco.payments.utils.ProcessProgressGenerators.generateProcessProgressDetails
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import java.util.*

@ExtendWith(SpringExtension::class)
@WebFluxTest(
    controllers = [ProcessProgressController::class],
    excludeAutoConfiguration = [KafkaAutoConfiguration::class, DataSourceAutoConfiguration::class],
    properties = [
        "containers.kafka.enabled=false", "containers.db.enabled=false"
    ]
)
@Import(ObjectMapperConfig::class)
class ProcessProgressControllerTest {
    companion object {
        private const val PROCESS_WITH_PROCESS_TYPE_PATH = "/processProgress/process/{processId}/{processType}"
        private const val PROCESS_PATH = "/processProgress/process/{processId}"
        private const val PARENT_PROCESS_PATH = "/processProgress/parentProcessId/{parentProcessId}"
        private const val PARENT_PROCESS_WITH_TYPE_PATH =
            "/processProgress/parentProcessId/{parentProcessId}/{processType}"
    }

    @MockkBean
    private lateinit var repository: ProcessProgressRepository

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var objectWriter: ObjectWriter

    @Test
    fun retrieveProcessDetails_whenProcessWasFound_then200WithProcessDetailsIsReturned() = runTest {
        val processUuid = UUID.randomUUID()
        val processType = ProcessType.CREATE_CUSTOMER
        val response = ProcessProgressDetails(
            id = UUID.randomUUID(),
            type = processType.name,
            progress = ProgressType.FINISHED.name,
            entityId = UUID.randomUUID(),
            details = "some details",
            parentId = null
        )
        coEvery { repository.retrieveProcessDetailsByName(any(), any()) } returns response

        webTestClient.get().uri(PROCESS_WITH_PROCESS_TYPE_PATH, processUuid, processType.toString())
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(objectWriter.writeValueAsString(response))

        coVerify(exactly = 1) { repository.retrieveProcessDetailsByName(processUuid, processType) }
    }

    @Test
    fun retrieveProcessDetails_whenProcessWasNotFound_then404IsReturned() = runTest {
        val processUuid = UUID.randomUUID()
        val processType = ProcessType.CREATE_CUSTOMER
        val notFound = HttpStatus.NOT_FOUND
        val response = PaymentsErrorResponse(
            notFound.value(),
            notFound.name,
            ErrorTypes.RESOURCE_NOT_FOUND,
            listOf(ErrorDetail(null, null, "Process progress with id $processUuid was not found"))
        )
        coEvery { repository.retrieveProcessDetailsByName(any(), any()) } returns null

        webTestClient.get().uri(PROCESS_WITH_PROCESS_TYPE_PATH, processUuid, processType.toString())
            .exchange()
            .expectStatus().isNotFound
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(objectWriter.writeValueAsString(response))

        coVerify(exactly = 1) { repository.retrieveProcessDetailsByName(processUuid, processType) }
    }

    @Test
    fun retrieveProcessDetails_whenProcessNameIsInvalid_then400WithErrorDetailsIsReturned() = runTest {
        val processUuid = UUID.randomUUID()
        val processName = "test-process-name"
        val response = invalidProcessNameErrorResponse()

        webTestClient.get().uri(PROCESS_WITH_PROCESS_TYPE_PATH, processUuid, processName)
            .exchange()
            .expectStatus().isBadRequest
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(objectWriter.writeValueAsString(response))

        coVerify(exactly = 0) { repository.retrieveProcessDetailsByName(any(), any()) }
    }

    @Test
    fun retrieveProcessDetails_whenProcessIdIsInvalid_then400WithErrorDetailsIsReturned() = runTest {
        val processUuid = "test-value"
        val processType = ProcessType.CREATE_CUSTOMER.name
        val response = invalidProcessIdErrorResponse()

        webTestClient.get().uri(PROCESS_WITH_PROCESS_TYPE_PATH, processUuid, processType)
            .exchange()
            .expectStatus().isBadRequest
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(objectWriter.writeValueAsString(response))

        coVerify(exactly = 0) { repository.retrieveProcessDetailsByName(any(), any()) }
    }

    @Test
    fun retrieveDetailsForParentProcessId_whenProcessIdIsInvalid_then400WithErrorDetailsIsReturned() = runTest {
        val processUuid = "test-value"
        val response = invalidProcessIdErrorResponse()

        webTestClient.get().uri(PROCESS_PATH, processUuid)
            .exchange()
            .expectStatus().isBadRequest
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(objectWriter.writeValueAsString(response))

        coVerify(exactly = 0) { repository.retrieveProcessDetailsByName(any(), any()) }
    }

    @Test
    fun retrieveDetailsForParentProcessId_whenProcessWasNotFound_then404WithErrorDetailsIsReturned() = runTest {
        val processUuid = UUID.randomUUID()
        val notFound = HttpStatus.NOT_FOUND
        val response = PaymentsErrorResponse(
            notFound.value(),
            notFound.name,
            ErrorTypes.RESOURCE_NOT_FOUND,
            listOf(ErrorDetail(null, null, "Process progress with id $processUuid was not found"))
        )
        coEvery {
            repository.retrieveProcessDetails(any())
        } returns emptyList()

        webTestClient.get().uri(PROCESS_PATH, processUuid)
            .exchange()
            .expectStatus().isNotFound
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(objectWriter.writeValueAsString(response))

        coVerify { repository.retrieveProcessDetails(processUuid) }
    }

    @Test
    fun retrieveDetailsForParentProcessId_whenRequestWasValid_then200WithPayloadIsReturned() = runTest {
        val processUuid = UUID.randomUUID()
        val expectation = generateProcessProgressDetails(
            processUuid,
            UUID.randomUUID(),
            ProcessType.CREATE_CUSTOMER_ACCOUNT,
            ProgressType.ALREADY_PROCESSED,
            UUID.randomUUID(),
            null
        )
        coEvery {
            repository.retrieveProcessDetails(any())
        } returns listOf(expectation)

        webTestClient.get().uri(PROCESS_PATH, processUuid)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(objectWriter.writeValueAsString(expectation))

        coVerify { repository.retrieveProcessDetails(processUuid) }
    }

    @Test
    fun retrieveDetailsForParentProcessId_whenParentProcessIdIsInvalid_then400WithErrorDetailsIsReturned() = runTest {
        val processUuid = "test-value"
        val badRequest = HttpStatus.BAD_REQUEST
        val response = invalidProcessIdErrorResponse()

        webTestClient.get().uri(PARENT_PROCESS_PATH, processUuid)
            .exchange()
            .expectStatus().isBadRequest
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(objectWriter.writeValueAsString(response))

        coVerify(exactly = 0) { repository.retrieveProcessDetailsByName(any(), any()) }
    }

    @Test
    fun retrieveDetailsForParentProcessId_whenParentProcessIdWasNotFound_then200WithEmptyListIsReturned() = runTest {
        val parentProcessId = UUID.randomUUID()
        coEvery {
            repository.findProcessDetailsForProcessTypesByIds(
                processTypes = any(),
                processId = isNull(),
                parentProcessId = any()
            )
        } returns emptyList()

        webTestClient.get().uri(PARENT_PROCESS_PATH, parentProcessId)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json("[]")

        coVerify {
            repository.findProcessDetailsForProcessTypesByIds(
                processTypes = emptyList(),
                processId = null,
                parentProcessId = parentProcessId
            )
        }
    }

    @Test
    fun retrieveDetailsForParentProcessId_whenParentProcessIdWasFound_then200WithResultsIsReturned() = runTest {
        val parentProcessId = UUID.randomUUID()
        val expectation = listOf(
            generateProcessProgressDetails(
                UUID.randomUUID(),
                UUID.randomUUID(),
                ProcessType.CREATE_CUSTOMER_ACCOUNT,
                ProgressType.ALREADY_PROCESSED,
                UUID.randomUUID(),
                null
            ),
            generateProcessProgressDetails(
                UUID.randomUUID(),
                UUID.randomUUID(),
                ProcessType.CREATE_CUSTOMER,
                ProgressType.FINISHED,
                UUID.randomUUID(),
                null
            )
        )
        coEvery {
            repository.findProcessDetailsForProcessTypesByIds(
                processTypes = any(),
                processId = isNull(),
                parentProcessId = any()
            )
        } returns expectation

        webTestClient.get().uri(PARENT_PROCESS_PATH, parentProcessId)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(objectWriter.writeValueAsString(expectation))

        coVerify {
            repository.findProcessDetailsForProcessTypesByIds(
                processTypes = any(),
                processId = null,
                parentProcessId = parentProcessId
            )
        }
    }

    @Test
    fun retrieveDetailsForParentProcessIdAndProcessType_whenProcessTypeIsInvalid_then400IsReturned() = runTest {
        val parentProcessId = UUID.randomUUID()
        val processType = "test-process"
        val response = invalidProcessNameErrorResponse()

        webTestClient.get().uri(PARENT_PROCESS_WITH_TYPE_PATH, parentProcessId, processType)
            .exchange()
            .expectStatus().isBadRequest
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(objectWriter.writeValueAsString(response))

        coVerify(exactly = 0) {
            repository.findProcessDetailsForProcessTypesByIds(
                processTypes = any(),
                processId = null,
                parentProcessId = parentProcessId
            )
        }
    }

    @Test
    fun retrieveDetailsForParentProcessIdAndProcessType_whenParentProcessIdIsInvalid_then400IsReturned() = runTest {
        val parentProcessId = "test-id"
        val processType = ProcessType.CREATE_CUSTOMER
        val response = invalidProcessIdErrorResponse()

        webTestClient.get().uri(PARENT_PROCESS_WITH_TYPE_PATH, parentProcessId, processType)
            .exchange()
            .expectStatus().isBadRequest
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(objectWriter.writeValueAsString(response))

        coVerify(exactly = 0) {
            repository.findProcessDetailsForProcessTypesByIds(
                processTypes = any(),
                processId = null,
                parentProcessId = any()
            )
        }
    }

    @Test
    fun retrieveDetailsForParentProcessIdAndProcessType_whenParentProcessIdWasNotFound_then200WithEmptyListIsReturned() =
        runTest {
            val parentProcessId = UUID.randomUUID()
            val processType = ProcessType.CREATE_CUSTOMER
            coEvery {
                repository.findProcessDetailsForProcessTypesByIds(
                    listOf(processType),
                    parentProcessId = parentProcessId
                )
            } returns emptyList()

            webTestClient.get().uri(PARENT_PROCESS_WITH_TYPE_PATH, parentProcessId, processType)
                .exchange()
                .expectStatus().isOk
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody().json("[]")

            coVerify {
                repository.findProcessDetailsForProcessTypesByIds(
                    processTypes = listOf(processType),
                    processId = null,
                    parentProcessId = parentProcessId
                )
            }
        }

    @Test
    fun retrieveDetailsForParentProcessIdAndProcessType_whenParentProcessIdWasFound_then200WithResultIsReturned() =
        runTest {
            val parentProcessId = UUID.randomUUID()
            val processType = ProcessType.CREATE_CUSTOMER
            val expectation = listOf(
                generateProcessProgressDetails(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    processType,
                    ProgressType.ALREADY_PROCESSED,
                    UUID.randomUUID(),
                    null
                )
            )
            coEvery {
                repository.findProcessDetailsForProcessTypesByIds(
                    listOf(processType),
                    parentProcessId = parentProcessId
                )
            } returns expectation

            webTestClient.get().uri(PARENT_PROCESS_WITH_TYPE_PATH, parentProcessId, processType)
                .exchange()
                .expectStatus().isOk
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody().json(objectWriter.writeValueAsString(expectation))

            coVerify {
                repository.findProcessDetailsForProcessTypesByIds(
                    processTypes = listOf(processType),
                    processId = null,
                    parentProcessId = parentProcessId
                )
            }
        }

    private fun invalidProcessNameErrorResponse() = PaymentsErrorResponse(
        code = HttpStatus.BAD_REQUEST.value(), status = HttpStatus.BAD_REQUEST.name, type = ErrorTypes.VALIDATION,
        details = listOf(ErrorDetail(value = null, field = null, message = "An improper process name was provided"))
    )

    private fun invalidProcessIdErrorResponse() = PaymentsErrorResponse(
        code = HttpStatus.BAD_REQUEST.value(), status = HttpStatus.BAD_REQUEST.name, type = ErrorTypes.VALIDATION,
        details = listOf(ErrorDetail(value = null, field = null, message = "Invalid UUID identifier was provided"))
    )
}
