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
import org.bronco.payments.model.Currencies
import org.bronco.payments.repositories.account.model.toApiResponse
import org.bronco.payments.services.account.CustomerAccountService
import org.bronco.payments.services.account.model.BatchAccountCreation
import org.bronco.payments.services.account.model.BatchAccountCreationResponse
import org.bronco.payments.services.account.model.CreateCustomerAccountCommand
import org.bronco.payments.services.processes.model.ProcessType
import org.bronco.payments.services.processes.model.ProgressType
import org.bronco.payments.utils.AccountDataGenerators.generateAccount
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
    controllers = [AccountController::class],
    excludeAutoConfiguration = [KafkaAutoConfiguration::class, DataSourceAutoConfiguration::class],
    properties = [
        "containers.kafka.enabled=false", "containers.db.enabled=false"
    ]
)
@Import(ObjectMapperConfig::class)
class AccountControllerTest {
    companion object {
        private const val ACCOUNT_PATH = "/accounts"
        private const val ACCOUNT_BY_ID_PATH = "$ACCOUNT_PATH/{id}"
        private const val ACCOUNT_BY_CUSTOMER_ID_PATH = "$ACCOUNT_PATH/customer/{id}"
        private const val ACCOUNT_BY_PROCESS_ID_PATH = "$ACCOUNT_PATH/process/{id}"
        private const val ACCOUNT_BY_PARENT_PROCESS_ID_PATH = "$ACCOUNT_PATH/process/parent/{id}"
        private const val SCHEDULE_ACCOUNT_CREATION_PATH = "$ACCOUNT_PATH/create"
        private const val SCHEDULE_BATCH_ACCOUNT_CREATION_PATH = "$SCHEDULE_ACCOUNT_CREATION_PATH/batch"
    }

    @MockkBean(relaxed = true)
    private lateinit var accountService: CustomerAccountService

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var objectWriter: ObjectWriter

    @Test
    fun retrieveAccountsByCustomerId_whenValidRequestAndCustomerExists_then200WithAllAccounts() = runTest {
        val customerId = UUID.randomUUID()
        val customerAccounts =
            listOf(generateAccount(customerId = customerId), generateAccount(customerId = customerId))
        coEvery { accountService.getAccountsByCustomerId(customerId) } returns customerAccounts

        webTestClient.get().uri(ACCOUNT_BY_CUSTOMER_ID_PATH, customerId.toString())
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(objectWriter.writeValueAsString(customerAccounts.map { it.toApiResponse() }))
    }

    @Test
    fun retrieveAccountsByCustomerId_whenValidRequestButNoAccountsPerCustomer_then404() = runTest {
        val customerId = UUID.randomUUID()
        coEvery { accountService.getAccountsByCustomerId(customerId) } returns emptyList()
        val notFound = HttpStatus.NOT_FOUND
        val expectedResponse = PaymentsErrorResponse(
            code = notFound.value(), status = notFound.name, type = ErrorTypes.RESOURCE_NOT_FOUND,
            details = listOf(
                ErrorDetail(
                    value = null,
                    field = null,
                    message = "Account for customer with id $customerId was not found"
                )
            )
        )

        webTestClient.get().uri(ACCOUNT_BY_CUSTOMER_ID_PATH, customerId.toString())
            .exchange()
            .expectStatus().isNotFound
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(objectWriter.writeValueAsString(expectedResponse))
    }

    @Test
    fun retrieveAccountsByCustomerId_whenInValidRequest_then400() = runTest {
        val customerId = "invalid"
        val badRequest = HttpStatus.BAD_REQUEST
        val expectedResponse = PaymentsErrorResponse(
            code = badRequest.value(), status = badRequest.name, type = ErrorTypes.VALIDATION,
            details = listOf(ErrorDetail(value = null, field = null, message = "Invalid UUID identifier was provided"))
        )

        webTestClient.get().uri(ACCOUNT_BY_CUSTOMER_ID_PATH, customerId)
            .exchange()
            .expectStatus().isBadRequest
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(objectWriter.writeValueAsString(expectedResponse))
        coVerify(exactly = 0) { accountService.getAccountsByCustomerId(any(UUID::class)) }
    }

