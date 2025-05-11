package org.bronco.payments.controllers

import io.swagger.v3.oas.annotations.tags.Tag
import org.bronco.payments.controllers.api.CreateCustomerRequest
import org.bronco.payments.controllers.api.CreateCustomerResponse
import org.bronco.payments.services.CustomerService
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
    private val customerService: CustomerService,
) {
    //TODO: swagger annotations
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun createCustomer(@RequestBody payload: CreateCustomerRequest): ResponseEntity<CreateCustomerResponse> {
        val result = customerService.createNewCustomer(payload)
        return ResponseEntity(result, result.errorDescription?.let { HttpStatus.BAD_REQUEST } ?: HttpStatus.OK)
    }
}