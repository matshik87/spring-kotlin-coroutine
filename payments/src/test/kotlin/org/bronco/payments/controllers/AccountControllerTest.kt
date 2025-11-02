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
import org.bronco.payments.repositories.account.model.toApiResponse
import org.bronco.payments.services.account.CustomerAccountService
import org.bronco.payments.utils.AccountDataGenerators.generateAccount
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

        webTestClient.get().uri("/accounts/customer/{id}", customerId.toString())
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

        webTestClient.get().uri("/accounts/customer/{id}", customerId.toString())
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

        webTestClient.get().uri("/accounts/customer/{id}", customerId)
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

        webTestClient.get().uri("/accounts/{id}", customerId.toString())
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

        webTestClient.get().uri("/accounts/{id}", accountId.toString())
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

        webTestClient.get().uri("/accounts/{id}", customerId)
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
        coEvery { accountService.getAccountsForProcessId(processId) } returns accounts
        webTestClient.get().uri("/accounts/process/{id}", processId)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(objectWriter.writeValueAsString(accounts))
        coVerify { accountService.getAccountsForProcessId(processId) }
    }

    @Test
    fun retrieveAccountsByProcessId_whenProcessWasNotFound_then200WithEmptyResponse() = runTest {
        val processId = UUID.randomUUID()
        webTestClient.get().uri("/accounts/process/{id}", processId)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(objectWriter.writeValueAsString(emptyList<Int>()))
        coVerify { accountService.getAccountsForProcessId(processId) }
    }

    @Test
    fun retrieveAccountsByProcessId_whenInValidRequest_then400() = runTest {
        val processId = "test"
        val badRequest = HttpStatus.BAD_REQUEST
        val expectedResponse = PaymentsErrorResponse(
            code = badRequest.value(), status = badRequest.name, type = ErrorTypes.VALIDATION,
            details = listOf(ErrorDetail(value = null, field = null, message = "Invalid UUID identifier was provided"))
        )

        webTestClient.get().uri("/accounts/process/{id}", processId)
            .exchange()
            .expectStatus().isBadRequest
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(objectWriter.writeValueAsString(expectedResponse))
        coVerify(exactly = 0) { accountService.getAccountsForProcessId(any(UUID::class)) }
    }
}