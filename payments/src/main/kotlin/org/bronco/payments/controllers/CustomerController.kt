package org.bronco.payments.controllers

import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.bronco.payments.controllers.api.CreateCustomerRequest
import org.bronco.payments.services.kafka.producer.customer.CustomerKafkaProducer
import org.bronco.payments.services.processes.model.ProcessProgressDetails
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "customers")
@RestController
@RequestMapping("customers")
class CustomerController(
    private val customerKafkaProducer: CustomerKafkaProducer
) {
    //TODO: swagger annotations
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun createCustomer(@Valid @RequestBody payload: CreateCustomerRequest): ResponseEntity<ProcessProgressDetails> {
        val result = customerKafkaProducer.dispatchCreateCustomer(payload)
        return ResponseEntity(result, HttpStatus.ACCEPTED)
    }
}