package org.bronco.payments.controllers

import com.fasterxml.jackson.databind.ObjectWriter
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.bronco.payments.config.ObjectMapperConfig
import org.bronco.payments.controllers.api.CreateCustomerRequest
import org.bronco.payments.controllers.api.ErrorDetail
import org.bronco.payments.controllers.api.ErrorTypes
import org.bronco.payments.controllers.api.PaymentsErrorResponse
import org.bronco.payments.model.ResourceNotFoundException
import org.bronco.payments.model.ResourceType
import org.bronco.payments.repositories.ResourceCouldNotBeenRemoved
import org.bronco.payments.services.customer.CustomerService
import org.bronco.payments.services.kafka.producer.customer.CustomerKafkaProducer
import org.bronco.payments.services.processes.model.ProcessType
import org.bronco.payments.services.processes.model.ProcessProgressDetails
import org.bronco.payments.services.processes.model.ProgressType
import org.bronco.payments.utils.CustomerDataGenerators.generateCreateCustomerRequest
import org.bronco.payments.utils.CustomerDataGenerators.generateRetrieveCustomerResponse
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
import org.springframework.web.reactive.function.BodyInserters
import java.util.*

//TODO: test and update it
@ExtendWith(SpringExtension::class)
@WebFluxTest(
    controllers = [CustomerController::class],
    excludeAutoConfiguration = [KafkaAutoConfiguration::class, DataSourceAutoConfiguration::class],
    properties = [
        "containers.kafka.enabled=false", "containers.db.enabled=false"
    ]
)
@Import(ObjectMapperConfig::class)
class CustomerControllerTest {
    @MockkBean
    private lateinit var customerKafkaProducer: CustomerKafkaProducer

    @MockkBean(relaxed = true)
    private lateinit var customerService: CustomerService

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var objectWriter: ObjectWriter

    @Test
    fun createCustomer_whenValidRequest_then204WithProgressDetailsIsReturned() = runTest {
        val requestPayload = generateCreateCustomerRequest("test-email@test.com")
        val processProgressDetails = ProcessProgressDetails(
            UUID.randomUUID(), ProcessType.CREATE_CUSTOMER.name, null,
            ProgressType.INITIALIZED.name, null, details = "no details"
        )
        coEvery { customerKafkaProducer.dispatchCreateCustomer(any(CreateCustomerRequest::class)) } returns processProgressDetails

        webTestClient.post().uri("/customers")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(requestPayload))
            .exchange()
            .expectStatus().isAccepted
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(objectWriter.writeValueAsString(processProgressDetails))