    @Test
    fun retrieveAccountById_whenValidRequestAndCustomerExists_then200WithAllAccounts() = runTest {
        val customerId = UUID.randomUUID()
        val account = generateAccount(customerId = customerId)
        coEvery { accountService.getById(customerId) } returns account

        webTestClient.get().uri(ACCOUNT_BY_ID_PATH, customerId.toString())
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(objectWriter.writeValueAsString(account))
    }

    @Test
    fun retrieveAccountById_whenValidRequestButNoAccountsPerCustomer_then404() = runTest {
        val accountId = UUID.randomUUID()
        coEvery { accountService.getById(accountId) } returns null
        val notFound = HttpStatus.NOT_FOUND
        val expectedResponse = PaymentsErrorResponse(
            code = notFound.value(), status = notFound.name, type = ErrorTypes.RESOURCE_NOT_FOUND,
            details = listOf(
                ErrorDetail(
                    value = null,
                    field = null,
                    message = "Customer account with id $accountId was not found"
                )
            )
        )

        webTestClient.get().uri(ACCOUNT_BY_ID_PATH, accountId.toString())
            .exchange()
            .expectStatus().isNotFound
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(objectWriter.writeValueAsString(expectedResponse))
    }

    @Test
    fun retrieveAccountById_whenInValidRequest_then400() = runTest {
        val customerId = "invalid"
        val badRequest = HttpStatus.BAD_REQUEST
        val expectedResponse = PaymentsErrorResponse(
            code = badRequest.value(), status = badRequest.name, type = ErrorTypes.VALIDATION,
            details = listOf(ErrorDetail(value = null, field = null, message = "Invalid UUID identifier was provided"))
        )

        webTestClient.get().uri(ACCOUNT_BY_ID_PATH, customerId)
            .exchange()
            .expectStatus().isBadRequest
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(objectWriter.writeValueAsString(expectedResponse))
        coVerify(exactly = 0) { accountService.getAccountsByCustomerId(any(UUID::class)) }
    }

    @Test
    fun retrieveAccountsByProcessId_whenProcessWasFound_then200WithResponse() = runTest {
        val processId = UUID.randomUUID()
        val accounts = listOf(generateAccount(customerId = UUID.randomUUID()))
        coEvery { accountService.getAccountsByProcessId(processId) } returns accounts
        webTestClient.get().uri(ACCOUNT_BY_PROCESS_ID_PATH, processId)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(objectWriter.writeValueAsString(accounts))
        coVerify { accountService.getAccountsByProcessId(processId) }
    }

    @Test
    fun retrieveAccountsByProcessId_whenProcessWasNotFound_then200WithEmptyResponse() = runTest {
        val processId = UUID.randomUUID()
        webTestClient.get().uri(ACCOUNT_BY_PROCESS_ID_PATH, processId)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(objectWriter.writeValueAsString(emptyList<Int>()))
        coVerify { accountService.getAccountsByProcessId(processId) }
    }

    @Test
    fun retrieveAccountsByProcessId_whenInValidRequest_then400() = runTest {
        val processId = "test"
        val badRequest = HttpStatus.BAD_REQUEST
        val expectedResponse = PaymentsErrorResponse(
            code = badRequest.value(), status = badRequest.name, type = ErrorTypes.VALIDATION,
            details = listOf(ErrorDetail(value = null, field = null, message = "Invalid UUID identifier was provided"))
        )

        webTestClient.get().uri(ACCOUNT_BY_PROCESS_ID_PATH, processId)
            .exchange()
            .expectStatus().isBadRequest
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(objectWriter.writeValueAsString(expectedResponse))
        coVerify(exactly = 0) { accountService.getAccountsByProcessId(any(UUID::class)) }
    }

    @Test
    fun retrieveAccountsByParentProcessId_whenValidRequestButNoMatchingProcess_then200WithEmptyResponse() = runTest {
        val parentProcess = UUID.randomUUID()

        webTestClient.get().uri(ACCOUNT_BY_PARENT_PROCESS_ID_PATH, parentProcess)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json("[]")
    }

