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
import org.bronco.payments.services.processes.model.ProcessName
import org.bronco.payments.services.processes.model.ProcessProgressDetails
import org.bronco.payments.services.processes.model.ProgressType
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
    @MockkBean
    private lateinit var repository: ProcessProgressRepository

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var objectWriter: ObjectWriter

    @Test
    fun retrieveProcessDetails_whenProcessWasFound_then200WithProcessDetailsIsReturned() = runTest {
        val processUuid = UUID.randomUUID()
        val processName = ProcessName.CREATE_CUSTOMER
        val response = ProcessProgressDetails(
            id = UUID.randomUUID(),
            name = processName.name,
            progress = ProgressType.FINISHED.name,
            entityId = UUID.randomUUID(),
            details = "some details"
        )
        coEvery { repository.retrieveProcessDetailsByName(any(), any()) } returns response

        webTestClient.get().uri("/processProgress/process/{processName}/{processId}", processName.toString(), processUuid)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(objectWriter.writeValueAsString(response))

        coVerify(exactly = 1) { repository.retrieveProcessDetailsByName(processUuid, processName) }
    }

    @Test
    fun retrieveProcessDetails_whenProcessWasNotFound_then404IsReturned() = runTest {
        val processUuid = UUID.randomUUID()
        val processName = ProcessName.CREATE_CUSTOMER
        val notFound = HttpStatus.NOT_FOUND
        val response = PaymentsErrorResponse(
            notFound.value(),
            notFound.name,
            ErrorTypes.RESOURCE_NOT_FOUND,
            listOf(ErrorDetail(null, null, "Process progress with id $processUuid was not found"))
        )
        coEvery { repository.retrieveProcessDetailsByName(any(), any()) } returns null

        webTestClient.get().uri("/processProgress/process/{processName}/{processId}", processName.toString(), processUuid)
            .exchange()
            .expectStatus().isNotFound
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(objectWriter.writeValueAsString(response))

        coVerify(exactly = 1) { repository.retrieveProcessDetailsByName(processUuid, processName) }
    }

    @Test
    fun retrieveProcessDetails_whenProcessNameIsInvalid_then400WithErrorDetailsIsReturned() = runTest {
        val processUuid = UUID.randomUUID()
        val processName = "test-process-name"
        val badRequest = HttpStatus.BAD_REQUEST
        val response = PaymentsErrorResponse(
            code = badRequest.value(), status = badRequest.name, type = ErrorTypes.VALIDATION,
            details = listOf(ErrorDetail(value = null, field = null, message = "An improper process name was provided"))
        )

        webTestClient.get().uri("/processProgress/process/{processName}/{processId}", processName, processUuid)
            .exchange()
            .expectStatus().isBadRequest
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(objectWriter.writeValueAsString(response))

        coVerify(exactly = 0) { repository.retrieveProcessDetailsByName(any(), any()) }
    }

    @Test
    fun retrieveProcessDetails_whenProcessIdIsInvalid_then400WithErrorDetailsIsReturned() = runTest {
        val processUuid = "test-value"
        val processName = ProcessName.CREATE_CUSTOMER.name
        val badRequest = HttpStatus.BAD_REQUEST
        val response = PaymentsErrorResponse(
            code = badRequest.value(), status = badRequest.name, type = ErrorTypes.VALIDATION,
            details = listOf(ErrorDetail(value = null, field = null, message = "Invalid UUID identifier was provided"))
        )

        webTestClient.get().uri("/processProgress/process/{processName}/{processId}", processName, processUuid)
            .exchange()
            .expectStatus().isBadRequest
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(objectWriter.writeValueAsString(response))

        coVerify(exactly = 0) { repository.retrieveProcessDetailsByName(any(), any()) }
    }
}