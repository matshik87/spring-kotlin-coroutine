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
import org.bronco.payments.repositories.account.AccountRepository
import org.bronco.payments.repositories.account.model.toApiResponse
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
    private lateinit var repository: AccountRepository

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var objectWriter: ObjectWriter

    @Test
    fun retrieveAccountsByCustomerId_whenValidRequestAndCustomerExists_then200WithAllAccounts() = runTest {
        val customerId = UUID.randomUUID()
        val customerAccounts = listOf(generateAccount(customerId = customerId), generateAccount(customerId = customerId))
        coEvery { repository.getAllCustomerAccounts(customerId) } returns customerAccounts

        webTestClient.get().uri("/accounts/customer/{id}", customerId.toString())
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(objectWriter.writeValueAsString(customerAccounts.map { it.toApiResponse() }))
    }

    @Test
    fun retrieveAccountsByCustomerId_whenValidRequestButNoAccountsPerCustomer_then404() = runTest {
        val customerId = UUID.randomUUID()
        coEvery { repository.getAllCustomerAccounts(customerId) } returns emptyList()
        val notFound = HttpStatus.NOT_FOUND
        val expectedResponse = PaymentsErrorResponse(
            code = notFound.value(), status = notFound.name, type = ErrorTypes.RESOURCE_NOT_FOUND,
            details = listOf(ErrorDetail(value = null, field = null, message = "Account for customer with id $customerId was not found"))
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
        coVerify(exactly = 0) { repository.getAllCustomerAccounts(any(UUID::class)) }
    }
}