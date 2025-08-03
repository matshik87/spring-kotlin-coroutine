package org.bronco.payments.controllers

import com.fasterxml.jackson.databind.ObjectWriter
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.bronco.payments.config.ObjectMapperConfig
import org.bronco.payments.controllers.api.CreateCustomerRequest
import org.bronco.payments.services.customer.CustomerService
import org.bronco.payments.services.kafka.producer.customer.CustomerKafkaProducer
import org.bronco.payments.services.processes.model.ProcessName
import org.bronco.payments.services.processes.model.ProcessProgressDetails
import org.bronco.payments.services.processes.model.ProgressType
import org.bronco.payments.utils.CustomerDataGenerators.generateCreateCustomerRequest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import java.util.*

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
    @MockkBean
    private lateinit var customerService: CustomerService
    @Autowired
    private lateinit var webTestClient: WebTestClient
    @Autowired
    private lateinit var objectWriter: ObjectWriter

    @Test
    fun createCustomer_whenValidRequest_then204WithProgressDetailsIsReturned() = runTest {
        val requestPayload = generateCreateCustomerRequest("test-email@test.com")
        val processProgressDetails = ProcessProgressDetails(
            UUID.randomUUID(), ProcessName.CREATE_CUSTOMER.name,
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
}