    @Test
    fun retrieveAccountsByParentProcessId_whenInvalidRequestDueToProcessId_then400WithErrorResponse() = runTest {
        val parentProcessId = "invalid"
        val badRequest = HttpStatus.BAD_REQUEST
        val expectedResponse = PaymentsErrorResponse(
            code = badRequest.value(), status = badRequest.name, type = ErrorTypes.VALIDATION,
            details = listOf(ErrorDetail(value = null, field = null, message = "Invalid UUID identifier was provided"))
        )

        webTestClient.get().uri(ACCOUNT_BY_PARENT_PROCESS_ID_PATH, parentProcessId)
            .exchange()
            .expectStatus().isBadRequest
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(objectWriter.writeValueAsString(expectedResponse))
        coVerify(exactly = 0) { accountService.getAccountsByParentProcessId(any(UUID::class)) }
    }

    @Test
    fun retrieveAccountsByParentProcessId_whenValidRequest_then200WithResponse() = runTest {
        val parentProcess = UUID.randomUUID()
        val accountData = generateAccount(customerId = UUID.randomUUID())
        coEvery { accountService.getAccountsByParentProcessId(any(UUID::class)) } returns listOf(accountData)
        val expectation = listOf(accountData.toApiResponse())

        webTestClient.get().uri(ACCOUNT_BY_PARENT_PROCESS_ID_PATH, parentProcess)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(objectWriter.writeValueAsString(expectation))
    }

    @Test
    fun scheduleAccountCreation_whenValidRequest_then200WithProcessDetailsIsReturned() = runTest {
        val customerId = UUID.randomUUID()
        val processId = UUID.randomUUID()
        val parentProcessId = UUID.randomUUID()
        val processDetails = generateProcessProgressDetails(
            processId = processId, parentProcessId = parentProcessId, processType = ProcessType.CREATE_CUSTOMER_ACCOUNT,
            progress = ProgressType.DISPATCHED, entityUuid = null, processDetails = null
        )
        val request = CreateCustomerAccountCommand(
            customerId = customerId,
            currencyCode = Currencies.USD.name,
            accountName = null
        )
        coEvery {
            accountService.scheduleNewAccountCreation(
                any(UUID::class),
                any(CreateCustomerAccountCommand::class)
            )
        } returns processDetails

        webTestClient.put()
            .uri(SCHEDULE_ACCOUNT_CREATION_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isAccepted
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(objectWriter.writeValueAsString(processDetails))
        coVerify { accountService.scheduleNewAccountCreation(any(), eq(request)) }
    }

    @Test
    fun scheduleBatchAccountCreation_whenValidRequest_then200WithProcessDetailsIsReturned() = runTest {
        val customerId1 = UUID.randomUUID()
        val customerId2 = UUID.randomUUID()
        val processDetails1 = generateProcessProgressDetails(
            processId = UUID.randomUUID(),
            parentProcessId = UUID.randomUUID(),
            processType = ProcessType.CREATE_CUSTOMER_ACCOUNT,
            progress = ProgressType.DISPATCHED,
            entityUuid = null,
            processDetails = null
        )
        val processDetails2 = generateProcessProgressDetails(
            processId = UUID.randomUUID(),
            parentProcessId = UUID.randomUUID(),
            processType = ProcessType.CREATE_CUSTOMER_ACCOUNT,
            progress = ProgressType.DISPATCHED,
            entityUuid = null,
            processDetails = null
        )
        val request1 = CreateCustomerAccountCommand(
            customerId = customerId1,
            currencyCode = Currencies.USD.name,
            accountName = null
        )
        val request2 = CreateCustomerAccountCommand(
            customerId = customerId2,
            currencyCode = Currencies.USD.name,
            accountName = null
        )
        coEvery { accountService.scheduleNewAccountCreation(any(UUID::class), any(UUID::class), any()) } answers {
            val receivedCustomerId = secondArg<UUID>()
            if (receivedCustomerId == customerId1) {
                listOf(processDetails1)
            } else {
                listOf(processDetails2)
            }
        }
        val command = BatchAccountCreation(listOf(request1, request2))
        val response = BatchAccountCreationResponse(
            mapOf(
                customerId1 to listOf(processDetails1),
                customerId2 to listOf(processDetails2)
            )
        )

        webTestClient.put()
            .uri(SCHEDULE_BATCH_ACCOUNT_CREATION_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(command)
            .exchange()
            .expectStatus().isAccepted
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(objectWriter.writeValueAsString(response))
        coVerify(exactly = 2) { accountService.scheduleNewAccountCreation(any(), any(), any()) }
    }
}