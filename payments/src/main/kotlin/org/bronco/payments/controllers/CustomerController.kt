package org.bronco.payments.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.bronco.payments.controllers.api.CreateCustomerRequest
import org.bronco.payments.controllers.api.RetrieveCustomerResponse
import org.bronco.payments.services.customer.CustomerService
import org.bronco.payments.services.kafka.producer.customer.CustomerKafkaProducer
import org.bronco.payments.services.processes.model.ProcessProgressDetails
import org.bronco.payments.utils.toUuid
import org.bronco.payments.validation.customer.ValidUuid
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.parameters.RequestBody as ApiRequestBody

@Tag(name = "customers", description = "Customer API deals with customer management")
@RestController
@RequestMapping("customers")
open class CustomerController(
    private val customerKafkaProducer: CustomerKafkaProducer,
    private val customerService: CustomerService
) {
    @Operation(
        method = "POST",
        tags = ["customers"],
        summary = """
            Creates a new customer. In case of an existing customer, no new user is to be created.
             The result of the operation can be verified at TODO: add specific endpoint to check the status.
            """,
        requestBody = ApiRequestBody(
            required = true,
            description = "Payload with necessary data to create a customer",
            useParameterTypeSchema = true,
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = CreateCustomerRequest::class)
                )
            ]
        ),
        responses = [
            ApiResponse(
                responseCode = "204",
                description = "Request was accepted. Checking the result with process id can verify its actual state",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ProcessProgressDetails::class)
                    )
                ]
            )
        ]
    )
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun createCustomer(@Valid @RequestBody payload: CreateCustomerRequest): ResponseEntity<ProcessProgressDetails> =
        ResponseEntity.accepted().body(customerKafkaProducer.dispatchCreateCustomer(payload))

    @Operation(
        method = "GET",
        summary = "Retrieves a customer identified by id",
        parameters = [
            Parameter(
                name = "id", description = "Customer id(UUID)", required = true, `in` = ParameterIn.PATH,
                allowEmptyValue = false
            )
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Customer was found",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = RetrieveCustomerResponse::class)
                    )
                ]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Customer does not exist",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = Any::class)
                    )
                ]
            )
        ]
    )
    @GetMapping(path = ["{id}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    @Validated
    suspend fun retrieveCustomer(@ValidUuid @PathVariable("id") customerId: String): ResponseEntity<RetrieveCustomerResponse> {
        return ResponseEntity.ok(customerService.retrieveCustomerById(customerId.toUuid()))
    }
}