        coVerify(exactly = 1) { customerKafkaProducer.dispatchCreateCustomer(any(CreateCustomerRequest::class)) }
    }

    @Test
    fun createCustomer_whenInvalidRequest_then400WithErrorDetailsIsReturned() = runTest {
        val value = "not-an-email"
        val requestPayload = generateCreateCustomerRequest(value)
        val badRequest = HttpStatus.BAD_REQUEST
        val response = PaymentsErrorResponse(
            code = badRequest.value(), status = badRequest.name, type = ErrorTypes.VALIDATION,
            details = listOf(ErrorDetail(value = value, field = "email", message = "Valid email is required"))
        )

        webTestClient.post().uri("/customers")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(requestPayload))
            .exchange()
            .expectStatus().isBadRequest
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(objectWriter.writeValueAsString(response))

        coVerify(exactly = 0) { customerKafkaProducer.dispatchCreateCustomer(any(CreateCustomerRequest::class)) }
    }

    @Test
    fun createCustomer_whenRequestFailedDueToFewFields_then400WithErrorDetailsIsReturned() = runTest {
        val email = "not-an-email"
        val nationality = "ZD"
        val firstName = "nam3n"
        val middleName = "middleeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee"
        val birthDate = "unknown"
        val phoneNumber = "0-700-CALL-ME"
        val requestPayload = generateCreateCustomerRequest(
            email = email,
            nationality = nationality,
            firstName = firstName,
            middleName = middleName,
            birthDay = birthDate,
            phoneNumber = phoneNumber,
            countryOfResidence = "GB"
        )
        val badRequest = HttpStatus.BAD_REQUEST
        val response = PaymentsErrorResponse(
            code = badRequest.value(), status = badRequest.name, type = ErrorTypes.VALIDATION,
            details = listOf(
                ErrorDetail(value = email, field = "email", message = "Valid email is required"),
                ErrorDetail(value = nationality, field = "nationality", message = "Nationality ISO code is expected"),
                ErrorDetail(value = firstName, field = "firstName", message = "First name is invalid"),
                ErrorDetail(
                    value = middleName,
                    field = "middleName",
                    message = "Middle name must consist of 2-40 characters"
                ),
                ErrorDetail(value = "unknown", field = "dateOfBirth", message = "Proper Date of birth is required"),
                ErrorDetail(
                    value = null,
                    field = null,
                    message = "Provided phone number is invalid"
                ),
            )
        )

        webTestClient.post().uri("/customers")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(requestPayload))
            .exchange()
            .expectStatus().isBadRequest
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(objectWriter.writeValueAsString(response))

        coVerify(exactly = 0) { customerKafkaProducer.dispatchCreateCustomer(any(CreateCustomerRequest::class)) }
    }

    @Test
    fun retrieveCustomer_whenInvalidRequest_then400IsReturned() = runTest {
        val requestId = "invalid-uuid"
        val badRequest = HttpStatus.BAD_REQUEST
        val response = PaymentsErrorResponse(
            code = badRequest.value(), status = badRequest.name, type = ErrorTypes.VALIDATION,
            details = listOf(
                ErrorDetail(value = null, field = null, message = "Invalid UUID identifier was provided"),
            )
        )

        webTestClient.get().uri("/customers/{id}", requestId)
            .exchange()
            .expectStatus().isBadRequest
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(objectWriter.writeValueAsString(response))

    }

    @Test
    fun retrieveCustomer_whenCustomerWasNotFound_then404IsReturned() = runTest {
        val customerId = UUID.randomUUID()
        val notFound = HttpStatus.NOT_FOUND
        val response = PaymentsErrorResponse(
            notFound.value(),
            notFound.name,
            ErrorTypes.RESOURCE_NOT_FOUND,
            listOf(ErrorDetail(null, null, "Customer with id $customerId was not found"))
        )
        coEvery { customerService.retrieveCustomerById(customerId) } throws ResourceNotFoundException(
            customerId,
            ResourceType.CUSTOMER
        )

        webTestClient.get().uri("/customers/{id}", customerId)
            .exchange()
            .expectStatus().isNotFound
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(objectWriter.writeValueAsString(response))
        coVerify(exactly = 1) { customerService.retrieveCustomerById(customerId) }
    }

    @Test
    fun retrieveCustomerById_whenCustomerWasFound_thenCustomerIsReturned() = runTest {
        val customerId = UUID.randomUUID()
        val responsePayload = generateRetrieveCustomerResponse(customerId)
        coEvery { customerService.retrieveCustomerById(customerId) } returns responsePayload

        webTestClient.get().uri("/customers/{id}", customerId)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(objectWriter.writeValueAsString(responsePayload))
        coVerify(exactly = 1) { customerService.retrieveCustomerById(customerId) }
    }

    @Test
    fun deleteCustomerById_whenNoException_then204IsReturned() = runTest {
        val customerId = UUID.randomUUID()

        webTestClient.delete().uri("/customers/{id}", customerId)
            .exchange()
            .expectStatus().isNoContent
            .expectBody().isEmpty
        coVerify(exactly = 1) { customerService.deleteById(customerId) }
    }

    @Test
    fun deleteCustomerById_whenCustomerWasNotFound_then400WithPayloadIsReturned() = runTest {
        val customerId = UUID.randomUUID()
        val exception = ResourceNotFoundException(customerId, ResourceType.CUSTOMER)
        coEvery { customerService.deleteById(any()) } throws exception
        val expectation = PaymentsErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            HttpStatus.NOT_FOUND.name,
            ErrorTypes.RESOURCE_NOT_FOUND,
            listOf(ErrorDetail(null, null, exception.message))
        )

        webTestClient.delete().uri("/customers/{id}", customerId)
            .exchange()
            .expectStatus().isNotFound
            .expectBody().json(objectWriter.writeValueAsString(expectation))
        coVerify(exactly = 1) { customerService.deleteById(customerId) }
    }

    @Test
    fun deleteCustomerById_whenCustomerCouldNotBeRemoved_then400WithPayloadIsReturned() = runTest {
        val customerId = UUID.randomUUID()
        val exception = ResourceCouldNotBeenRemoved(customerId, ResourceType.CUSTOMER, "could not be removed")
        coEvery { customerService.deleteById(any()) } throws exception
        val expectation = PaymentsErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.name,
            ErrorTypes.RESOURCE_NOT_REMOVABLE,
            listOf(ErrorDetail(null, null, exception.message))
        )

        webTestClient.delete().uri("/customers/{id}", customerId)
            .exchange()
            .expectStatus().isBadRequest
            .expectBody().json(objectWriter.writeValueAsString(expectation))
        coVerify(exactly = 1) { customerService.deleteById(customerId) }
    }

    @Test
    fun deleteCustomerById_whenCustomerIdIsInvalid_then400WithPayloadIsReturned() = runTest {
        val customerId = "testValue"
        val expectation = PaymentsErrorResponse(
            code = HttpStatus.BAD_REQUEST.value(),
            status = HttpStatus.BAD_REQUEST.name,
            type = ErrorTypes.VALIDATION,
            details = listOf(
                ErrorDetail(
                    value = null,
                    field = null,
                    message = "Invalid UUID identifier was provided",
                )
            )
        )

        webTestClient.delete().uri("/customers/{id}", customerId)
            .exchange()
            .expectStatus().isBadRequest
            .expectBody().json(objectWriter.writeValueAsString(expectation))
        coVerify(exactly = 0) { customerService.deleteById(any()) }
    }
}